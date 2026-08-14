# CLAUDE.md

Ce fichier fournit à Claude Code les règles à respecter en permanence sur ce projet.

## Vue d'ensemble

`glmproject` (groupId `com.glm.glmback`) est un projet Spring Boot généré par **seed4j**, construit en **architecture hexagonale (ports & adapters)** et **Domain-Driven Design (DDD)**. Stack : Java 25, Maven, Spring Boot 4.0.6, PostgreSQL (via Liquibase), Keycloak (OAuth2/OIDC).

Le code technique partagé vit dans `shared/` (dont le shared kernel `shared/pagination`) et `wire/`. Le contexte métier (bounded contexts et leur rôle) est décrit dans [documentation/contexte-metier.md](documentation/contexte-metier.md) — à tenir à jour à chaque nouveau bounded context.

**Chaque bounded context porte son propre `CLAUDE.md`**, à la racine de son package (`src/main/java/com/glm/glmback/<contexte>/CLAUDE.md`) : il énonce ce dont le contexte s'occupe, ce dont il ne s'occupe **pas**, ses agrégats, ses invariants et ses ports sortants. Le lire avant de toucher au contexte, et le créer avec tout nouveau bounded context. Il ne duplique ni les règles de code ci-dessous, ni la justification métier de `documentation/contexte-metier.md` — il pointe vers elles.

Toute nouvelle fonctionnalité doit suivre les règles ci-dessous dès sa création.

## Principes obligatoires

### Architecture hexagonale

Chaque bounded context est un package top-level sous `com.glm.glmback`, structuré en :

- `domain/` — objets métier purs (entités, value objects, invariants). Aucune dépendance vers Spring, JPA, HTTP, etc.
- `application/` — cas d'usage / orchestration. Dépend du `domain`, définit les ports (interfaces) dont il a besoin.
- `infrastructure/primary/` — adapters entrants (contrôleurs REST, sécurité, conversion JWT).
- `infrastructure/secondary/` — adapters sortants (repositories JPA, clients externes).

Règle de dépendance stricte : `domain` ne dépend de rien d'infrastructure ; `infrastructure` dépend de `domain`/`application`, jamais l'inverse. Les ports (interfaces) vivent côté `domain`/`application`, les implémentations côté `infrastructure` (Dependency Inversion).

### DDD

- Value objects immuables, validés via `Assert`/`AssertionException` (`shared/error/domain`) plutôt que des exceptions ad hoc.
- Invariants métier portés par le domaine, jamais par les adapters ou les DTOs.
- **Accès aux value objects** : aucun accesseur ne porte le préfixe `get` — on lit toujours par le nom de la propriété. Un value object qui enveloppe une **valeur primitive** (String, UUID, nombre) nomme son unique composant `value`, et son accesseur généré `value()` est le seul point de lecture : ne jamais écrire de méthode `get()` déléguant à l'accesseur du record. Deux cas gardent au contraire le nom de leur propriété : un record à plusieurs composants ou dont l'unique composant est lui-même un type métier, et un **identifiant**, dont le composant est nommé d'après ce qu'il enveloppe plutôt que `id` — le type englobant dit déjà que c'est un identifiant, le nommer `id` produirait `id.id()` chez tout appelant, et `value` perdrait l'information de représentation. L'étiquette passée à `Assert` reste en revanche le **nom métier** de la propriété : c'est elle qui remontera au client dans un message d'erreur. Le code seed4j généré antérieurement à cette règle reste tel quel pour rester régénérable.
- **Domaine immuable, sans mutation d'état** : aucun setter, aucune méthode de type `rename`/`describe` sur un agrégat ou un value object — ce sont des setters déguisés, qui n'expriment aucune intention métier. Un changement d'état, c'est la construction d'un nouvel agrégat complet (même identité, nouvelle date de modification) suivie d'un `update` explicite sur le repository. Le seul état mutable autorisé vit dans les adapters `infrastructure/secondary`, confiné derrière un port.
  - Cette reconstruction s'écrit dans une **méthode de transition portée par l'agrégat**, nommée d'après l'acte métier, recevant **uniquement ce que cet acte change** et rendant un nouvel agrégat complet. Elle n'est jamais énumérée par un service : là-bas, chaque composant aurait deux sources plausibles — l'existant et la commande — qu'aucun type ne distingue, et une recopie prise à la mauvaise source compilerait sans bruit (un titre repris de l'existant rendrait la modification sans effet, une date de création remplacée par l'instant courant réécrirait l'histoire). Le step builder garantit la complétude, jamais la justesse d'une recopie.
  - La frontière avec les `rename`/`describe` proscrits : **une méthode par acte métier**, recevant toutes les valeurs que cet acte modifie — jamais une méthode par champ.
  - La règle est récursive : un value object qui porte une part de l'état expose sa propre transition et conserve ce qui ne change pas (`Fiche.revise` garde `dateDeCreation` et refuse toujours une `dateDeModification` antérieure).
  - Le step builder reste la voie de la **création** et de la relecture depuis la persistance ; les transitions passent par la méthode métier.
