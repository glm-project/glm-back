# CLAUDE.md

Ce fichier fournit à Claude Code les règles à respecter en permanence sur ce projet.

## Vue d'ensemble

`glmproject` (groupId `com.glm.glmback`) est un projet Spring Boot généré par **seed4j**, construit en **architecture hexagonale (ports & adapters)** et **Domain-Driven Design (DDD)**. Stack : Java 25, Maven, Spring Boot 4.0.6, PostgreSQL (via Liquibase), Keycloak (OAuth2/OIDC). En dehors du code technique partagé (`shared/` — dont le shared kernel `shared/pagination` qui fournit `Page<T>` et `Pageable` — et `wire/`), le seul bounded context métier existant est `elementdefabrication/` (agrégats `OrdreDeFabrication` et `Produit`, coiffés par les interfaces scellées `ElementDeFabrication` et `ElementDeFabricationId`). Il expose le port `ElementDeFabricationRepository` dans son `domain`, implémenté pour l'instant par un adapter in-memory dans `infrastructure/secondary` ; il n'a pas encore de couche `application` ni d'adapter primaire. Toute nouvelle fonctionnalité doit suivre les règles ci-dessous dès sa création.

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
- **Accès aux value objects** : aucun accesseur ne porte le préfixe `get` — on lit toujours par le nom de la propriété. Un value object qui enveloppe une **valeur primitive** (String, UUID, nombre) nomme son unique composant `value`, et son accesseur généré `value()` est le seul point de lecture : ne jamais écrire de méthode `get()` déléguant à l'accesseur du record. Deux cas gardent au contraire le nom de leur propriété : un record à plusieurs composants ou dont l'unique composant est lui-même un type métier (`Fiche.titre()`, `ElementDeFabricationCriteria.periode()`), et un **identifiant**, dont le composant est nommé d'après ce qu'il enveloppe — `uuid` (`OrdreDeFabricationId.uuid()`, déclaré par `ElementDeFabricationId`). Le type englobant dit déjà que c'est un identifiant : le nommer `id` produirait `id.id()` chez tout appelant, et `value` perdrait l'information de représentation. L'étiquette passée à `Assert` reste en revanche le **nom métier** (`Assert.field("titre", value)`) : c'est elle qui remontera au client dans un message d'erreur. Le code seed4j de `shared/authentication` (`Username.get()`, `Roles.get()`) est antérieur à cette règle et reste tel quel pour rester régénérable.
- **Domaine immuable, sans mutation d'état** : aucun setter, aucune méthode de type `rename`/`describe` sur un agrégat ou un value object — ce sont des setters déguisés, qui n'expriment aucune intention métier. Un changement d'état, c'est la construction d'un nouvel agrégat complet (même identité, nouvelle `dateDeModification`) suivie d'un `update` explicite sur le repository. Le seul état mutable autorisé vit dans les adapters `infrastructure/secondary`, confiné derrière un port.
- **Repository** : le port est une interface du `domain` nommée d'après l'agrégat, son implémentation vit dans `infrastructure/secondary`. Sémantique stricte et explicite plutôt qu'un `save` fourre-tout : `create` échoue si l'identité existe déjà, `update` et `delete` échouent si elle est inconnue, chacun avec une exception métier propre au bounded context (jamais un nouveau `AssertionErrorType`, qui appartient au shared kernel). Modèle de référence : `elementdefabrication/domain/ElementDeFabricationRepository` et son adapter `InMemoryElementDeFabricationRepository`. La lecture d'une collection ne rend jamais une liste nue : elle prend un objet de critères portant les règles de sélection (`matches` vit dans le `domain`, jamais dans l'adapter) et un `Pageable`, et rend une `Page<T>` (`shared/pagination/domain`). Toute lecture paginée impose un **tri total déterministe** côté adapter, sans quoi la pagination est incorrecte par construction — l'ordre d'itération d'une `Map` n'est pas spécifié, et une page 2 pourrait répéter ou omettre des éléments vus en page 1. C'est la raison d'être du `Comparable` porté par l'identifiant (`ElementDeFabricationId`) : il fournit la clé de départage sans obliger l'adapter à descendre jusqu'à la valeur enveloppée. Cet ordre naturel est technique, jamais une relation d'identité — deux identifiants de types différents mais de même UUID se comparent à `0` sans être `equals`, donc ne jamais placer d'identifiants dans un `TreeSet`/`TreeMap`.
- **Pas d'assertion défensive dans les adapters** : `Assert` valide les invariants à la **construction** des objets du domaine. Un adapter qui reçoit un value object ou un agrégat le reçoit déjà valide — y rajouter un `Assert.notNull` duplique une garantie que le typage donne déjà, et ajoute une ligne à couvrir sans rien protéger.
- **Ubiquitous language en français** : les concepts métier (packages de bounded context, agrégats, entités, value objects, composants de records, paramètres métier, noms de champs passés à `Assert`) sont nommés en français, sans accents ni caractères spéciaux dans les identifiants Java (`OrdreDeFabrication`, `Fiche`, `Titre`, `dateDeCreation`). Les verbes et l'API techniques restent en anglais (`create`, `rename`, `builder`, `build`, `get`, `newId`). Le code de `shared/` et `wire/` reste intégralement en anglais.
- Un package de bounded context porte le nom de son agrégat (ex. `elementdefabrication` pour `ElementDeFabrication`). Attention : sans `module-info.java`, une interface `sealed` exige ses sous-types dans le **même package** — plusieurs agrégats coiffés par un type scellé vivent donc forcément dans un seul contexte.
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
- Note : il n'existe pas d'ArchUnit ni de Testcontainers actif dans les tests malgré les dépendances `org.reflections`/`testcontainers-postgresql` présentes dans `pom.xml` — ne pas présumer que des tests d'architecture ou des conteneurs de test existent déjà.

