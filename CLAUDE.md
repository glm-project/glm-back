# CLAUDE.md

Ce fichier fournit à Claude Code les règles à respecter en permanence sur ce projet.

## Vue d'ensemble

`glmproject` (groupId `com.glm.glmback`) est un projet Spring Boot généré par **seed4j**, construit en **architecture hexagonale (ports & adapters)** et **Domain-Driven Design (DDD)**. Stack : Java 25, Maven, Spring Boot 4.0.6, PostgreSQL (via Liquibase), Keycloak (OAuth2/OIDC). Le scaffold actuel ne contient que du code technique partagé (`shared/`, `wire/`) — aucun bounded context métier n'a encore été créé. Toute nouvelle fonctionnalité doit suivre les règles ci-dessous dès sa création.

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