- **Repository** : le port est une interface du `domain` nommée d'après l'agrégat, son implémentation vit dans `infrastructure/secondary`. Sémantique stricte et explicite plutôt qu'un `save` fourre-tout : `create` échoue si l'identité existe déjà, `update` et `delete` échouent si elle est inconnue, chacun avec une exception métier propre au bounded context (jamais une exception générique appartenant au shared kernel). La lecture d'une collection ne rend jamais une liste nue : elle prend un objet de critères portant les règles de sélection (la logique de correspondance vit dans le `domain`, jamais dans l'adapter) et un objet de pagination, et rend une page de résultats. Toute lecture paginée impose un **tri total déterministe** côté adapter, sans quoi la pagination est incorrecte par construction — l'ordre d'itération d'une `Map` n'est pas spécifié, et une page 2 pourrait répéter ou omettre des éléments vus en page 1. L'identifiant de l'agrégat porte pour cela un ordre naturel (`Comparable`) qui fournit la clé de départage sans obliger l'adapter à descendre jusqu'à la valeur enveloppée. Cet ordre naturel est technique, jamais une relation d'identité.
- **État mutable requis par une règle métier** (compteur, séquence, horloge) : il ne devient jamais un champ du domaine. On déclare un **port** dans le `domain`, son implémentation porte l'état en `infrastructure/secondary`, et la règle qui l'utilise vit dans une **fabrique de domaine**. Un adapter ne décide jamais s'il faut produire une valeur — on ne l'appelle que quand la décision est prise, sans quoi il lui faudrait connaître les invariants de l'agrégat.
- **Une donnée de paramétrage se traite comme de l'état mutable** : le domaine la reçoit par un port, il ne la code jamais en constante. Quand plusieurs responsabilités concourent à assembler une valeur (décision de générer, forme du résultat, éléments constitutifs), chacune reste isolée : la décision vit dans la fabrique de domaine, la forme dans le value object produit, chaque élément constitutif vient de son propre port. Un port qui rendrait un objet déjà assemblé oblige son adapter à connaître le reste du format — c'est le signe qu'il doit rendre la valeur brute et laisser le domaine assembler.
- **Pas d'assertion défensive dans les adapters** : `Assert` valide les invariants à la **construction** des objets du domaine. Un adapter qui reçoit un value object ou un agrégat le reçoit déjà valide — y rajouter un `Assert.notNull` duplique une garantie que le typage donne déjà, et ajoute une ligne à couvrir sans rien protéger.
- **Ubiquitous language en français** : les concepts métier (packages de bounded context, agrégats, entités, value objects, composants de records, paramètres métier, noms de champs passés à `Assert`) sont nommés en français, sans accents ni caractères spéciaux dans les identifiants Java. Les verbes et l'API techniques restent en anglais (`create`, `builder`, `build`, `get`, `newId`). Le code de `shared/` et `wire/` reste intégralement en anglais.
- **Une interface scellée ne se justifie que si ses sous-types diffèrent par un champ, un invariant ou un comportement.** Quand ils ne diffèrent que par leur identité de type, la variation est une **valeur** : un composant énuméré sur un agrégat unique, pas une hiérarchie. Deux sous-types identiques au nom près font payer chaque évolution de forme autant de fois qu'il y a de sous-types, imposent un `switch` à chaque lecture pour retrouver ce qu'on savait déjà, et dédoublent jusqu'aux méthodes des ports. Scinder plus tard, sur des différences réelles, coûte moins cher que de porter une hiérarchie vide.
- Un package de bounded context porte le nom de son agrégat. Attention : sans `module-info.java`, une interface `sealed` exige ses sous-types dans le **même package** — plusieurs agrégats coiffés par un type scellé vivent donc forcément dans un seul contexte.
- Package-level annotations à poser systématiquement via `package-info.java` :
  - `@com.glm.glmback.SharedKernel` : code partagé entre plusieurs bounded contexts.
  - `@com.glm.glmback.BusinessContext` : code privé à un seul bounded context — **ne jamais l'importer depuis un autre contexte**.
    Voir `documentation/package-types.md`.

