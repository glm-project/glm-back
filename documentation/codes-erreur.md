# Codes d'erreur

Chaque erreur métier sort en `ProblemDetail` (RFC 7807) dont le champ `type` porte un **code stable** :

```
urn:glm:erreur:<contexte>:<code>
```

```json
{
  "type": "urn:glm:erreur:atelier:suivi-d-atelier-cloture",
  "title": "suivi d'atelier cloture",
  "status": 409,
  "message": "Le suivi d'atelier 6d0c1a4e-... est cloture"
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

Chaque URN est épinglé caractère par caractère par un `*ExceptionAdviceTest`, qui n'apporte que sa table `(exception,
URN, statut)` et hérite ses deux vérifications d'`ExceptionAdviceContract` : l'épinglage ligne à ligne, et un test
d'exhaustivité comparant l'ensemble des exceptions traduites à l'ensemble des exceptions éprouvées — **ajouter un
handler sans ligne de table passe le build au rouge**. La table ne porte pas de colonne `title` : le titre n'est pas
un contrat, l'épingler ferait échouer le build sur une reformulation que ce document autorise. Ce que la mécanique
elle-même produit — composition de l'URN, report du statut, du titre et du `message` — est éprouvé une fois par
`ProblemCodeTest`. Quatre scénarios Cucumber vérifient en plus la forme sérialisée, sur un 404 et un 409 de deux
contextes différents — les tests unitaires appellent le handler, eux seuls prouvent que le `type` arrive bien dans le
corps JSON. Les codes n'entrent pas dans `openapi.json` (les `@ApiResponse` d'erreur ne portent pas de schéma) : ces
tests sont le seul endroit qui les tient.

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

| Code                                 | Statut | `title`                            | Exception                               |
| ------------------------------------ | ------ | ---------------------------------- | --------------------------------------- |
| `suivi-d-atelier-introuvable`        | 404    | suivi d'atelier introuvable        | `SuiviDAtelierIntrouvableException`     |
| `fin-automatique-introuvable`        | 404    | fin automatique introuvable        | `FinAutomatiqueIntrouvableException`    |
| `element-de-fabrication-introuvable` | 404    | element de fabrication introuvable | `ElementEngageableIntrouvableException` |
| `operateur-introuvable`              | 404    | operateur introuvable              | `OperateurDAtelierIntrouvableException` |
| `poste-de-travail-introuvable`       | 404    | poste de travail introuvable       | `PosteDAtelierIntrouvableException`     |
| `activite-visee-introuvable`         | 404    | activite visee introuvable         | `ActiviteViseeIntrouvableException`     |
| `operateur-non-habilite`             | 409    | operateur non habilite             | `OperateurNonHabiliteException`         |
| `element-deja-engage`                | 409    | element deja engage                | `ElementDejaEngageException`            |
| `suivi-d-atelier-cloture`            | 409    | suivi d'atelier cloture            | `SuiviDAtelierClotureException`         |
| `evenement-anterieur-a-l-engagement` | 409    | evenement anterieur a l'engagement | `EvenementAvantEngagementException`     |
| `activite-non-echue`                 | 409    | activite non echue                 | `ActiviteNonEchueException`             |
| `activite-deja-regularisee`          | 409    | activite deja regularisee          | `ActiviteDejaRegulariseeException`      |
| `fin-avant-debut`                    | 409    | fin avant debut                    | `FinAvantDebutException`                |
| `fin-apres-borne`                    | 409    | fin apres borne                    | `FinApresBorneException`                |
| `saisie-concurrente`                 | 409    | saisie concurrente                 | `SaisieConcurrenteException`            |
| `pointage-ignore`                    | 409    | pointage ignore                    | `PointageIgnoreException`               |
| `date-de-survenue-future`            | 400    | date de survenue future            | `DateDeSurvenueFutureException`         |

`fin-automatique-introuvable` répond à `GET /api/atelier/suivis/{id}/anomalies/{pointage}` quand le pointage n'ouvre
aucune fin automatique non régularisée : le front revient à la liste.

Les quatre refus de `POST /api/atelier/suivis/{id}/regularisations` propres à la régularisation directe, avec
`activite-visee-introuvable` (404) et `date-de-survenue-future` (400) : `activite-non-echue` (l'activité n'est pas une
fin automatique : échéance non atteinte, ou terminée par une fin réelle, pointage ou clôture), `activite-deja-regularisee` (une régularisation
vise déjà l'activité), `fin-avant-debut` (l'heure n'est pas postérieure au début de l'activité : aucune activité n'a une durée nulle) et `fin-apres-borne` (l'heure dépasse
le début suivant sur la clé ou la clôture, `borneDeFin` du dossier). Un renvoi du même `id` déjà dans la table des événements répond 200 avant toute
règle. Hors ce cas, `activite-non-echue` tant que l'échéance n'est pas atteinte n'est pas définitif : le même geste
rejoué après l'échéance est accepté. `activite-deja-regularisee`, `fin-avant-debut` et `fin-apres-borne` ne changent
pas en rejouant (la borne ne bouge qu'avec un nouveau pointage ou une clôture déplacée).

`saisie-concurrente` est le seul code sur lequel **rejouer** l'appel est la bonne réaction : la saisie était valide,
un autre pointage s'est glissé entre la lecture et l'écriture.

`pointage-ignore` répond à un pointage (`POST /api/atelier/suivis/{id}/pointages`) que la règle de réception ignore : il
ne s'accorde pas à l'état de sa clé (opérateur, élément, poste) et part dans la table d'audit, sans entrer au journal.
Les quatre raisons d'audit (`DEJA_EN_COURS`, `AUCUNE_ACTIVITE`, `APRES_ECHEANCE`, `ANTERIEUR`) ne sortent pas dans la
réponse. Le refus ne s'affiche pas à l'opérateur : le pupitre retire l'effet local du pointage et se recale sur le
référentiel. Renvoyer l'identifiant d'un pointage déjà ignoré répond le même refus, sans nouvelle ligne d'audit. Seul
`suivi-d-atelier-cloture`, pour un démarrage ou une non conformité sur un élément clôturé, est un refus à afficher.

`activite-visee-introuvable` répond à la régularisation directe dont l'activité n'est pas une activité de ce suivi. Il est
définitif : la même saisie rejouée reçoit le même refus. Les pointages ne désignent plus d'activité.

Aucun code ne refuse un pointage qui contredit le journal d'un élément : la règle de réception l'ignore (`pointage-ignore`)
et l'audite.

### `operateur` — `urn:glm:erreur:operateur:`

| Code                           | Statut | `title`                      | Exception                              |
| ------------------------------ | ------ | ---------------------------- | -------------------------------------- |
| `operateur-introuvable`        | 404    | operateur introuvable        | `OperateurIntrouvableException`        |
| `poste-de-travail-introuvable` | 404    | poste de travail introuvable | `PosteHabilitableIntrouvableException` |
| `operateur-ayant-pointe`       | 409    | operateur ayant pointe       | `OperateurAPointeException`            |
| `identite-deja-utilisee`       | 409    | identite deja utilisee       | `IdentiteDejaUtiliseeException`        |
| `identifiant-deja-utilise`     | 409    | identifiant deja utilise     | `IdentifiantDejaUtiliseException`      |

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
| `categorie-inconnue`                 | 409    | categorie inconnue                 | `CategorieInconnueException`               |

### `categorie-de-produit` — `urn:glm:erreur:categorie-de-produit:`

| Code                       | Statut | `title`                  | Exception                         |
| -------------------------- | ------ | ------------------------ | --------------------------------- |
| `categorie-introuvable`    | 404    | categorie introuvable    | `CategorieIntrouvableException`   |
| `categorie-deja-existante` | 409    | categorie deja existante | `CategorieDejaExistanteException` |
| `categorie-utilisee`       | 409    | categorie utilisee       | `CategorieUtiliseeException`      |
| `ordre-incomplet`          | 409    | ordre incomplet          | `OrdreIncompletException`         |

### `nature-de-travail` — `urn:glm:erreur:nature-de-travail:`

| Code                    | Statut | `title`               | Exception                      |
| ----------------------- | ------ | --------------------- | ------------------------------ |
| `nature-introuvable`    | 404    | nature introuvable    | `NatureIntrouvableException`   |
| `nature-deja-existante` | 409    | nature deja existante | `NatureDejaExistanteException` |

`nature-deja-existante` refuse un libellé qu'une autre nature porte déjà, à la casse, aux accents ou aux espaces près,
à la déclaration comme au renommage.

### `parametrage` — `urn:glm:erreur:parametrage:`

| Code               | Statut | `title`          | Exception                  |
| ------------------ | ------ | ---------------- | -------------------------- |
| `logo-invalide`    | 400    | logo invalide    | `LogoInvalideException`    |
| `logo-introuvable` | 404    | logo introuvable | `LogoIntrouvableException` |

`logo-invalide` refuse un fichier trop lourd, illisible, d'un autre format que PNG ou JPEG, ou d'autres dimensions
que 50 x 50 pixels. Son `message` dit laquelle de ces règles il enfreint, avec la valeur reçue : il s'affiche tel quel.
`logo-introuvable` répond à une adresse d'image dont la version n'est plus celle du logo courant, ou à une entreprise
sans logo : le client relit la version dans `GET /api/parametrage`.

### `feuille-de-temps` — `urn:glm:erreur:feuille-de-temps:`

| Code                    | Statut | `title`                    | Exception                   |
| ----------------------- | ------ | -------------------------- | --------------------------- |
| `operateur-introuvable` | 404    | operateur introuvable      | `OperateurInconnuException` |
| `evaluation-future`     | 400    | instant d'evaluation futur | `EvaluationFutureException` |

### `synthese-des-heures` — `urn:glm:erreur:synthese-des-heures:`

| Code                    | Statut | `title`                    | Exception                   |
| ----------------------- | ------ | -------------------------- | --------------------------- |
| `operateur-introuvable` | 404    | operateur introuvable      | `OperateurInconnuException` |
| `evaluation-future`     | 400    | instant d'evaluation futur | `EvaluationFutureException` |

## Ajouter une erreur

1. Une constante dans l'`enum` du contexte — le nom est le code, choisi une fois pour toutes.
2. Le handler dans l'advice : `return ErreurDXxx.MA_CONSTANTE.problem(e);`.
3. Une ligne `new PublishedProblem(exception, URN, statut)` dans la table de l'`*ExceptionAdviceTest`, avec l'URN
   écrite en toutes lettres. Sans elle, le test d'exhaustivité échoue.
4. Une ligne dans le catalogue ci-dessus. Celle-là, aucun test ne la réclame : le catalogue est tenu à la main, et
   c'est la seule pièce du contrat qui puisse se démoder en silence.

Les refus définitifs des gestes d’activité sont `activite-visee-introuvable` (régularisation), `operateur-introuvable`,
`poste-de-travail-introuvable`, `suivi-d-atelier-introuvable`, `operateur-non-habilite`,
`evenement-anterieur-a-l-engagement` et `date-de-survenue-future`. `suivi-d-atelier-cloture` n'y sort plus que pour un
démarrage ou une non conformité — la seule erreur à afficher à l'opérateur. `pointage-ignore` est un refus définitif
que le pupitre ne montre jamais. `saisie-concurrente` n'y remonte qu'après trois essais du serveur.
