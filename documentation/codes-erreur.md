# Codes d'erreur

Chaque erreur métier sort en `ProblemDetail` (RFC 7807) dont le champ `type` porte un **code stable** :

```
urn:glm:erreur:<contexte>:<code>
```

```json
{
  "type": "urn:glm:erreur:atelier:transition-d-atelier-interdite",
  "title": "transition d'atelier interdite",
  "status": 409,
  "message": "une REPRISE suppose une PAUSE en cours"
}
```

C'est **`type` que le client teste**, jamais `title`. Un client qui branche sur `status` + `title` branche sur une
phrase française : elle se reformule à la première relecture, et plusieurs contextes en publient la même
(« operateur introuvable » comme « poste de travail introuvable » sortent chacun de trois advices). Le segment de
contexte est précisément ce qui les sépare.

Les trois champs ont trois usages distincts :

| Champ     | Pour qui       | Stabilité                                                                    |
| --------- | -------------- | ---------------------------------------------------------------------------- |
| `type`    | le code client | **contrat** — ne change pas sans version                                     |
| `title`   | le journal     | indicatif, peut être reformulé                                               |
| `message` | l'utilisateur  | rédigé par le domaine, à afficher tel quel (cf. `atelier-api.md`, § Erreurs) |

## Où vit le code

Un `enum` par bounded context, dans son `infrastructure/primary`, implémentant
`shared/error/infrastructure/primary/ProblemCode` : `ErreurDAtelier`, `ErreurDOperateur`, `ErreurDePosteDeTravail`,
`ErreurDElementDeFabrication`, `ErreurDeFeuilleDeTemps`. Chaque constante porte son statut HTTP et son titre, et
l'advice s'y réduit à une ligne par exception traduite.

**Le nom de la constante est le contrat publié.** `ProblemCode` en dérive le segment de code par
`name().toLowerCase(ROOT).replace('_', '-')` — renommer `SAISIE_CONCURRENTE` change ce que reçoit le front. C'est
voulu : l'identifiant vit dans le fichier de contrat, dont la seule raison de changer est que le contrat change. Le
piège écarté est `e.getClass().getSimpleName()`, qui aurait fait d'un refactoring d'exception une rupture d'API
silencieuse.

Chaque URN est épinglé caractère par caractère par un `*ExceptionAdviceTest`, et un test d'exhaustivité par advice
compare l'ensemble des exceptions traduites à l'ensemble des exceptions éprouvées : **ajouter un handler sans ligne de
table passe le build au rouge**. Quatre scénarios Cucumber vérifient en plus la forme sérialisée, sur un 404 et un 409
de deux contextes différents — les tests unitaires appellent le handler, eux seuls prouvent que le `type` arrive bien
dans le corps JSON. Les codes n'entrent pas dans `openapi.json` (les `@ApiResponse` d'erreur ne portent pas de
schéma) : ces tests sont le seul endroit qui les tient.

## Ce que le catalogue ne couvre pas

- **401 et 403** : produits par la chaîne de filtres, aucun advice ne s'exécute, le corps est vide. Le front les
  traite par intercepteur, sur le statut seul.
- **400 de Bean Validation** : `BeanValidationErrorsHandler`, déjà structuré par un `errors` en `Map<champ, message>`.
- **`AuthenticationExceptionAdvice`** (401 « not authenticated », 500 « unknown authentication ») : ces deux-là
  passent bien par un advice, mais ce sont des erreurs techniques, pas des refus métier. Elles sortent encore en
  `about:blank` — à reprendre le jour où le front en aura besoin.
- **500** : une `AssertionException` non mappée n'a pas de code métier — c'est un défaut, pas un cas d'usage.

## Catalogue

### `atelier` — `urn:glm:erreur:atelier:`

