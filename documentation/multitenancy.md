# Multi-tenant : un schema PostgreSQL par entreprise

Les donnees de chaque entreprise cliente vivent dans leur propre schema PostgreSQL, au sein d'une base
et d'un pool de connexions uniques. L'entreprise de l'utilisateur courant est portee par le token
Keycloak.

## Principe de securite

**Le token ne fournit jamais un nom de schema, seulement une cle.** Le claim `tenant` est recherche
dans la table de correspondance issue de `application.multitenancy.tenants` ; une cle absente de cette
configuration ne produit aucun schema, elle produit un 403. Un token forge ne peut donc pas designer un
schema arbitraire, et le nom de schema effectivement pousse sur la connexion vient toujours du fichier
de configuration.

## Chaine complete

| Etape                                          | Ou                                                                      |
| ---------------------------------------------- | ----------------------------------------------------------------------- |
| Attribut `tenant` de l'utilisateur             | realm Keycloak, `glmproject-realm.json`                                 |
| Mapper `tenant` vers le claim d'access token   | client scope `glmproject` du realm                                      |
| Lecture du claim                               | `shared/multitenancy/application/CurrentTenant`                         |
| Autorisation `/api/**`                         | `shared/multitenancy/infrastructure/primary/TenantAuthorizationManager` |
| Correspondance tenant vers schema              | `wire/database/infrastructure/secondary/TenantSchemas`                  |
| Identifiant de tenant Hibernate                | `wire/database/infrastructure/secondary/CurrentTenantResolver`          |
| Positionnement du schema sur la connexion      | Hibernate, via `MULTI_TENANT_SCHEMA_MAPPER`                             |
| Creation et migration des schemas au demarrage | `wire/database/infrastructure/secondary/TenantSchemasInitializer`       |

L'identifiant de tenant vu par Hibernate **est** le nom du schema : un tenant inconnu echoue donc des
l'ouverture de session, et non au fond de l'acquisition de connexion.

Hibernate 7 sait positionner lui-meme le schema (`Connection.setSchema` a l'acquisition, restauration a
la liberation) des lors qu'un `TenantSchemaMapper` est configure — aucun `MultiTenantConnectionProvider`
maison n'est necessaire.

## Migrations

`spring.liquibase.enabled` est volontairement a `false` : l'autoconfiguration migrerait le seul schema
par defaut. `TenantSchemasInitializer` rejoue `master.xml` **une fois par schema**, avec
`defaultSchema` et `liquibaseSchema` positionnes, chaque schema portant donc son propre
`databasechangelog`. Un `EntityManagerFactoryDependsOnPostProcessor` declare dans `DatabaseConfiguration`
garantit que cette initialisation precede l'`EntityManagerFactory`.

## Ou sont declarees les entreprises

Le **mecanisme** est du code de production : la liaison `application.multitenancy`, la resolution du
schema, l'initialisation Liquibase. Le **jeu d'entreprises** `impeccmold` / `katilys`, lui, est une
donnee de developpement — il n'a rien a faire dans l'artefact livre.

D'ou la repartition :

| Fichier                                           | Contenu                                      |
| ------------------------------------------------- | -------------------------------------------- |
| `src/main/resources/config/application.yml`       | `tenants: []` — aucune entreprise par defaut |
| `src/main/resources/config/application-local.yml` | impeccmold et katilys, pour le dev           |
| `src/test/resources/config/application-test.yml`  | impeccmold et katilys, pour les tests        |

Consequence : **lancer l'application sans profil ne cree aucun schema**, et tout appel a `/api/**`
repond 403 faute de tenant connu. C'est le comportement voulu — un artefact de production ne
s'auto-provisionne pas des entreprises de demonstration. Pour un lancement local :

```bash
java -jar target/glmproject-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

La liste est dupliquee entre les profils `local` et `test` : les profils Spring ne s'heritent pas entre
`src/main` et `src/test`, et ces deux jeux ont vocation a diverger le jour ou un test aura besoin d'une
entreprise supplementaire.

## Ajouter une entreprise

1. Declarer le tenant dans le profil concerne (`application-local.yml` en developpement) :

```yaml
application:
  multitenancy:
    tenants:
      - id: nouvelle_entreprise
        schema: nouvelle_entreprise
