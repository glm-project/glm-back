# Multi-tenant : un schema PostgreSQL par entreprise

Les donnees de chaque entreprise cliente vivent dans leur propre schema PostgreSQL, dans la base principale
ou dans une base qui lui est dediee, et chaque entreprise a son propre pool de connexions. L'entreprise de l'utilisateur courant est portee par le token
Keycloak, et la liste des entreprises est la table `tenant` du schema par defaut.

## Principe de securite

**Le token ne fournit jamais un nom de schema, seulement une cle.** Le claim `tenant` est recherche
parmi les lignes actives du registre `tenant` ; une cle absente du registre, ou suspendue, ne produit
aucun schema, elle produit un 403. Un token forge ne peut donc pas designer un schema arbitraire, et le
nom de schema effectivement pousse sur la connexion vient toujours du registre.

## Chaine complete

| Etape                                          | Ou                                                                      |
| ---------------------------------------------- | ----------------------------------------------------------------------- |
| Attribut `tenant` de l'utilisateur             | realm Keycloak, `glmproject-realm.json`                                 |
| Mapper `tenant` vers le claim d'access token   | client scope `glmproject` du realm                                      |
| Lecture du claim                               | `shared/multitenancy/application/CurrentTenant`                         |
| Autorisation `/api/**`                         | `shared/multitenancy/infrastructure/primary/TenantAuthorizationManager` |
| Registre des entreprises (table `tenant`)      | `wire/database/infrastructure/secondary/AdminSchemaInitializer`         |
| Correspondance tenant vers schema              | `wire/database/infrastructure/secondary/TenantRegistry`                 |
| Identifiant de tenant Hibernate                | `wire/database/infrastructure/secondary/CurrentTenantResolver`          |
| Pool de connexions de l'entreprise             | `wire/database/infrastructure/secondary/TenantDataSources`              |
| Choix du pool par Hibernate                    | `wire/database/infrastructure/secondary/TenantConnectionProvider`       |
| Positionnement du schema sur la connexion      | Hibernate, via `MULTI_TENANT_SCHEMA_MAPPER` (`TenantRegistry`)          |
| Creation et migration des schemas au demarrage | `wire/database/infrastructure/secondary/TenantSchemasInitializer`       |

L'identifiant de tenant vu par Hibernate est la **cle** de l'entreprise. `CurrentTenantResolver` la
verifie dans le registre : une entreprise inconnue echoue donc des l'ouverture de session, et non au fond
de l'acquisition de connexion. Hors requete, il rend un identifiant reserve (`_out_of_request`, qui ne
respecte pas le motif d'une cle) : pool principal et schema par defaut.

```
 jeton {tenant: katilys}
   └─ CurrentTenantResolver ──► "katilys"
        ├─ TenantConnectionProvider ──► pool Hikari-katilys        (TenantDataSources)
        └─ TenantSchemaMapper       ──► setSchema("katilys")       (TenantRegistry)
```