### Fixtures

- Les jeux de données **valides et partagés** ne sont jamais déclarés dans un test : ils vivent dans une classe `*Fixture` par contexte borné (`ElementsDeFabricationFixture`, `PaginationFixture`), en `src/test/java` dans le package du domaine concerné, sur le modèle `public final class` à constructeur privé de `JsonHelper`. Ces classes n'ont aucun état mutable et exposent des factories d'**au plus un argument**, les variantes se distinguant par leur nom (`ordreDeFabricationAssemblageCarter(id)`, `ordreDeFabricationAssemblageCarterCreeLe(date)`) plutôt que par une liste de paramètres.
- Le nom d'un membre de fixture **évoque la donnée, jamais son rôle** : `titreAssemblageCarter()` et non `titre()`, `LE_20_FEVRIER_2026` et non `DATE_DE_MODIFICATION`. Un nom de rôle ment dès qu'un test emploie la valeur autrement — la date de modification de l'un est la date de création de l'autre — et oblige à ouvrir la fixture pour lire l'assertion. Les agrégats et value objects sont préfixés de leur type (`ficheAssemblageCarter()`, `ordreDeFabricationAssemblageCarter()`) : l'import statique à joker ne montre au lecteur aucun type d'origine.
- Restent en clair dans le test trois choses, sans quoi il cesserait d'énoncer ce qu'il vérifie : les **valeurs invalides** des tests d'invariants (`null`, `" "`, `"a".repeat(256)`), l'**objet sous test construit par son propre builder** — comparer une fixture à une fixture rend l'assertion circulaire, alors que confronter `OrdreDeFabrication.builder()` à `fiche()` prouve que le builder de l'agrégat délègue bien à celui du value object — et les **valeurs qui encodent la relation testée** (dates ordonnées d'un test de tri, sondes de bornes `plusSeconds(1)`).
- Une fixture ne compte pas dans la couverture : JaCoCo n'analyse que `target/classes`.

### Commandes

- `mvn test` — tests unitaires uniquement (Surefire, exclut `*IT*` et `*CucumberTest*`).
- `mvn verify` — tests d'intégration + Cucumber (Failsafe) + check Jacoco. C'est la commande de référence avant de considérer une fonctionnalité terminée.

## Infrastructure locale

```bash
docker compose -f src/main/docker/postgresql.yml up -d
docker compose -f src/main/docker/keycloak.yml up -d
```

- PostgreSQL : schéma géré exclusivement par Liquibase (`spring.jpa.hibernate.ddl-auto: none`), changelogs dans `src/main/resources/config/liquibase/changelog/`, master dans `src/main/resources/config/liquibase/master.xml`. Toute évolution de schéma passe par un nouveau changelog, jamais par `ddl-auto`.
- Keycloak : realm `glmproject` importé depuis `src/main/docker/keycloak-realm-config/glmproject-realm.json`, issuer `http://localhost:9080/realms/glmproject`.
- Profils Spring : `application.yml` (base) et `application-local.yml` (logs DEBUG). Pas de profil `prod` pour l'instant.

## Conventions de code

- **Step builder immuable au-delà de 3 arguments** : dès qu'un constructeur ou une factory expose plus de 3 arguments, fournir un step builder à étapes forcées — une interface fonctionnelle par étape, chaque étape retournant l'interface de l'étape suivante, la dernière retournant l'objet construit (pas de `build()` optionnel, donc pas d'objet incomplet possible). Le builder ne doit porter **aucun état mutable** : l'implémenter par une chaîne de lambdas imbriquées, les valeurs étant capturées dans les fermetures. Le builder d'un agrégat délègue à celui de ses value objects plutôt que de dupliquer les étapes. Modèles de référence : `elementdefabrication/domain/Fiche` et `elementdefabrication/domain/OrdreDeFabrication`. Attention : un record public ne peut pas avoir de constructeur canonique privé (JLS 8.10.4) — sur un record, le builder est la voie idiomatique imposée par convention, le constructeur canonique restant techniquement accessible. Le builder de `shared/error/domain/NotAfterTimeException` (champs réaffectés à chaque étape) est antérieur à cette règle et ne doit pas servir de modèle.
- Checkstyle (`checkstyle.xml` à la racine) appliqué à `src/main` et `src/test` — respecter le style existant plutôt qu'introduire de nouvelles conventions.
- Formatage via Prettier (`npm run prettier:check` / `npm run prettier:format`), couvrant aussi les fichiers Gherkin.

## Documentation complémentaire

Ne pas dupliquer ce qui est déjà documenté — s'y référer :

- `documentation/package-types.md` — `SharedKernel` / `BusinessContext`.
- `documentation/assertions.md` — usage d'`Assert`.
- `documentation/postgresql.md` — configuration PostgreSQL.
- `documentation/logs-spy.md` — capture de logs en test.
- `documentation/cors-configuration.md` — configuration CORS.
- `documentation/cucumber.md` — écriture des scénarios et du glue code Cucumber.