```

L'identifiant et le schema doivent respecter `^[a-z][a-z0-9_]{0,62}$`.

2. Donner l'attribut `tenant` aux utilisateurs de cette entreprise dans Keycloak (valeur = l'`id`
   declare ci-dessus).
3. Redemarrer l'application : le schema est cree et migre au demarrage.

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
engagement, cloture, corrections). `ROLE_ADMIN` est reserve a l'administration technique — `/api/admin/**` et
`/management/**` — et ne donne aucun acces au metier : un `admin.*` ne peut que lire, via son `ROLE_USER`. Pour
travailler sur l'API metier en developpement, se connecter en `gestionnaire.*`.

Trois pieges de l'import du realm :

- Depuis Keycloak 24, le profil utilisateur declaratif rejette les attributs non declares. L'attribut
  `tenant` est donc declare dans le composant `org.keycloak.userprofile.UserProfileProvider` du realm ;
  sans cela l'attribut disparait silencieusement et le claim n'apparait jamais dans le token.
- Le realm, exporte avant Keycloak 24, ne porte pas le client scope `basic` qui emet normalement le
  claim `sub`. Sans `sub`, `CustomClaimConverter` echoue et **tout** token porteur est rejete en 401 :
  un mapper `oidc-sub-mapper` a donc ete ajoute au client scope `glmproject`.
- `KC_DB=dev-file` : le realm n'est reimporte que sur un volume neuf. Apres modification du JSON :

```bash
docker compose -f src/main/docker/keycloak.yml down -v
docker compose -f src/main/docker/keycloak.yml up -d
```

Le client `web_app` a `directAccessGrantsEnabled: false` : il n'y a pas de grant `password` disponible,
un token se recupere via le front. Pour un `curl` ponctuel, activer temporairement le direct access
grant sur le client depuis la console d'administration.

## Tests

- `@WithTenant("impeccmold")` (dans `shared/multitenancy/infrastructure/primary`) authentifie par un JWT
  porteur du claim de tenant. `@WithMockUser`, applique par `@IntegrationTest`, n'en produit pas : sous
  cette seule annotation, tout appel a `/api/**` repond 403.
- `TenantSecurityContexts.authenticateOn(...)` fait la meme chose au milieu d'un test, pour comparer
  deux entreprises dans le meme scenario.
- Un test d'integration ne peut pas etre `@Transactional` : le listener transactionnel s'execute avant
  celui qui installe le contexte de securite, la session s'ouvrirait donc sans tenant. Passer par le
  `TransactionTemplate` **dans** le corps du test.
- Cote Cucumber, le token factice a la forme `base64("<username>|<roles>|<tenant>")` ; le step a deux
  arguments retombe sur `impeccmold`.

## Passage en production : decisions restant a prendre

**Le montage actuel est un choix de phase de conception.** Un seul cluster PostgreSQL, une seule
`DataSource`, une liste de tenants statique dans les profils de developpement, et une migration jouee au demarrage
de l'application : c'est suffisant pour valider la mecanique d'isolation, ce n'est pas un modele de
deploiement. Deux axes independants restent a trancher avant une mise en production.

### Axe 1 — Topologie : ou vivent physiquement les donnees

Aujourd'hui : un schema par entreprise dans une base unique. Demain, possiblement une base ou une
**instance PostgreSQL par entreprise**.

Ce changement casse un seul choix technique : Hibernate ne peut plus poser le schema lui-meme via
`TenantSchemaMapper`, qui ne sait faire qu'un `setSchema` sur une connexion deja obtenue d'une
`DataSource` unique. Il faut alors un `MultiTenantConnectionProvider` — Hibernate le prevoit avec
`AbstractDataSourceBasedMultiTenantConnectionProviderImpl`, qui rend une `ConnectionProvider`, donc un
pool, par tenant.

Classes a reprendre, toutes dans `wire/database/infrastructure/secondary` :

| Classe                               | Devenir                                                                |
| ------------------------------------ | ---------------------------------------------------------------------- |
| `TenantSchemas`                      | rend un couple (DataSource, schema) au lieu d'un nom de schema         |
| `MultitenancyHibernateConfiguration` | declare un `MultiTenantConnectionProvider` a la place du schema mapper |
| `TenantSchemasInitializer`           | boucle sur les DataSources, plus sur les schemas d'une seule           |
| `CurrentTenantResolver`              | rend l'identifiant de tenant, plus le nom de schema                    |

Attention a cette derniere ligne : **l'identifiant de tenant vu par Hibernate est aujourd'hui le nom du
schema**, choix delibere pour qu'un tenant inconnu echoue des l'ouverture de session. En multi-instance
il n'a plus de sens — l'identifiant redevient la cle, et c'est le provider qui resout instance et schema.

Ce qui ne bouge pas, quelle que soit la topologie : le domaine, les agregats, les repositories, les
changelogs, `CurrentTenant`, `Tenant`, le port `Tenants`, `TenantAuthorizationManager`, et toute la
configuration Keycloak. Aucun agregat ne porte d'identifiant d'entreprise, et rien hors de
`wire/database` ne sait ce qu'est un schema.

### Axe 2 — Provisioning : d'ou vient la liste des entreprises

Les profils `local` et `test` ne tiennent plus des qu'il y a plusieurs instances, parce que la liste
porte alors des **identifiants de connexion**, qui n'ont rien a faire dans un fichier versionne. La base
`application.yml` declare deja `tenants: []` : c'est l'adapter du port `Tenants` qui changera de source,
pas le format des properties.

La forme habituelle est un plan de controle : une table `tenant` dans une base d'administration (hote,
port, base, schema, et une _reference_ de secret), plus un gestionnaire de secrets pour les mots de
passe. Le port `Tenants` ne change pas ; seul son adapter passe de « lit les properties » a « lit la
table d'administration ».

Deux consequences a anticiper :

- **Un tenant ajoute a chaud** suppose de construire les `DataSource` paresseusement et de rafraichir le
  registre sans redemarrer. Le dimensionnement devient reel : N entreprises multipliees par la taille de
  pool, ca se compte.
- **`TenantSchemas` melange aujourd'hui deux responsabilites** qu'il faudra separer : quelles entreprises
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