**Un pool par entreprise.** `TenantDataSources` ouvre un `HikariDataSource` par entreprise active, copie
de la configuration du pool principal (`spring.datasource.*`), nomme `Hikari-<cle>` et positionne sur le
schema de l'entreprise. Chaque colonne de connexion renseignee dans le registre remplace la valeur
principale : `jdbc_url` (le pilote est alors deduit de l'URL), `username`, `pool_max_size`, et le mot de
passe, lu dans la variable d'environnement que **nomme** `secret_ref`. Une variable absente empeche le
demarrage, avec un message qui nomme l'entreprise et la variable, jamais une valeur. Un pool ne s'ouvre qu'a sa premiere connexion : une entreprise sans
activite ne tient aucune connexion. Les pools sont fermes a l'arret du contexte. Une entreprise qui epuise
son pool n'attend que sur le sien ; les autres continuent d'etre servies.

**Le schema reste pose par Hibernate**, comme avant : le fournisseur de connexions ne declare pas gerer le
schema (`handlesConnectionSchema` a `false`), Hibernate fait donc `Connection.setSchema` a l'acquisition
et le restaure a la liberation.

Une base dediee injoignable au demarrage empeche l'application de demarrer, comme une migration en echec.

**Le pool principal** (`spring.datasource`) sert le registre, les besoins de demarrage
d'Hibernate et tout acces hors requete. C'est aussi lui que connait `JpaTransactionManager` : sans effet
tant qu'aucun `JdbcTemplate` ou `DataSourceUtils` n'est utilise dans une transaction JPA, ce qui est le
cas. Un tel acces partirait sur le pool principal, et non sur celui de l'entreprise.

**Dimensionnement** : sur un serveur PostgreSQL donne, le nombre maximal de connexions est la somme des
pools qui le visent — le pool principal et ceux des entreprises qu'il heberge, chacun de
`pool_max_size` ou, a defaut, de `maximum-pool-size`. Elle doit rester sous `max_connections`.

## Migrations

`spring.liquibase.enabled` est volontairement a `false` : l'autoconfiguration migrerait le seul schema
par defaut. `TenantSchemasInitializer` rejoue `master.xml` **une fois par schema**, sur la base de
l'entreprise — par des connexions hors pool, pour ne pas ouvrir son pool avant sa premiere requete — avec
`defaultSchema` et `liquibaseSchema` positionnes, chaque schema portant donc son propre
`databasechangelog`. Un `EntityManagerFactoryDependsOnPostProcessor` declare dans `DatabaseConfiguration`
garantit que cette initialisation precede l'`EntityManagerFactory`.

Le modele d'activites s'installe exclusivement sur un schema neuf, selon
[l'ADR 0003](adr/0003-bootstrap-only-empty-tenant-schemas.md). Le premier changeset technique,
`initialisation_schema_neuf`, precede toute DDL metier dans `2026/08/001-element_de_fabrication.xml`.
A sa premiere execution, il exige que le `databasechangelog` de cette entreprise ne contienne aucune
ligne ; la precondition arrete sur echec **et** erreur. La connexion brute de l'initialiseur reste en
`public` : la requete qualifie donc le schema avec `${database.defaultSchemaName}`, configure par
Liquibase pour chaque tenant.

La garde est ensuite enregistree normalement dans cet historique. Au redemarrage, Liquibase la saute
comme tout changeset deja execute : ni ses donnees ni son historique ne sont remis a zero. Une
entreprise ajoutee passe sa propre premiere garde, independamment des autres entreprises deja installees.
Un prefixe ancien non vide, meme arrete avant septembre `001` avec des checksums valides, est refuse
avant toute nouvelle DDL metier. Un schema portant des tables metier sans historique echoue a la
creation normale de ces tables ; aucune adoption n'est prevue.

Ce bootstrap ne migre ni ne reinitialise une installation ancienne. Toute intervention sur une base,
un volume ou un conteneur preexistant requiert l'autorisation de son proprietaire sur la ressource et
la commande exactes.

## Ou sont declarees les entreprises

Les entreprises sont les lignes de la table `tenant`, dans le schema par defaut
(`application.multitenancy.default-schema`, `public`), qui ne porte aucune table metier :

| colonne         | role                                                                                     |
| --------------- | ---------------------------------------------------------------------------------------- |
| `id`            | cle, valeur du claim `tenant` Keycloak (`^[a-z][a-z0-9_]{0,62}$`)                        |
| `schema_name`   | schema des donnees de l'entreprise (meme motif, unique)                                  |
| `status`        | `ACTIVE`, ou `SUSPENDED` : l'entreprise recoit 403, ses donnees restent                  |
| `jdbc_url`      | base dediee de l'entreprise ; `NULL` : base principale                                   |
| `username`      | role PostgreSQL de l'entreprise ; `NULL` : utilisateur principal                         |
| `secret_ref`    | **nom** de la variable d'env qui porte le mot de passe ; `NULL` : mot de passe principal |
| `pool_max_size` | taille du pool de l'entreprise ; `NULL` : `spring.datasource.hikari.maximum-pool-size`   |

Au demarrage, `AdminSchemaInitializer` cree et migre cette table avec son propre changelog
(`config/liquibase/admin/master.xml`, historique dans le `databasechangelog` du schema par defaut).
`TenantRegistry` lit ensuite les lignes `ACTIVE`, une fois : une entreprise ajoutee ou suspendue n'est
prise en compte qu'au **redemarrage** suivant. Enfin `TenantSchemasInitializer` cree et migre le schema
de chacune.

Le **mecanisme** est du code de production. Le **jeu d'entreprises** `impeccmold` / `katilys`, lui, est
une donnee de developpement — il n'a rien a faire dans l'artefact livre. Il est depose dans la table par
un changelog distinct, que seul son profil declare via `application.multitenancy.seed-change-log` :

| Profil  | `seed-change-log`                                                                                                                                         |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| aucun   | absent — registre vide                                                                                                                                    |
| `local` | `config/liquibase/admin/seed/local.xml` (src/main) : impeccmold et katilys                                                                                |
| `test`  | `config/liquibase/admin/seed/test.xml` (src/test) : impeccmold, katilys, les entreprises propres a certaines classes de test, et une entreprise suspendue |

Consequence : **lancer l'application sans profil ne cree aucun schema**, et tout appel a `/api/**`
repond 403 faute de tenant connu. C'est le comportement voulu — un artefact de production ne
s'auto-provisionne pas des entreprises de demonstration. Pour un lancement local :

```bash
java -jar target/glmproject-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Ajouter une entreprise

```
  ┌─────────────────────┐   ┌───────────────────────┐   ┌──────────────────────┐   ┌────────────────┐
  │ 1. Base             │──►│ 2. Ligne `tenant`     │──►│ 3. Keycloak          │──►│ 4. Redemarrage │
  │ (si base dediee)    │   │ (script SQL)          │   │ users tenant=<id>    │   │                │
  └─────────────────────┘   └───────────────────────┘   └──────────────────────┘   └────────────────┘
```

1. **Base de donnees**
   - Entreprise sur la base principale : rien a faire, son schema sera cree au demarrage.
   - Entreprise sur sa propre base : creer la base et un role PostgreSQL proprietaire, puis declarer son
     mot de passe dans une variable d'environnement de l'application, par exemple
     `GLM_TENANT_ACME_DB_PASSWORD`. La variable se declare **avant** la ligne `tenant` : sans ligne, elle
     n'est pas lue, alors qu'une ligne sans variable empecherait tout redemarrage.
2. **Declarer l'entreprise** sur la base principale avec `documentation/scripts/ajouter-tenant.sql` :

```bash
# base principale
psql "$DATABASE_URL" -v id=acme -f documentation/scripts/ajouter-tenant.sql
# base dediee
psql "$DATABASE_URL" -v id=acme -v jdbc_url=jdbc:postgresql://pg-acme:5432/acme -v username=acme \
  -v secret_ref=GLM_TENANT_ACME_DB_PASSWORD -v pool_max_size=10 -f documentation/scripts/ajouter-tenant.sql
```

L'identifiant et le schema (par defaut, l'identifiant) doivent respecter `^[a-z][a-z0-9_]{0,62}$`. En
developpement, ajouter plutot un changeset au jeu du profil (`seed/local.xml`) : une base locale recreee
le retrouve.

3. **Keycloak** : donner l'attribut `tenant` (valeur = l'`id`) et les roles (`GESTIONNAIRE`, `USER`) aux
   utilisateurs de l'entreprise.
4. **Redemarrer** l'application : pool ouvert, schema neuf cree, garde passee, migrations jouees.
5. **Verifier** : connexion d'un utilisateur de l'entreprise ; a sa premiere requete, le log
   `Hikari-<id> - Start completed`.

| Operation               | Comment                                                                                                |
| ----------------------- | ------------------------------------------------------------------------------------------------------ |
| Suspendre               | `UPDATE public.tenant SET status = 'SUSPENDED' WHERE id = '...'` + redemarrage → 403, donnees intactes |
| Reactiver               | `status = 'ACTIVE'` + redemarrage                                                                      |
| Agrandir son pool       | `pool_max_size = 20` + redemarrage                                                                     |
| Deplacer vers sa base   | dump/restore du schema, puis `jdbc_url`, `username`, `secret_ref` + redemarrage                        |
| Changer un mot de passe | variable d'environnement (redemarrage)                                                                 |

## Utilisateurs de developpement

Le realm `glmproject` contient six utilisateurs d'entreprise, mot de passe egal au login :

| username                  | roles                            | tenant       |
| ------------------------- | -------------------------------- | ------------ |
| `gestionnaire.impeccmold` | `ROLE_GESTIONNAIRE`, `ROLE_USER` | `impeccmold` |
| `admin.impeccmold`        | `ROLE_ADMIN`, `ROLE_USER`        | `impeccmold` |
| `user.impeccmold`         | `ROLE_USER`                      | `impeccmold` |
| `gestionnaire.katilys`    | `ROLE_GESTIONNAIRE`, `ROLE_USER` | `katilys`    |
| `admin.katilys`           | `ROLE_ADMIN`, `ROLE_USER`        | `katilys`    |
| `user.katilys`            | `ROLE_USER`                      | `katilys`    |

Les utilisateurs historiques `admin` et `user` portent `tenant: impeccmold`.

**C'est `ROLE_GESTIONNAIRE`, et non `ROLE_ADMIN`, qui ouvre les actes metier** (creation, modification, suppression,
engagement, cloture, regularisation). `ROLE_ADMIN` est reserve a l'administration technique — `/api/admin/**` et
`/management/**` — et ne donne aucun acces au metier : un `admin.*` ne peut que lire, via son `ROLE_USER`. Pour
travailler sur l'API metier en developpement, se connecter en `gestionnaire.*`.

Trois pieges de l'import du realm :

- Depuis Keycloak 24, le profil utilisateur declaratif rejette les attributs non declares. L'attribut
  `tenant` est donc declare dans le composant `org.keycloak.userprofile.UserProfileProvider` du realm ;
  sans cela l'attribut disparait silencieusement et le claim n'apparait jamais dans le token.
- Le realm, exporte avant Keycloak 24, ne porte pas le client scope `basic` qui emet normalement le
  claim `sub`. Le mapper `oidc-sub-mapper` du client scope `glmproject` conserve cette identite standard
  dans les access tokens.
- `KC_DB=dev-file` : le realm n'est reimporte que sur un volume neuf. Apres modification du JSON :

```bash
docker compose -f src/main/docker/keycloak.yml down -v
docker compose -f src/main/docker/keycloak.yml up -d
```

Le client `web_app` a `directAccessGrantsEnabled: false` : il n'y a pas de grant `password` disponible,
un token se recupere via le front. Pour un `curl` ponctuel, activer temporairement le direct access
grant sur le client depuis la console d'administration.

## Validite du jeton pour les appels API

Le serveur lit les claims du JWT signe : identite, roles et entreprise appartiennent au meme jeton.
Le decoder Nimbus verifie la signature, l'issuer, la validite temporelle et l'audience configuree.
Il ne charge pas `userinfo` au decodage et ne conserve aucun cache utilisateur ou objet de requete.
Un nouveau jeton portant un autre role ou tenant prend effet des son premier appel.

La revocation de session ou le retrait d'un role chez Keycloak ne revoque pas immediatement un JWT
deja emis : l'API le reconnait jusqu'a son expiration, avec la tolerance d'horloge du validateur Spring.
La duree d'emission se regle dans le realm (`accessTokenLifespan`) ; l'API ne fait pas d'introspection
par requete. `SignedJwtConfigurationTest` controle la signature, l'expiration, l'issuer et l'audience
avec de vrais JWT signes, les changements de roles et d'entreprise pour un meme sujet, ainsi que
l'absence d'appel `userinfo` sur mille requetes distinctes.

## Tests

- `@WithTenant("impeccmold")` (dans `shared/multitenancy/infrastructure/primary`) authentifie par un JWT
  porteur du claim de tenant. `@WithMockUser`, applique par `@IntegrationTest`, n'en produit pas : sous
  cette seule annotation, tout appel a `/api/**` repond 403.
- `TenantSecurityContexts.authenticateOn(...)` fait la meme chose au milieu d'un test, pour comparer
  deux entreprises dans le meme scenario.
- Avec MockMvc, une requete vers une autre entreprise porte son propre JWT via `jwt().jwt(...)`.
  `TenantSecurityContexts.authenticateOn(...)` change le contexte du thread pour la preparation en base,
  mais ne remplace pas le contexte de test que `@WithTenant` reapplique a la requete HTTP.
- Un test d'integration ne peut pas etre `@Transactional` : le listener transactionnel s'execute avant
  celui qui installe le contexte de securite, la session s'ouvrirait donc sans tenant. Passer par le
  `TransactionTemplate` **dans** le corps du test.
- Une classe de test qui veut des donnees que personne d'autre ne voit utilise une entreprise qui lui est
  propre (`supervision_fixture`, `dossier_fixture`, ...) : elle se declare dans `seed/test.xml`, jamais
  dans les properties de la classe.
- Cote Cucumber, le token factice a la forme `base64("<username>|<roles>|<tenant>")` ; le step a deux
  arguments retombe sur `impeccmold`.

`FreshActivitySchemaIT` possede trois PostgreSQL Testcontainers neufs et non reutilises, distincts de
la datasource des autres IT. Il demarre la vraie application, son initialiseur et ses mappings JPA
avec `ddl-auto=none`, sans jeu de test : chaque entreprise y est inseree dans le registre avant le
demarrage. Deux entreprises, redemarrage sans modification, puis une troisieme neuve. Les
catalogues, contraintes, index, journaux, projections et lecteurs reels sont controles par tenant.
Un autre conteneur installe un prefixe historique valide depuis une fixture de test controlee ; son
refus laisse catalogue et historique inchanges, ainsi que les donnees d'un tenant voisin deja neuf.
Le dernier cas prouve le refus d'adopter des tables metier sans historique. Ces tests ne touchent
aucune base, aucun volume ni conteneur preexistant.

`DedicatedDatabaseIT` demarre l'application avec une entreprise sur la base principale et une autre sur un
second PostgreSQL, avec son propre role et un mot de passe passe par la variable que nomme `secret_ref` :
schema cree et migre sur la seule base dediee, ecritures de chaque entreprise sur sa base, taille de pool
propre. Sans la variable, l'application ne demarre pas et la base dediee reste intacte.

## Passage en production : decisions restant a prendre

**Le montage actuel est un choix de phase de conception.** Un seul cluster PostgreSQL, une seule
base principale et des bases dediees declarees a la main, un pool par entreprise, un registre lu une fois au demarrage, et une migration jouee au demarrage
de l'application : c'est suffisant pour valider la mecanique d'isolation, ce n'est pas un modele de
deploiement. Deux axes independants restent a trancher avant une mise en production.

### Axe 1 — Topologie : ou vivent physiquement les donnees

**Fait (#88, #89)** : un schema par entreprise, dans la base principale ou dans une base ou une instance
PostgreSQL qui lui est dediee. L'identifiant de tenant Hibernate est la cle de l'entreprise,
`TenantConnectionProvider` (`AbstractDataSourceBasedMultiTenantConnectionProviderImpl`) prend la connexion
dans le pool de l'entreprise, et `TenantSchemasInitializer` migre chaque schema sur la base de son
entreprise.

Ce qui ne bouge pas, quelle que soit la topologie : le domaine, les agregats, les repositories, les
changelogs, `CurrentTenant`, `Tenant`, le port `Tenants`, `TenantAuthorizationManager`, et toute la
configuration Keycloak. Aucun agregat ne porte d'identifiant d'entreprise, et rien hors de
`wire/database` ne sait ce qu'est un schema.

### Axe 2 — Provisioning : d'ou vient la liste des entreprises

**Fait (#87)** : la liste des entreprises est la table `tenant` du schema par defaut, et non plus une
liste de properties. Elle porte deja les colonnes d'une base dediee (`jdbc_url`, `username`, et une
_reference_ de secret `secret_ref`). Le port `Tenants` n'a pas change ; seul son
adapter est passe de « lit les properties » a « lit la table d'administration ».

Deux consequences restent a anticiper :

- **Un tenant ajoute a chaud** suppose de construire les `DataSource` paresseusement et de rafraichir le
  registre sans redemarrer. Le dimensionnement devient reel : N entreprises multipliees par la taille de
  pool, ca se compte.
- **`TenantRegistry` melange encore deux responsabilites** qu'il faudra separer : quelles entreprises
  existent (registre) et ou vivent leurs donnees (topologie). En production elles viennent de sources
  differentes et changent a des rythmes differents.

### Axe 3 — Migrations : quand elles sont jouees

Aujourd'hui au demarrage de l'application (voir « Migrations » plus haut). En production, elles doivent
passer dans un **job de deploiement distinct** :

- avec plusieurs repliques, toutes tentent de migrer au boot ; Liquibase les serialise via
  `databasechangeloglock` par schema, mais un crash en cours de migration laisse un verrou a lever a la
  main ;
- un job separe est rejouable : `databasechangelog` etant par schema, un echec au troisieme tenant se
  rattrape en relancant, les deux premiers etant sautes ;
- le temps de demarrage cesse de croitre avec le nombre d'entreprises.

Deux conditions a respecter :

1. **Le job doit lancer le meme artefact avec la meme configuration** que le serveur, jamais un script ou
   un plugin Maven qui reimplementerait la boucle sur les tenants. Deux listes divergent tot ou tard, et
   une entreprise absente du job n'a simplement pas de schema : l'application demarre sans broncher et le
   premier appel de cet utilisateur part en erreur SQL. La forme visee est un mode `--migrate-only`
   reutilisant `TenantSchemasInitializer`.
2. **Les migrations doivent devenir retrocompatibles** (expand/contract). Si le serveur continue de
   tourner pendant la migration, l'ancien code s'execute contre le nouveau schema : ajouter une table ou
   une colonne nullable est sans risque, renommer ou supprimer une colonne casse l'instance en cours.
   Tout changement destructif se fait en deux deploiements.