### SOLID / isolation des responsabilités

- Une classe = une responsabilité ; ne pas mélanger orchestration (`application`) et logique métier (`domain`), ni logique métier et détails techniques (`infrastructure`).
- Programmer contre des interfaces définies dans `domain`/`application` (ports), injectées dans les adapters (DIP).
- Préférer plusieurs interfaces spécifiques à une interface fourre-tout (ISP), et des extensions par composition plutôt que par modification de code existant (OCP).

### TDD

- Cycle red-green-refactor obligatoire : écrire le test (unitaire, intégration ou scénario Cucumber) avant ou en même temps que le code de production correspondant.
- Le build impose une couverture Jacoco stricte (0 ligne/branche manquante par classe, cf. `pom.xml`) — un code non testé casse `mvn verify`. Ne jamais désactiver ce check pour contourner le problème.

## Stratégie de tests

### Outside-in : Cucumber

- Scénarios Gherkin dans `src/test/features/*.feature`.
- Runner : `com.glm.glmback.cucumber.CucumberTest` (`@Suite`, moteur JUnit Platform, tag filter `not @disabled`).
- Contexte Spring de test : `com.glm.glmback.cucumber.CucumberConfiguration` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, profil `test`).
- DSL d'assertions REST réutilisable dans `com.glm.glmback.cucumber.rest` (`CucumberRestClient`, `CucumberRestAssertions`) — l'utiliser plutôt que réinventer des appels HTTP dans les steps.
- Détails et exemples : `documentation/cucumber.md`.

### Inside-out : unitaire et intégration

- **Unitaire** (`*Test.java`, annotation `@UnitTest`) : pas de contexte Spring, JUnit 5 + AssertJ. À utiliser pour tester `domain` et `application` en isolation (mocks pour les ports).
- **Intégration** (`*IT.java`, annotation `@IntegrationTest`) : `@SpringBootTest` + `TestSecurityConfiguration` + `@WithMockUser`, profil `test`. À utiliser pour les adapters `infrastructure` (sécurité, config, éventuellement persistence).
- Note : il n'existe pas de tests ArchUnit malgré la dépendance `org.reflections` présente dans `pom.xml` — ne pas présumer que des tests d'architecture existent déjà. Testcontainers, en revanche, est actif : `src/test/resources/config/application-test.yml` pointe la datasource sur `jdbc:tc:postgresql`, donc **`mvn verify` exige que Docker soit démarré** — sans lui, tous les ITs et scénarios Cucumber échouent au chargement du contexte Spring.

### Fixtures

- Les jeux de données **valides et partagés** ne sont jamais déclarés dans un test : ils vivent dans une classe `*Fixture` par contexte borné, en `src/test/java` dans le package du domaine concerné, sur le modèle `public final class` à constructeur privé. Ces classes n'ont aucun état mutable et exposent des factories d'**au plus un argument**, les variantes se distinguant par leur nom plutôt que par une liste de paramètres.
- Le nom d'un membre de fixture **évoque la donnée, jamais son rôle** (une constante nommée d'après sa valeur plutôt que son usage). Un nom de rôle ment dès qu'un test emploie la valeur autrement — et oblige à ouvrir la fixture pour lire l'assertion. Les agrégats et value objects sont préfixés de leur type : l'import statique à joker ne montre au lecteur aucun type d'origine.
- Restent en clair dans le test trois choses, sans quoi il cesserait d'énoncer ce qu'il vérifie : les **valeurs invalides** des tests d'invariants (`null`, chaîne vide, valeur hors bornes), l'**objet sous test construit par son propre builder** — comparer une fixture à une fixture rend l'assertion circulaire, alors que confronter le builder alimenté en valeurs brutes aux value objects attendus prouve que le constructeur enveloppe correctement — et les **valeurs qui encodent la relation testée** (dates ordonnées d'un test de tri, sondes de bornes).
- Une fixture ne compte pas dans la couverture : JaCoCo n'analyse que `target/classes`.

