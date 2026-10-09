# Codes d'erreur

Chaque erreur métier sort en `ProblemDetail` (RFC 7807) dont le champ `type` porte un **code stable** :

```
urn:glm:erreur:<contexte>:<code>
```

```json
{
  "type": "urn:glm:erreur:atelier:identifiant-evenement-reutilise",
  "title": "identifiant d'evenement reutilise",
  "status": 409,
  "message": "Identifiant deja reserve pour un autre geste"
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

| Code                                 | Statut | `title`                            | Exception                                 |
| ------------------------------------ | ------ | ---------------------------------- | ----------------------------------------- |
| `suivi-d-atelier-introuvable`        | 404    | suivi d'atelier introuvable        | `SuiviDAtelierIntrouvableException`       |
| `evenement-d-atelier-introuvable`    | 404    | evenement d'atelier introuvable    | `EvenementDAtelierIntrouvableException`   |
| `element-de-fabrication-introuvable` | 404    | element de fabrication introuvable | `ElementEngageableIntrouvableException`   |
| `operateur-introuvable`              | 404    | operateur introuvable              | `OperateurDAtelierIntrouvableException`   |
| `poste-de-travail-introuvable`       | 404    | poste de travail introuvable       | `PosteDAtelierIntrouvableException`       |
| `activite-visee-introuvable`         | 404    | activite visee introuvable         | `ActiviteViseeIntrouvableException`       |
| `operateur-non-habilite`             | 409    | operateur non habilite             | `OperateurNonHabiliteException`           |
| `activite-visee-incoherente`         | 409    | activite visee incoherente         | `ActiviteViseeIncoherenteException`       |
| `element-deja-engage`                | 409    | element deja engage                | `ElementDejaEngageException`              |
| `evenement-deja-annule`              | 409    | evenement deja annule              | `EvenementDejaAnnuleException`            |
| `suivi-d-atelier-cloture`            | 409    | suivi d'atelier cloture            | `SuiviDAtelierClotureException`           |
| `evenement-anterieur-a-l-engagement` | 409    | evenement anterieur a l'engagement | `EvenementAvantEngagementException`       |
| `saisie-concurrente`                 | 409    | saisie concurrente                 | `SaisieConcurrenteException`              |
| `identifiant-evenement-reutilise`    | 409    | identifiant d'evenement reutilise  | `IdentifiantDEvenementReutiliseException` |
| `date-de-survenue-future`            | 400    | date de survenue future            | `DateDeSurvenueFutureException`           |
| `proposition-invalide`               | 400    | proposition invalide               | `PropositionInvalideException`            |
| `nature-d-anomalie-invalide`         | 400    | nature d'anomalie invalide         | `NatureDAnomalieInvalideException`        |
| `apercu-obsolete`                    | 409    | apercu obsolete                    | `ApercuObsoleteException`                 |
| `confirmation-reutilisee`            | 409    | confirmation reutilisee            | `ConfirmationReutiliseeException`         |

`proposition-invalide` refuse une adresse incohérente avec le suivi de la route ou l'acte, une identité
prospective absente pour une création, présente pour une annulation ou égale à la commande.
`nature-d-anomalie-invalide` refuse un paramètre `nature` absent ou inconnu sur `GET /api/atelier/anomalies`.
`apercu-obsolete` refuse une adresse qui n'ouvre ni conflit ni fin automatique, une révision dépassée ou des conséquences
matériellement différentes, échéance métier franchie, habilitation retirée ou ressource disparue comprises.
Il impose un nouvel aperçu. `confirmation-reutilisee` refuse une commande déjà enregistrée avec une autre
proposition ou une autre identité authentifiée ; le client vérifie son reçu canonique.

`saisie-concurrente` est le seul code sur lequel **rejouer** l'appel est la bonne réaction : la saisie était valide,
un autre pointage s'est glissé entre la lecture et l'écriture.

`identifiant-evenement-reutilise` refuse un identifiant de geste déjà réservé pour un autre contenu : autre nature,
suivi, opérateur, type, poste ou date fournie, et pour un pointage d'atelier autre intention ou autre cible.

`activite-visee-introuvable` et `activite-visee-incoherente` refusent une transition ou une fin dont la cible n'est
pas une activité de ce suivi, ou appartient à un autre opérateur ou à un autre poste. Ils valent pour le pointage, la
régularisation et la correction, et sont définitifs : le même geste rejoué reçoit le même refus.

Aucun code ne refuse un geste qui contredit le journal d'un élément : sa cible déjà terminée ou remplacée à son heure,
son ouvrant annulé, une transition vers sa propre catégorie. Pointage, régularisation, correction et annulation
l’enregistrent, et sa séquence est en conflit jusqu’à ce que le gestionnaire la résolve.

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

Les refus définitifs des gestes d’activité sont `activite-visee-introuvable`, `activite-visee-incoherente`, `operateur-introuvable`,
`poste-de-travail-introuvable`, `suivi-d-atelier-introuvable`, `identifiant-evenement-reutilise`,
`operateur-non-habilite`, `evenement-anterieur-a-l-engagement` et `date-de-survenue-future`. `suivi-d-atelier-cloture` n'y sort
plus que pour un démarrage ou une non conformité — la seule erreur à afficher à l'opérateur. `saisie-concurrente`
n'y remonte qu'après trois essais du serveur. Un pointage d'atelier qui contredit le journal n'y est jamais refusé.