| Code                                 | Statut | `title`                            | Exception                                 |
| ------------------------------------ | ------ | ---------------------------------- | ----------------------------------------- |
| `suivi-d-atelier-introuvable`        | 404    | suivi d'atelier introuvable        | `SuiviDAtelierIntrouvableException`       |
| `journee-de-travail-introuvable`     | 404    | journee de travail introuvable     | `JourneeDeTravailIntrouvableException`    |
| `evenement-d-atelier-introuvable`    | 404    | evenement d'atelier introuvable    | `EvenementDAtelierIntrouvableException`   |
| `evenement-de-presence-introuvable`  | 404    | evenement de presence introuvable  | `EvenementDePresenceIntrouvableException` |
| `element-de-fabrication-introuvable` | 404    | element de fabrication introuvable | `ElementEngageableIntrouvableException`   |
| `operateur-introuvable`              | 404    | operateur introuvable              | `OperateurDAtelierIntrouvableException`   |
| `poste-de-travail-introuvable`       | 404    | poste de travail introuvable       | `PosteDAtelierIntrouvableException`       |
| `aucune-journee-de-travail-en-cours` | 404    | aucune journee de travail en cours | `AucuneJourneeDeTravailEnCoursException`  |
| `operateur-non-habilite`             | 409    | operateur non habilite             | `OperateurNonHabiliteException`           |
| `element-deja-engage`                | 409    | element deja engage                | `ElementDejaEngageException`              |
| `journee-de-travail-deja-ouverte`    | 409    | journee de travail deja ouverte    | `JourneeDeTravailDejaOuverteException`    |
| `evenement-deja-annule`              | 409    | evenement deja annule              | `EvenementDejaAnnuleException`            |
| `evenement-de-presence-deja-annule`  | 409    | evenement de presence deja annule  | `EvenementDePresenceDejaAnnuleException`  |
| `suivi-d-atelier-cloture`            | 409    | suivi d'atelier cloture            | `SuiviDAtelierClotureException`           |
| `transition-d-atelier-interdite`     | 409    | transition d'atelier interdite     | `TransitionDAtelierInterditeException`    |
| `transition-de-presence-interdite`   | 409    | transition de presence interdite   | `TransitionDePresenceInterditeException`  |
| `evenement-anterieur-a-l-engagement` | 409    | evenement anterieur a l'engagement | `EvenementAvantEngagementException`       |
| `saisie-concurrente`                 | 409    | saisie concurrente                 | `SaisieConcurrenteException`              |

`saisie-concurrente` est le seul code sur lequel **rejouer** l'appel est la bonne réaction : la saisie était valide,
un autre pointage s'est glissé entre la lecture et l'écriture.

### `operateur` — `urn:glm:erreur:operateur:`

| Code                           | Statut | `title`                      | Exception                              |
| ------------------------------ | ------ | ---------------------------- | -------------------------------------- |
| `operateur-introuvable`        | 404    | operateur introuvable        | `OperateurIntrouvableException`        |
| `poste-de-travail-introuvable` | 404    | poste de travail introuvable | `PosteHabilitableIntrouvableException` |
| `operateur-ayant-pointe`       | 409    | operateur ayant pointe       | `OperateurAPointeException`            |
| `identite-deja-utilisee`       | 409    | identite deja utilisee       | `IdentiteDejaUtiliseeException`        |
| `matricule-deja-utilise`       | 409    | matricule deja utilise       | `MatriculeDejaUtiliseException`        |

`urn:glm:erreur:operateur:poste-de-travail-introuvable` n'est pas
`urn:glm:erreur:poste-de-travail:poste-de-travail-introuvable` : le premier refuse une habilitation qu'on posait sur
un opérateur, le second une lecture du référentiel des postes. Même titre, même statut, deux contextes — c'est le
segment de contexte, et lui seul, qui les distingue.

### `poste-de-travail` — `urn:glm:erreur:poste-de-travail:`

| Code                           | Statut | `title`                      | Exception                            |
| ------------------------------ | ------ | ---------------------------- | ------------------------------------ |
| `poste-de-travail-introuvable` | 404    | poste de travail introuvable | `PosteDeTravailIntrouvableException` |
| `libelle-deja-utilise`         | 409    | libelle deja utilise         | `LibelleDejaUtiliseException`        |
| `poste-de-travail-pointe`      | 409    | poste de travail pointe      | `PosteDeTravailPointeException`      |
| `poste-de-travail-utilise`     | 409    | poste de travail utilise     | `PosteDeTravailUtiliseException`     |

### `element-de-fabrication` — `urn:glm:erreur:element-de-fabrication:`

| Code                                 | Statut | `title`                            | Exception                                  |
| ------------------------------------ | ------ | ---------------------------------- | ------------------------------------------ |
| `element-de-fabrication-introuvable` | 404    | element de fabrication introuvable | `ElementDeFabricationIntrouvableException` |
| `reference-deja-utilisee`            | 409    | reference deja utilisee            | `ReferenceDejaUtiliseeException`           |

### `feuille-de-temps` — `urn:glm:erreur:feuille-de-temps:`

| Code                    | Statut | `title`               | Exception                   |
| ----------------------- | ------ | --------------------- | --------------------------- |
| `operateur-introuvable` | 404    | operateur introuvable | `OperateurInconnuException` |

## Ajouter une erreur

1. Une constante dans l'`enum` du contexte — le nom est le code, choisi une fois pour toutes.
2. Le handler dans l'advice : `return ErreurDXxx.MA_CONSTANTE.problem(e);`.
3. Une ligne dans la table de l'`*ExceptionAdviceTest`, avec l'URN écrit en toutes lettres. Sans elle, le test
   d'exhaustivité échoue.
4. Une ligne dans le catalogue ci-dessus. Celle-là, aucun test ne la réclame : le catalogue est tenu à la main, et
   c'est la seule pièce du contrat qui puisse se démoder en silence.