### Commandes

- `mvn test` — tests unitaires uniquement (Surefire, exclut `*IT*` et `*CucumberTest*`).
- `mvn verify` — tests d'intégration + Cucumber (Failsafe) + check Jacoco. C'est la commande de référence avant de considérer une fonctionnalité terminée.

## Infrastructure locale

```bash
docker compose -f src/main/docker/postgresql.yml up -d
docker compose -f src/main/docker/keycloak.yml up -d
```

- PostgreSQL : schéma géré exclusivement par Liquibase (`spring.jpa.hibernate.ddl-auto: none`), point d'entrée `src/main/resources/config/liquibase/master.xml` (il porte les `property` de types et n'inclut rien d'autre que la racine des changelogs). Toute évolution de schéma passe par un nouveau changelog, jamais par `ddl-auto`.
- **Arborescence des changelogs, datée et hiérarchique**, sous `src/main/resources/config/liquibase/changelog/` : un répertoire par **année** (`2026/`), un par **mois** sur deux chiffres (`08/`), et dans le répertoire du mois les scripts nommés `NNN-nom_du_script.xml`, l'incrément repartant de `001` à chaque mois. **Chaque répertoire porte un `changelog.xml`**, y compris la racine : celui de la racine référence les années, celui d'une année ses mois, celui d'un mois ses scripts. Les `include` internes à l'arborescence sont tous `relativeToChangelogFile="true"`, ce qui rend chaque niveau autonome. Ajouter un script, c'est donc créer le fichier **et** l'inclure dans le `changelog.xml` de son mois — l'ordre d'exécution est celui des `include`, jamais un tri implicite du système de fichiers.
- **Multi-tenant : un schéma PostgreSQL par entreprise cliente**, désigné par le claim `tenant` du token Keycloak. `spring.liquibase.enabled` est volontairement à `false` — `TenantSchemasInitializer` crée et migre chaque schéma déclaré sous `application.multitenancy.tenants` au démarrage. Voir [documentation/multitenancy.md](documentation/multitenancy.md) avant de toucher à la persistance, à la sécurité ou aux tests d'intégration.
- Keycloak : realm `glmproject` importé depuis `src/main/docker/keycloak-realm-config/glmproject-realm.json`, issuer `http://localhost:9080/realms/glmproject`. Après modification du realm : `docker compose -f src/main/docker/keycloak.yml down -v` (`KC_DB=dev-file` ne réimporte que sur un volume neuf).
- Profils Spring : `application.yml` (base) et `application-local.yml` (logs DEBUG **et jeu d'entreprises de developpement**). Pas de profil `prod` pour l'instant. `application.yml` déclare `tenants: []` : les entreprises `impeccmold`/`katilys` sont une donnée de développement, portée par les profils `local` et `test` — lancer l'application sans profil ne crée donc aucun schéma.

## Conventions de code

- **Step builder au-delà de 3 arguments** : dès qu'un constructeur ou une factory expose plus de 3 arguments, fournir un step builder à étapes forcées — une interface par étape, chaque étape retournant l'interface de l'étape suivante, la dernière retournant l'objet construit (pas de `build()` optionnel, donc pas d'objet incomplet possible). Deux implémentations, selon le risque à couvrir.
  - **Chaîne de lambdas imbriquées** quand les étapes portent des types **tous distincts** et restent peu nombreuses — les quatre ports d'un service de domaine, voir `ElementsDeFabricationService`. Le constructeur privé prend alors la liste positionnelle : aucune inversion n'est possible, le compilateur les attrape toutes. Moitié moins de code, immutabilité structurelle, et aucun recast puisque chaque étape rend une lambda n'implémentant que son interface.
  - **Classe imbriquée `private static final`** dès que le constructeur prendrait sinon une longue liste positionnelle de **types interchangeables**, où une inversion compile sans bruit — deux `String` et deux `Instant` dans `Fiche` et `ElementDeFabrication`, patrons de référence. Elle implémente toutes les interfaces d'étapes, un champ **privé** par étape (`VisibilityModifier` de Checkstyle refuse tout autre niveau), chaque étape affectant son champ et rendant `this`, l'étape terminale rendant l'objet bâti. Les points ci-dessous précisent ce second cas.
  - **Le constructeur privé de l'objet bâti prend le builder**, jamais une liste positionnelle de N paramètres où deux valeurs de même type peuvent s'inverser silencieusement. C'est là, et nulle part ailleurs, que se construisent les entités et les value objects de l'objet bâti.
  - **Visibilité la plus étroite possible** : `builder()` et les interfaces d'étapes sont **package-private** dès lors que leurs appelants vivent dans le même package — ce qui est le cas de tout agrégat, construit depuis son propre domaine. Cette portée est ce qui compense la mutabilité du builder : hors du package, le recast d'une étape tardive vers une étape antérieure ne peut même pas s'écrire. Le patron garantit qu'aucune étape n'est sautable et qu'aucun objet incomplet n'est constructible ; il ne garantit pas, à l'intérieur du package, qu'une valeur ne soit reposée — le filet restant les `Assert` du constructeur canonique. Un appelant légitime hors du package élargit la portée d'autant, pas davantage : `ElementDeFabrication.builder()` est `public` parce que la relecture depuis la persistance se fait dans `infrastructure/secondary`. C'est la seule raison admise d'élargir — la création, elle, reste l'affaire du domaine.
  - **Type d'une étape** : une **primitive** (`String`, `Instant`, `long`) quand la valeur est brute et que l'objet bâti la compose lui-même en value object — `titre` et `description`, assemblés en `Fiche`. Le **type du domaine** quand l'appelant le détient déjà : l'identifiant de l'agrégat, ou le `Prefixe` issu du port de paramétrage. Le déballer pour que le constructeur le remballe ne gagne rien.
  - **Une étape porte la valeur finale, jamais ses ingrédients** : le step builder assemble, il ne fabrique pas. Le `Nom` se compose d'un préfixe, d'une année et d'un compteur, mais l'étape prend un `Nom` — sa fabrication (`Nom.of`) appartient à la **fabrique de domaine** qui détient les ports, ici `ElementsDeFabricationService`. Exposer les ingrédients obligerait la modification, qui conserve pourtant le nom existant, à le décomposer pour le reconstruire à l'identique : un aller-retour qui ne peut que se tromper.
  - Un value object porteur d'une règle de composition reste une **valeur simple** et garantit sa forme par une **assertion de motif** — ce qui vaut aussi pour une valeur relue depuis la persistance, qui n'a jamais transité par la fabrique.
  - Attention : un record public ne peut pas avoir de constructeur canonique privé (JLS 8.10.4) — sur un record, le builder est la voie idiomatique imposée par convention, le constructeur canonique restant techniquement accessible.
- **Rôles** : `GESTIONNAIRE` ouvre les actes métier (création, engagement, clôture, corrections), `USER` le pointage et la lecture. `ADMIN` est réservé à l'administration technique (`/api/admin/**`, `/management/**`) et **ne donne aucun accès métier** — ne jamais le remettre dans un `@Secured` de service applicatif. Les rôles se déclarent sur le service applicatif, jamais sur le contrôleur.
- **OpenAPI** : springdoc est au classpath, la configuration globale vit dans `wire/openapi`. Seul `atelier` est annoté (`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`) — c'est le patron à suivre pour les contextes suivants, `elementdefabrication` restant à reprendre.
- Checkstyle (`checkstyle.xml` à la racine) appliqué à `src/main` et `src/test` — respecter le style existant plutôt qu'introduire de nouvelles conventions.
- Formatage via Prettier (`npm run prettier:check` / `npm run prettier:format`), couvrant aussi les fichiers Gherkin.

## Documentation complémentaire

Ne pas dupliquer ce qui est déjà documenté — s'y référer :

- `documentation/package-types.md` — `SharedKernel` / `BusinessContext`.
- `documentation/assertions.md` — usage d'`Assert`.
- `documentation/postgresql.md` — configuration PostgreSQL.
- `documentation/logs-spy.md` — capture de logs en test.
- `documentation/cors-configuration.md` — configuration CORS.
- `documentation/multitenancy.md` — isolation par entreprise, schéma par tenant, utilisateurs de développement.
- `documentation/cucumber.md` — écriture des scénarios et du glue code Cucumber.
- `documentation/atelier-api.md` — guide d'intégration de l'API atelier pour le développeur front.
- `documentation/strategie/authentification-pointage.md` — stratégie retenue pour identifier l'opérateur au pupitre, sécuriser le poste et survivre à une coupure réseau.
