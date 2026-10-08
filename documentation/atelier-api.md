# API atelier — guide d'intégration front

Ce document accompagne la spécification OpenAPI ([`openapi.json`](openapi.json), servie sur `/swagger-ui.html` et
`/v3/api-docs`). La spec dit **ce que** chaque route accepte et rend ; ce guide dit **pourquoi**, et dans quel ordre
les appeler. Les deux sont nécessaires : le modèle de l'atelier a trois ou quatre partis pris qui rendent une
implémentation naïve fausse sans jamais lever d'erreur.

`openapi.json` est le livrable : il est commité, et la CI du back échoue tant qu'il ne décrit pas l'API telle qu'elle
est servie — c'est ce qui permet au front de générer ses types depuis ce fichier sans jamais appeler le back. **Le
renommer ou le déplacer casse la synchronisation d'en face, en silence.** Il ne décrit que les routes réelles : les
contrôleurs qui n'existent qu'en test en sont exclus.

Le détail métier et sa justification par le verbatim client sont dans
[contexte-metier.md](contexte-metier.md) ; les règles de code, dans [glm-back/AGENTS.md](../AGENTS.md).

Ce guide décrit le contrat back ; sa livraison et celle des consommateurs restent coordonnées.

---

## 1. Authentification et entreprise

Toutes les routes vivent sous `/api/**` et exigent un jeton Keycloak (realm `glmproject`, issuer
`http://localhost:9080/realms/glmproject`).

Le jeton **doit porter un claim `tenant`** nommant une entreprise déclarée. Chaque entreprise possède son propre schéma
PostgreSQL, et toute la surface `/api/**` répond **403** à un jeton sans entreprise connue — avant même d'atteindre le
contrôleur. Un 403 inexpliqué en développement, c'est presque toujours ça.

**Aucune route ne prend l'entreprise en paramètre.** Elle est toujours lue du jeton. Ne jamais l'ajouter à une URL.

### Les trois rôles

| Rôle           | Ce qu'il ouvre                                                                                        |
| -------------- | ----------------------------------------------------------------------------------------------------- |
| `USER`         | L'opérateur : pointer ses activités, lire.                                                            |
| `GESTIONNAIRE` | Tout ce que fait un `USER`, plus engager, clôturer et corriger (`regularise` / `annule` / `corrige`). |
| `ADMIN`        | Administration technique (`/api/admin/**`, `/management/**`) uniquement. **Aucun accès métier.**      |

Un `admin.*` qui appelle une route de gestion reçoit 403 : c'est voulu. Utilisateurs de développement (mot de passe
égal au login) : `gestionnaire.impeccmold`, `user.impeccmold`, `gestionnaire.katilys`, `user.katilys`.

**L'auteur d'une saisie n'est jamais dans le corps de la requête** : il est déduit du jeton. Ne pas prévoir de champ
« auteur » dans les formulaires. L'**opérateur**, lui, est bien dans le corps : sur un poste d'atelier partagé, celui
qui saisit n'est pas forcément celui dont on compte le temps.

---

## 2. Les quatre idées à comprendre avant de coder

### Le journal est la seule vérité

Le détail d'atelier reconstruit son agrégat depuis les faits du journal. Atelier réconcilie aussi ses projections
d'activités et de conflits à chaque écriture ; la feuille, la synthèse, le coût et le référentiel pupitre lisent
cette interprétation et évaluent l'expiration à leur instant explicite. Une correction recalcule donc les bornes,
les conflits et leurs conséquences dans les rapports.

Conséquence directe pour le front : après toute écriture, la réponse contient déjà l'agrégat entièrement recalculé.
**Ne jamais reconstruire l'état côté client** en appliquant l'événement localement — re-rendre depuis la réponse.

### L'horodatage est bitemporel

Chaque événement porte deux dates :

- `dateDeSurvenue` — l'heure **métier**, celle où le fait a eu lieu ;
- `dateDEnregistrement` — l'heure de la **saisie**.

Les instants du journal, de la clôture et des activités sont conservés exactement à la nanoseconde, y compris
lorsqu'un fait précède le suivant d'une seule nanoseconde. La réponse sérialise l'instant en UTC (`Z`) ; le décalage
d'origine (`+02:00`, par exemple) ne change pas l'instant. Les anciennes dates déjà arrondies restent telles quelles.

Un affichage honnête montre l'heure métier, et signale la saisie différée par l'écart entre les deux (« pointé le
11/05 à 9 h 15 pour le 10/05 à 17 h »).

Cet écart ne fait pas une régularisation : un pointage rejoué par un pupitre resté hors ligne arrive lui aussi après
coup. Le booléen `estUneRegularisation` dit l'**acte** qui a porté le fait au journal : vrai pour une régularisation
(`POST …/regularisations`) et pour le remplaçant d'une correction (`PUT …/evenements/{evtId}`), même saisis à l'heure
du fait ; faux pour tout ce qui passe par `POST …/pointages`, quels que soient le rôle de l'utilisateur et la
`dateDeSurvenue` fournie. Jamais l'identité de l'auteur.

Le booléen `estSaisiParUnTiers` **n'existe plus** : l'auteur est un identifiant de connexion et l'opérateur une fiche du
référentiel, et rien ne relie encore les deux. Il reviendra le jour où l'authentification sera tranchée.

### L'opérateur et le poste sont des identifiants

En **entrée**, `operateur` et `poste` sont les **UUID** des fiches des référentiels
(`GET /api/operateurs`, `GET /api/postes-de-travail`) — plus jamais du texte saisi. Un identifiant inconnu répond 404.

En **sortie**, ils sont des objets résolus à la lecture :

```json
{ "operateur": { "id": "…", "nom": "Dupont", "prenom": "Jean" }, "poste": { "id": "…", "libelle": "Fraiseuse 1" } }
```

Ces libellés sont **relus à chaque appel**, jamais figés : une fiche corrigée s'affiche corrigée sur tout
l'historique. Ne pas les mettre en cache côté front au-delà de la session d'écran.

La `nature` fait exception : elle est **copiée au moment de la saisie**, et elle vient du **poste**, pas de la personne.
Un poste requalifié plus tard ne requalifie pas les heures déjà passées.

`coutHoraire` et `tauxHoraire` suivent exactement la même règle : copiés sur l'événement au moment du pointage
(coût du poste, taux de l'opérateur), jamais recalculés à la lecture. Ils sont absents quand la source du référentiel
n'est pas valorisée, ou quand aucun poste n'est fourni pour `coutHoraire`. Seul le `GESTIONNAIRE` les reçoit,
y compris dans les réponses de pointage et de rejeu ; les lectures `USER` conservent le journal et ses identités,
avec les champs de tarifs absents. La même confidentialité vaut pour les tarifs courants des référentiels
opérateur et poste. Les tarifs saisis restent exactement représentables en centimes, strictement positifs
et inférieurs à 100 000 000 : création et révision refusent toute perte de précision ou dépassement par un 400,
sans arrondi implicite.

### L'habilitation est une règle dure

Pointer sur un poste où l'opérateur n'est pas déclaré répond **409**. C'est vrai du pointage comme de la régularisation
et de la correction. Un écran de pupitre doit donc **ne proposer que les postes de l'opérateur choisi**, lisibles dans
`GET /api/operateurs/{id}`, plutôt que laisser le serveur refuser.

La règle ne joue que si un poste est fourni : sans parc machine, il n'y a rien à habiliter.

### Un événement annulé reste au journal

`annule` ne supprime rien : l'événement demeure, porteur de son objet `annulation` (auteur, date, motif), et le repli
l'écarte du calcul. Le `journal` rendu par l'API **contient donc les événements annulés**.

Pour un écran d'atelier, filtrer sur `annulation == null`. Pour un écran d'audit, tout montrer — c'est là tout
l'intérêt de les conserver.

### La pause se traduit en faits d'activité

La pause n'existe pas pour le serveur ([ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)).
Le pupitre envoie une fin par activité actionnable, avec sa cible ; la reprise ouvre une nouvelle activité en
`DEBUT`, ou en `NON_CONFORMITE` pour celle qui l'était, sur le même poste. La nouvelle ouverture ne vise pas
l'activité d'avant la pause. La mémoire de reprise est locale au pupitre.

### Une activité oubliée se termine automatiquement à son échéance

Une activité que rien n'a terminée se termine automatiquement à son **échéance** : son début plus 13 heures écoulées,
sans fuseau — le passage à l'heure d'été ne l'allonge ni ne la raccourcit. Rien n'est écrit au journal : la fin
automatique se juge à l'instant d'évaluation de la lecture. Un travail commencé à 8 h et jamais arrêté est en cours à 20 h 59 ;
à 21 h, et à toute lecture ultérieure, il est terminé à 21 h, avec une anomalie.

Ce que les réponses en montrent :

- `activitesEnCours[]`, du détail comme de la grille, ne contient que les activités **en cours à l'instant de la
  lecture** : une activité échue en sort d'elle-même, et l'élément passe `INTERROMPU` si plus rien n'y est en cours.
  Chaque activité porte son `ouverture` — l'identité que visera une fin ou une transition — et son `echeance`.
- Deux lectures espacées peuvent différer au voisinage d'une échéance : c'est l'instant de lecture qui tranche.

La même échéance vaut pour les gestes, jugés sur leur heure métier, quel que soit le moment où ils arrivent :

- un geste pointé **au plus tard à l'échéance** de l'activité qu'il vise la termine à son heure, même reçu le
  lendemain : la fin pointée à 17 h et publiée après une coupure réseau remplace la fin automatique et retire
  l'anomalie. Un geste pile à l'échéance l'emporte ;
- une **fin pointée après l'échéance** est enregistrée (`201`) : l'activité garde ses 13 h et son anomalie,
  sans conflit ni qualification supplémentaire dans le journal. Ce succès est acquitté comme tout pointage conservé ;
- une **transition pointée après l'échéance** de sa cible ouvre la nouvelle activité à son heure ; la cible garde sa
  borne automatique, et rien n'est compté entre les deux. Une relance après l'échéance laisse le même trou ;
- une **clôture** postérieure à l'échéance ne prolonge rien ;
- seul le gestionnaire établit une fin réelle au-delà de l'échéance, en **régularisant** la fin ou la transition
  (`POST …/regularisations`). Corriger un début déplace l'échéance : l'activité peut redevenir en cours.

### Un pointage dit son intention et vise son activité

Le type d'un pointage ne dit pas ce qu'il fait d'une activité : son **intention** le dit, et elle est requise, sans
valeur par défaut.

| Geste                          | `type`           | `intention`  | `cible`                                  |
| ------------------------------ | ---------------- | ------------ | ---------------------------------------- |
| Ouvrir ou reprendre en travail | `DEBUT`          | `OUVERTURE`  | absente                                  |
| Ouvrir ou reprendre en NC      | `NON_CONFORMITE` | `OUVERTURE`  | absente                                  |
| Passer de NC à travail         | `DEBUT`          | `TRANSITION` | l'activité NC remplacée, requise         |
| Passer de travail à NC         | `NON_CONFORMITE` | `TRANSITION` | l'activité de travail remplacée, requise |
| Terminer                       | `FIN`            | `FIN`        | l'activité terminée, requise             |

Toute autre combinaison répond **400** (Bean Validation, détail dans `errors`). La même forme vaut pour la
régularisation et la correction.

- **Une activité se désigne par l'identifiant de son pointage ouvrant d'origine.** Le journal le rend dans
  `activite`, sur l'ouverture et la transition qui ouvrent l'activité ; la transition et la fin portent celle qu'elles
  visent dans `cible`. Le remplaçant d'une correction d'un ouvrant garde l'`activite` du fait corrigé : les gestes qui
  la visaient y restent rattachés, et un pupitre continue de viser l'identifiant du geste qu'il a lui-même envoyé.
- **Un geste ne touche que sa cible.** Une fin termine l'activité qu'elle vise, jamais une autre ; une transition la
  remplace par une activité distincte, de l'autre catégorie. Une ouverture termine à son heure l'activité en cours
  sur le même poste : c'est la relance.
- **La cible est une activité de ce suivi, du même opérateur et du même poste.** Introuvable dans ce suivi, elle répond
  **404** `activite-visee-introuvable` ; ouverte par un autre opérateur ou sur un autre poste, **409**
  `activite-visee-incoherente`. Ces deux refus sont définitifs : rejouer le même geste ne changera rien.
- **Un geste qui contredit le journal n'est jamais refusé** : sa cible est déjà terminée ou remplacée à son heure, son
  ouvrant est annulé, ou la transition vise une activité de sa propre catégorie. Il est enregistré (`201`), et la
  séquence est **en conflit** jusqu'à ce que le gestionnaire la résolve (voir ci-dessous). Une transition dont la cible
  n'est plus en cours ne devient jamais une ouverture. Une cible échue, elle, ne contredit rien (voir l'échéance
  ci-dessus).

### Des pointages contradictoires restent en conflit, jusqu'à la décision du gestionnaire

Le gestionnaire et l'opérateur peuvent consulter `GET /api/atelier/anomalies?nature=CONFLIT` (voir [Anomalies de pointage](#anomalies-de-pointage)). Cette page lit les projections
courantes sans charger les journaux : une ligne désigne une séquence par `adresse.suivi` et `adresse.pointage`,
avec la révision du suivi, les références brutes, les fiches disponibles, le premier instant métier exact et
le nombre de pointages. `operateur` et `element` cherchent du texte partiel sans casse, y compris dans les
identifiants, et se combinent avant `page` et `size`. Les caractères `%`, `_` et `\` restent littéraux.
Le tri suit le premier pointage, puis les identifiants du suivi et de l'ancrage. `total` et les `lignes` proviennent
d'une même acquisition SQL. `complete: true` caractérise une lecture réussie, même vide ; un échec d'acquisition
remonte en erreur HTTP. Les explications détaillées appartiennent au dossier de la séquence.
Le dossier expose les diagnostics produits pendant l'interprétation : `CIBLE_REMPLACEE`,
`CIBLE_DEJA_TERMINEE`, `GESTE_AVANT_OUVERTURE`, `OUVRANT_ANNULE`, `TRANSITION_MEME_CATEGORIE`,
`CIBLE_ECHUE_AVEC_AUTRE_ACTIVITE` ou `CONTRADICTION_REGULARISATION`. Chaque diagnostic identifie le
pointage contradictoire, l'activité visée, son ouvrant connu et le fait qui l'a terminée lorsqu'il existe.
L'identité d'un ouvrant annulé ou postérieur au geste reste présente ; aucune activité n'est créée pour
compléter l'explication. Une contradiction de régularisation concerne un geste régularisé ou une cible
prolongée par une régularisation. Le même passage dans l'interpréteur produit séquences et diagnostics.

Le serveur ne choisit jamais entre deux pointages qui se contredisent, quel que soit leur ordre d'arrivée. Travail A à
8 h, transition de A vers une non conformité à 12 h, fin de A à 17 h : que la transition arrive avant la fin ou le
lendemain, après elle, les trois faits sont conservés, la fin ne termine pas la non conformité et la transition n'est
pas ignorée. La séquence est **en conflit**. Un fait déjà accepté devient donc contradictoire à l'arrivée d'un fait
antérieur.

Sont en conflit : une fin ou une transition qui vise une activité déjà remplacée avant son heure (relance ou
transition), déjà terminée par une fin — le double appui sur « arrêter » compris —, pas encore ouverte à son heure, ou
dont l'ouverture est annulée ; une transition vers sa propre catégorie ; une transition qui vise une activité échue
pendant qu'une autre est en cours sur le même poste. Une régularisation, une correction ou une annulation qui crée ou
laisse une contradiction est admise de la même façon. Ne sont pas en conflit : un geste qui vise une activité
seulement échue, ou pointé pile à son échéance.

Ce que les réponses en montrent :

- Une activité à résoudre n'est **ni en cours ni terminée** : absente d'`activitesEnCours`, sans fin, sans durée, et
  son échéance ne la termine pas. L'`etat` du suivi se juge sur les seules activités interprétables : une nouvelle
  ouverture après le conflit est en cours, et l'élément avec elle.
- Hors de la séquence, les activités du même poste gardent leur lecture : ce qui précède la contradiction, et
  l'ouverture pointée après elle.

Les séquences et leurs deux listes sont projetées par atelier à chaque écriture, sans dépendre de l'instant de
lecture. Une séquence sans activité à résoudre reste conservée dans cette projection ; la résolution la retire.
Le journal demeure la source de vérité. La plage possible d'une activité à résoudre est bornée par sa fin au plus
tard : échéance ou régularisation recevable plus tardive, limitée par la clôture qui ne la prolonge jamais.
Cette borne vient des faits et la projection est réécrite à chaque correction, annulation, résolution ou clôture.

**Pour le pupitre, un pointage conservé en conflit est un succès** : il est acquitté `201` — `200` au rejeu, sans
second fait —, et il ne doit pas être republié. Aucune réponse ne rend plus la séquence en conflit.

**Pour le gestionnaire, le conflit se résout par les actes existants**, correction et annulation, et disparaît au
recalcul dès que les faits redeviennent cohérents ; l'historique garde pointages et corrections (voir l'écran
back-office).

---

## 3. Les écrans et leurs appels

### Écran d'atelier (rôle `USER`)

Le tableau des éléments à faire :

```
GET /api/atelier/suivis?etats=EN_ATTENTE&etats=EN_COURS&etats=INTERROMPU
```

Les filtres sont **tous facultatifs** — cet écran ne défile pas et n'a aucune notion de date. `etats` absent ne filtre
rien. `debut`/`fin` ne servent qu'au back-office, et **une borne seule est ignorée** : il faut les deux.

Le filtre `etats` juge l'état à l'instant de la lecture, le même que celui de chaque ligne rendue : un élément dont la
seule activité a atteint son échéance sort de `etats=EN_COURS` et entre dans `etats=INTERROMPU`, sans aucune écriture.

La liste rend une page de **`RestSuiviDAtelierEnGrille`**, sans propriété `journal` (ni tableau vide, ni
valeur `null`). Tous les autres champs sont conservés : `id`, `element`, `nom`, `categorie`, `engagePar`, `engageLe`,
`etat`, `cloturePar`, `clotureLe` et `activitesEnCours`. L'état et les activités restent calculés par le serveur depuis
le journal ; ce changement allège la réponse HTTP et le cache du pupitre, pas la relecture en base.

Le journal complet, **événements annulés compris**, se lit via `GET /api/atelier/suivis/{id}`, qui conserve
`RestSuiviDAtelier`, comme les réponses des actes métier. Un consommateur qui lisait le journal dans la liste doit
utiliser le détail. Côté `glm-front`, générer le contrat depuis la révision backend épinglée avec
`npm run api:generate`, conformément à son guide API. La limite de pagination de la grille reste un suivi côté front.

Chaque geste d’activité du pupitre porte un `id` UUID créé une fois par le front, et son
intention. Il peut aussi porter `dateDeSurvenue`, l'heure réelle conservée quand le pupitre a été hors ligne.

```
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid A>", "type": "DEBUT", "intention": "OUVERTURE", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid B>", "type": "NON_CONFORMITE", "intention": "TRANSITION", "cible": "<uuid A>", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid C>", "type": "FIN", "intention": "FIN", "cible": "<uuid B>", "operateur": "<uuid>", "poste": "<uuid poste>" }
```

À retenir :

- **Arrêter un élément après sa clôture est absorbé** (`200`) : la clôture l'a déjà arrêté. Une fin survenue avant la
  clôture, mais reçue après elle, est enregistrée à son heure (`201`). Arrêter une activité échue est enregistré sans
  effet (`201`). Arrêter deux fois la même activité — le double appui — n'est plus absorbé : la seconde fin est
  enregistrée (`201`), et la séquence est en conflit. Démarrer ou pointer une non conformité sur un élément clôturé
  reste refusé (`409 suivi-d-atelier-cloture`) : c'est le seul refus à afficher à l'opérateur, « OF clôturé, vous ne
  pouvez plus pointer dessus ».
- **Deux saisies simultanées ne sont plus un refus** : le serveur rejoue lui-même l'écriture devancée.
- **Une reprise du travail après non conformité se pointe `DEBUT`, en transition** qui vise la non conformité. Il
  n'existe pas de type « reprise ». Ce qui change, c'est la `categorie` de l'activité ouverte, `TRAVAIL`.
- **Une ouverture sur une activité déjà en cours la relance** au lieu d'être refusée : la période précédente s'arrête
  à l'heure du geste si elle précède son échéance ; après, la fin automatique et le trou jusqu’à la nouvelle
  ouverture sont conservés. Une transition ciblée de même catégorie met la séquence en conflit.
- **À heure métier égale**, le journal range la fin, puis la transition, puis l'ouverture, et départage enfin par
  l'identifiant : jamais par l'heure de réception.
- `poste` est **toujours facultatif**, comme la `nature`. Une entreprise sans parc machine les laisse vides et doit
  retrouver un comportement nominal, pas un cas dégradé. Ne jamais rendre le champ obligatoire côté formulaire.
- Un poste fourni doit être **habilité pour cet opérateur**, sans quoi 409. Filtrer la liste des postes sur la fiche de
  l'opérateur choisi évite d'avoir à traiter ce refus.
- Un envoi accepté répond **201**. Rejouer exactement le même corps répond **200**, sans créer de second événement ;
  conserver donc l'UUID dans la file offline jusqu'à l'acquittement, avec son intention et sa cible. Réutiliser cet UUID
  avec un autre contenu — une autre intention ou une autre cible comprises — répond 409
  (`identifiant-evenement-reutilise`). Une date future répond 400 et ne réserve pas l'UUID.

Les états d'un élément :

| `etat`       | Sens                                                                                        |
| ------------ | ------------------------------------------------------------------------------------------- |
| `EN_ATTENTE` | Engagé, aucun pointage actif. Personne n'y a encore touché.                                 |
| `EN_COURS`   | Au moins une activité en cours à l'instant de la lecture — **y compris en non conformité**. |
| `INTERROMPU` | Il y a eu du travail, mais plus aucune activité n'est en cours : terminée, ou échue.        |
| `CLOTURE`    | Clôturé. N'accepte plus de pointage (409), mais reste corrigeable.                          |

Attention : une non conformité **ne fait pas** passer à `INTERROMPU`. L'activité reste ouverte — ce temps-là se compte
aussi —, seule sa `categorie` change. Pour signaler visuellement une non conformité, lire
`activitesEnCours[].categorie`, pas `etat`.

### La supervision de l'atelier, en une lecture complète (rôles `USER` et `GESTIONNAIRE`)

```http
GET /api/atelier/supervision
```

La réponse rend `evaluation`, `operateurs`, `activites`, sans pagination. `evaluation` est
l'instant relevé une seule fois sur l'horloge du serveur ; il gouverne toutes les expirations de cette lecture.
Chaque acquisition relit les référentiels et les projections, sans cache ni données de démonstration.

`operateurs` contient le référentiel complet, même les personnes sans activité ou sans métier : `id`, `nom`,
`prenom` et `metiers`. Les métiers sont les natures distinctes des postes actuellement habilités. Aucun taux
horaire ni coût n'est rendu. Les activités gardent leur `operateurId`, même lorsque sa fiche ne
figure pas dans cette collection : le consommateur peut alors constater que la lecture est inexploitable.

`activites` contient les activités interprétables encore sans fin réelle. Chacune porte son `id`, identité stable
de l'ouvrant d'origine, `operateurId`, `categorie`, `debut`, `echeance`, `element` et le `poste` facultatif. L'élément
porte son identité, sa `categorie` — le code d'une catégorie de produit de l'entreprise, `MOULE` ou `OF` par exemple —, son nom copié à l'engagement et sa référence
courante facultative. Le poste porte son identité, son libellé courant et la nature facultative copiée sur
l'activité ; requalifier le poste ne réécrit pas cette nature historique.

Avant l'échéance, l'état est `EN_COURS`, sans `finRetenue`. Dès l'échéance, borne incluse, il est
`TERMINEE_AUTOMATIQUEMENT` et `finRetenue` porte cette échéance. Cette anomalie reste rendue après une relance ou
la clôture du suivi : une ouverture de 8 h oubliée garde sa fin automatique de 21 h, même si un autre travail
commence le lendemain. Une fin recevable ou régularisée retire l'anomalie ; une correction de l'ouverture peut
repousser l'échéance et rendre la même activité en cours. Les activités terminées réellement sortent de cette
collection.

La lecture consomme `activite_d_atelier`, sans rejouer les journaux. Elle utilise une
transaction unique en `READ COMMITTED` : l'évaluation est commune, mais les requêtes peuvent observer une écriture
concurrente entre les collections. Elle ne promet donc pas un instantané de la base. La lecture répétable exige
un autre patron d'acquisition de connexion, comme expliqué dans le
[contexte pupitre](../src/main/java/com/glm/glmback/pupitre/AGENTS.md).

La supervision ne rend que des activités rattachées à un élément de fabrication. Le travail personnel ou non
facturable sera déclaré par le superviseur sous la forme d'un élément d'une catégorie dédiée ; sa création et ce sous-type
appartiennent à un chantier distinct. Aucun travail sans élément n'est créé par cette lecture.

### Le référentiel du pupitre, en un seul appel (rôle `USER`)

```
GET /api/pupitre/referentiel
```

Un pupitre hors ligne ne reconstitue plus son cache en paginant `GET /api/operateurs` puis
`GET /api/atelier/suivis`. Cette route rend **tout d'un coup** : les opérateurs désignables avec leur identifiant et
leurs postes habilités, les éléments encore pointables avec leurs activités en cours, et les codes des catégories
de produit dans l'ordre choisi par le gestionnaire (`PUT /api/categories-de-produit/ordre`) : le pupitre range ses
tuiles par catégorie, dans cet ordre, et la liste est vide tant que l'entreprise n'en a déclaré aucune.

```json
{
  "genereLe": "2026-09-14T09:31:02.418Z",
  "operateurs": [
    {
      "id": "…",
      "nom": "Dupont",
      "prenom": "Jean",
      "identifiant": "049",
      "postes": [{ "id": "…", "libelle": "Fraiseuse 1" }]
    }
  ],
  "suivis": [
    {
      "id": "…",
      "nom": "OF-2026-000042",
      "reference": "M-1187",
      "categorie": "OF",
      "etat": "EN_COURS",
      "activites": [
        {
          "operateur": "…",
          "poste": "…",
          "categorie": "TRAVAIL",
          "depuis": "2026-09-14T08:02:00Z",
          "ouverture": "…",
          "echeance": "2026-09-14T21:02:00Z"
        }
      ]
    }
  ],
  "categories": ["MOULE", "OF"]
}
```

À savoir avant de brancher un cache dessus :

- **Elle n'est pas paginée, et c'est le point.** Tout est lu en un appel et une transaction unique : plus de boucle
  de pages à écrire, ni de gardes sur les totaux, les doublons ou les pages vides — ces trois gardes existaient
  contre le recousage de pages, qui n'existe plus. En revanche la réponse **n'est pas un instantané strict** : les
  opérateurs et les suivis sont lus par des requêtes successives sous l'isolation par défaut de PostgreSQL, et un
  changement validé entre les deux se voit dans la seconde. Concrètement, une activité en cours peut désigner un
  opérateur absent de la liste jointe — afficher l'identifiant brut plutôt que planter, l'appel suivant recollera.
  Une version antérieure de ce document annonçait une lecture répétable : elle n'a jamais fonctionné et a été
  retirée, la route répondait `500` à chaque appel.
- **`genereLe` date l'évaluation.** Elle dit quand le serveur a produit la réponse et sert à juger l'expiration — de quoi
  afficher « référentiel du 14/09 à 09:31 » et mesurer un retard. Elle **change à chaque appel**, y compris quand
  rien n'a bougé : ce n'est pas la date du dernier changement, et s'en servir pour décider d'un rafraîchissement
  n'aurait aucun sens. Il n'y a ni `ETag` ni `304`.
- **Les opérateurs sont désignables indépendamment de leurs activités.** Ils portent leur identité, leur identifiant
  éventuel et leurs habilitations.

- **Les activités sont interprétées par atelier**, puis leur expiration est évaluée à `genereLe`. Une activité
  à résoudre, terminée par un fait ou échue est absente d'`activites`. `ouverture` est l'identité stable à viser
  par une fin ou une transition, conservée après correction ; `echeance` permet l'expiration hors ligne, à cet
  instant inclus, sans fabriquer de fin. `etat` vaut `EN_COURS` s'il reste une activité interprétable en cours,
  sinon `INTERROMPU` s'il existe un pointage actif, sinon `EN_ATTENTE`.
- **Aucun montant.** Ni `tauxHoraire` d'opérateur, ni `coutHoraire` de poste : un écran d'atelier partagé n'a pas à
  les recevoir, et `GET /api/couts-de-revient/{elementId}` reste réservé au `GESTIONNAIRE`.
- **Aucun élément clôturé, aucun journal.** `etat` ne vaut donc jamais `CLOTURE` ici. Le journal complet, événements
  annulés compris, se lit toujours par `GET /api/atelier/suivis/{id}` — c'est aussi lui qu'on relit après un
  `saisie-concurrente`.
- **`nom` et `reference` ne suivent pas la même règle.** `nom` est celui copié à l'engagement, figé ; `reference`
  est celle du référentiel, relue à chaque appel. Un élément supprimé du référentiel garde sa tuile et perd sa seule
  référence.

L'écriture, elle, ne change pas : ce sont toujours les `POST` de l'écran d'atelier ci-dessus, avec l'UUID de geste
créé par le pupitre et la `dateDeSurvenue` conservée hors ligne.

### Écran back-office (rôle `GESTIONNAIRE`)

```
POST   /api/atelier/suivis                                    engager un élément
PUT    /api/atelier/suivis/{id}/cloture                        clôturer, ou déplacer la clôture
DELETE /api/atelier/suivis/{id}/cloture                        rouvrir
POST   /api/atelier/suivis/{id}/regularisations                régulariser la fin d'une activité échue
```

**La régularisation directe** établit la fin d'une activité que rien n'a terminée avant son échéance (une fin
automatique). Le corps ne porte que trois champs :

```json
{
  "id": "6d0c1a4e-…",
  "activite": "ab8f8dba-…",
  "dateDeSurvenue": "2026-05-10T17:00:00Z"
}
```

- `id` est généré par le client **une fois par saisie** et conservé d'un renvoi à l'autre ;
- `activite` est l'`ActiviteId` de l'activité (l'identifiant de son pointage ouvrant) ;
- `dateDeSurvenue` est l'heure à laquelle la fin a réellement eu lieu. Elle peut dépasser l'échéance de l'activité.

L'opérateur, le poste et le type du fait se déduisent de l'activité : la saisie ne les porte pas. L'événement écrit est
une fin (`FIN`) qui vise l'activité, `estUneRegularisation` vrai ; elle ne passe pas par la règle de réception des
pointages. Une régularisation qui n'est pas la fin d'une activité échue n'existe plus.

**Idempotence.** La présence de `id` dans le journal est vérifiée avant toute règle : un renvoi de la même saisie répond
**200** (au lieu de 201), avec le suivi tel qu'il est, et n'écrit rien — y compris quand l'activité est désormais
régularisée.

**Refus**, par ordre de vérification (le code est dans `type`, voir [codes-erreur.md](codes-erreur.md)) :

| Statut | Code                         | Cas                                                                                                 |
| ------ | ---------------------------- | --------------------------------------------------------------------------------------------------- |
| 404    | `activite-visee-introuvable` | Aucun pointage de ce suivi n'a ouvert cette activité.                                               |
| 409    | `activite-deja-regularisee`  | Une régularisation vise déjà cette activité.                                                        |
| 409    | `activite-non-echue`         | L'activité n'est pas une fin automatique : échéance non atteinte, ou terminée par un pointage.      |
| 400    | `date-de-survenue-future`    | L'heure dépasse l'instant présent.                                                                  |
| 409    | `fin-avant-debut`            | L'heure précède le début de l'activité.                                                             |
| 409    | `fin-apres-borne`            | L'heure dépasse le début suivant sur la clé (opérateur et poste) ou la clôture : voir `borneDeFin`. |

`saisie-concurrente` (409) reste le refus de concurrence : un pointage s'est glissé entre la lecture et l'écriture,
relire le dossier. Le dossier d'une fin automatique donne la borne `borneDeFin` : le plus tôt du début suivant sur la
clé et de la clôture, ou rien ; l'instant présent borne toujours la fin.

**La clôture ne fige rien pour le gestionnaire** : la régularisation reste possible ensuite, et la clôture elle-même
se déplace (`PUT`) ou s'annule (`DELETE`). Ne pas griser la régularisation sur un élément clôturé.

`PUT .../evenements/{evtId}` est une **correction** : une annulation et une régularisation en un seul appel. Le journal
en ressort avec deux événements de plus, pas un — l'ancien annulé, le nouveau à l'heure corrigée. Le remplaçant d'un
pointage ouvrant garde son `activite` : la fin qui visait l'activité la termine toujours. Corriger un début de 8 h à
12 h, lu à 22 h, rend l'activité en cours jusqu'à son échéance de 1 h, et la fin que le pupitre pointe ensuite en
visant le pointage d'origine la termine.

Le remplaçant porte aussi `remplace`, l'UUID de l'événement corrigé : ce lien distingue la correction d'une
annulation suivie d'une régularisation et vaut aussi pour une fin. Corriger un remplaçant crée le lien vers ce
remplaçant ; l'annuler conserve son lien. Un pointage ou une régularisation rend `remplace: null`. Les anciens
événements sans lien explicite rendent aussi `null` : aucune proximité de date ou d'auteur ne reconstitue une
correction certaine.

Déplacer par correction un ouvrant vers un autre opérateur ou poste répond **409** `activite-visee-incoherente` si
un geste actif vise encore cette activité depuis l'ancienne clé. Corriger ou annuler d'abord ce geste permet ensuite
de déplacer l'ouvrant.

**Une séquence en conflit se résout par ces mêmes actes**, et disparaît de `conflits` au recalcul dès que les faits
redeviennent cohérents ; les pointages et corrections restent au journal. Travail A à 8 h, transition vers une non
conformité à 12 h, fin de A à 17 h :

- annuler la transition erronée (`POST …/evenements/{transition}/annulation`) rend A de 8 h à 17 h ;
- corriger la fin de A en fin de la non conformité (`PUT …/evenements/{fin}`, `cible` = l'activité de la non
  conformité, `dateDeSurvenue` 17 h) rend A de 8 h à 12 h, puis la non conformité de 12 h à 17 h.

Une résolution en plusieurs actes passe par des états intermédiaires en conflit, tous admis : chaque réponse rend la
séquence telle que les faits la laissent. Une régularisation, une correction ou une annulation n'est jamais refusée
parce qu'elle crée ou laisse une contradiction ; les refus qui ne tiennent pas à une contradiction demeurent — cible
introuvable ou d'un autre poste, habilitation, événement antérieur à l'engagement ou postérieur à la clôture,
événement déjà annulé.

### Évaluer le relevé des heures

```
GET /api/feuilles-de-temps/{operateurId}?annee=2026&semaine=20&evaluation=2026-05-11T20:59:59Z
GET /api/syntheses-des-heures/{operateurId}?annee=2026&semaine=20&evaluation=2026-05-11T20:59:59Z
```

Les deux lectures acceptent un instant ISO-8601 facultatif et rendent l'instant effectivement utilisé dans
`evaluation`. Chaque lecture relève l'heure du serveur une seule fois : elle fournit l'instant par défaut et
vérifie la borne future. Cet instant d'évaluation gouverne l'expiration et les jours atteints par les activités
en cours et par les plages possibles à résoudre. Passer le même instant à la feuille et à la synthèse assure la même
décision d'expiration. Une écriture entre les appels peut changer les faits lus ; l'instant commun ne garantit
pas un instantané commun.

Un instant passé est accepté, ainsi qu'un instant jusqu'à l'heure du serveur plus deux minutes, borne incluse.
Au-delà, la réponse est 400 `evaluation-future` dans le contexte de la lecture, sans rapport. Un instant fourni
vide ou mal formé répond aussi 400. Les faits connus restent interprétés même postérieurs à cet instant : ce n'est
pas une lecture historique. La semaine, le fuseau et les rôles de lecture gardent leurs règles.

La feuille rend sept jours, vides compris, avec les portions d'activités qui recouvrent la semaine, même commencées
avant celle-ci. Chaque portion garde dans `activite` l'identité stable, l'état et les bornes entières : `TERMINEE`
ou `TERMINEE_AUTOMATIQUEMENT` porte une fin ; `EN_COURS` rend une indication sans fin sur chaque jour atteint,
sans durée comptabilisée. La synthèse compte seulement les portions terminées, réelles ou automatiques.
Les portions sont coupées aux minuits du fuseau de l'entreprise ; ces coupes préservent les bornes entières.

### Lire les jours possibles d'un conflit

La feuille rend une activité `A_RESOUDRE` sur chaque jour de sa plage possible, jusqu'à `evaluation` ou sa
`activite.finAuPlusTard`, la première borne atteinte. Cette fin possible est exclusive, issue des faits d'atelier
et conservée entière sur chaque portion. `fin` et `activite.fin` restent absents ; aucune durée n'est fabriquée.
L'identité de l'activité reste celle de l'ouverture originale, même après correction du pointage ouvrant.
Une régularisation peut porter cette plage au-delà de 13 h ou d'une semaine ; une clôture la limite sans la prolonger.

### Totaux et conflits dans la synthèse

La synthèse rend `conflits[]`, dont chaque entrée porte `element`, `poste` facultatif, `activites[]` (identités
stables) et `pointages[]` (identités des gestes dans l'ordre métier). Une séquence est rendue si une activité ou
un pointage de cette séquence figure dans la semaine. Elle reste visible sans activité ni poste ; aucune borne
basse arbitraire sur le début ne supprime un conflit commencé avant la semaine.

Les quatre totaux — `jours[].dureeOperationnelle`, `dureeOperationnelleTotale`, `elements[].duree` et
`elements[].dureeNonConformite` — sont des objets :

```json
{ "complete": true, "valeur": "PT2H" }
```

Un total dépendant d'une activité à résoudre porte seulement `{ "complete": false }` : `valeur` est absente,
jamais une somme partielle ni zéro. La NC reste comprise dans le total ; sa part séparée ne dépend que des NC.
Un conflit de travail laisse donc une NC certaine chiffrée. Les jours et éléments indépendants restent complets.
Une activité en cours ne contribue pas à la durée et n'incomplète aucun total. Un conflit sans activité à résoudre
laisse les totaux complets. Les corrections et annulations recalculent les valeurs et retirent le conflit résolu.

Le journal brut `jours[].pointages[]` porte aussi `id`, `intention` et `cible` facultative. Son ordre est l'heure
métier, puis `FIN < TRANSITION < OUVERTURE`, puis l'identité ; il ne suit jamais l'ordre de réception.
Une FIN ordinaire après l'échéance seule conserve les 13 h complètes et l'anomalie automatique dans la feuille,
sans conflit ni nouvelle qualification « sans effet » dans le journal.

## 4. Erreurs

Toutes les erreurs métier sont des `ProblemDetail` (RFC 7807) portant un `type`, un `title`, un `status` et une
propriété `message` lisible.

Le `type` est un **code stable** de la forme `urn:glm:erreur:<contexte>:<code>` — c'est **lui** qu'on teste pour
brancher, jamais le couple `status` + `title` : le titre est une phrase française qui se reformule, et le même titre
sort de plusieurs contextes. Le catalogue complet est dans [documentation/codes-erreur.md](codes-erreur.md).

Deux statuts du tableau ci-dessous n'en portent pas : le **400** de Bean Validation, qui se lit par son `errors`
(`Map<champ, message>`), et le **403**, qui vient de la chaîne de filtres sans corps du tout.

| Statut | Cas                                                                                                                                                                                    |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400    | Corps invalide (Bean Validation), intention et cible comprises — détail par champ dans `errors` — ou date de survenue future.                                                          |
| 403    | Jeton sans entreprise connue, ou rôle insuffisant.                                                                                                                                     |
| 404    | Suivi, événement, élément de fabrication ou activité visée introuvable.                                                                                                                |
| 409    | Élément déjà engagé, élément clôturé, événement déjà annulé, activité visée d'un autre opérateur ou poste, événement antérieur à l'engagement, UUID réutilisé, **saisie concurrente**. |

Le journal d'un élément ne refuse aucun geste qui le contredit : une fin datée avant le début de sa cible,
ou un geste qui vise une activité terminée, remplacée ou annulée, reste enregistré ; sa séquence est en conflit.

Sur les routes de pointage, la **saisie concurrente** est rejouée par le serveur et ne remonte plus qu'après trois
échecs. Sur les actes du gestionnaire, elle reste le seul 409 qui ne dit rien de la saisie elle-même : elle était valide, mais quelqu'un a
pointé sur le même élément entre la lecture et l'écriture. C'est le seul cas où **rejouer** l'appel
tel quel est la bonne réaction — relire l'agrégat, et reproposer la saisie.

Le pupitre date ses gestes avec l'horloge du poste, qui peut avancer un peu sur celle du serveur : une
`dateDeSurvenue` en avance de **deux minutes au plus** est ramenée à l'instant courant du serveur. Au-delà, elle
répond 400 avec le code stable `date-de-survenue-future`. Le pupitre peut alors corriger son contenu et réutiliser le même UUID.

---

## 5. Limites de l'implémentation actuelle

- **`nature` est vide dès qu'aucun poste n'est pointé**, puisqu'elle vient du poste. Un pointage sans poste n'a pas de
  nature, et c'est le comportement nominal d'une entreprise sans parc machine.
- **`coutHoraire` et `tauxHoraire` sont réservés au `GESTIONNAIRE`**, sur les référentiels et les événements du journal, jamais combinés : l'atelier capture ces valeurs, il ne
  les combine jamais. La valorisation vit dans un autre contexte, `GET /api/couts-de-revient/{elementId}`, qui rend
  une ligne par nature d'opération avec le temps passé, le temps de non conformité daté, et le coût séparé en machine
  et main d'œuvre. Deux différences à connaître avant de brancher un écran dessus : il s'appelle avec l'identifiant
  de l'**élément de fabrication**, pas celui du suivi, et il est réservé au rôle `GESTIONNAIRE`.
- **Régulariser sur un poste dont l'opérateur a été dé-habilité depuis est refusé** (409), l'habilitation étant
  vérifiée sur les trois actes de correction. Retirer une habilitation ferme donc aussi la porte au rattrapage des
  saisies passées sur ce poste.
- **Ni un opérateur ni un poste ayant un fait historique d’activité, même annulé, ne se supprime** : `DELETE /api/operateurs/{id}` et
  `DELETE /api/postes-de-travail/{id}` répondent 409. Un écran d'administration doit le prévoir plutôt que le
  découvrir.

## Coût de revient d'un élément

`GET /api/couts-de-revient/{elementId}` exige `GESTIONNAIRE`. Le rapport rend `evaluation`, relevée une fois
sur l'horloge du serveur, et `activitesEnCours`, nombre d'activités exclues du temps, de tous les coûts et du
partage humain. Les activités terminées automatiquement comptent dès leur échéance et leur période figure
sur la ligne dans `finsAutomatiques`, signalant l'anomalie active. L'échéance est inclusive ; une lecture ultérieure
conserve cette borne.

La machine coûte l'intervalle terminé entier ; la main d'œuvre se partage par postes distincts occupés par
les seules activités terminées du même opérateur, tous éléments confondus. Une fin nouvellement reçue peut
modifier ce partage sur un autre élément. Les tarifs viennent du fait ouvrant actif figé par atelier.

Les champs de durée et de montant des lignes et du rapport sont des objets : `{ "complete": true,
"valeur": ... }`, y compris zéro. Un taux absent produit zéro, indépendamment du diviseur.

Une activité que le moteur juge à résoudre n'est lue par aucun calcul : ni le temps, ni les coûts, ni le
partage humain ne la voient, et le rapport ne rend ni conflit ni pointage contradictoire. Chaque pointage de
la ligne porte ses `anomalies` (`FIN_AUTOMATIQUE` ou aucune), sa `fin` et ses `parts`, dont le `diviseur` est
toujours connu.

Cette route ne prend pas de paramètre d'évaluation et ne garantit pas un instantané face aux écritures concurrentes.

## Anomalies de pointage

Ces routes sont décrites dans le [contrat OpenAPI généré](openapi.json) et éprouvées par les scénarios REST.

| Capacité             | Route                                                                           | Droit                    |
| -------------------- | ------------------------------------------------------------------------------- | ------------------------ |
| Liste paginée        | `GET /api/atelier/anomalies?nature=CONFLIT&operateur=…&element=…&page=0&size=5` | `USER` ou `GESTIONNAIRE` |
| Liste des fins auto. | `GET /api/atelier/anomalies?nature=FIN_AUTOMATIQUE&…`                           | `USER` ou `GESTIONNAIRE` |
| Dossier adressé      | `GET /api/atelier/suivis/{suivi}/anomalies/{pointage}`                          | `USER` ou `GESTIONNAIRE` |

Une **anomalie de pointage** est ce que le gestionnaire doit trancher. Elle porte une `nature` : `CONFLIT`
(une séquence en conflit) ou `FIN_AUTOMATIQUE` (une activité terminée à son échéance faute de fin réelle). Les anciennes routes
`/api/atelier/conflits` et `/api/atelier/suivis/{suivi}/conflits/{pointage}` sont supprimées : elles répondent 404,
sans redirection. Le tableau `conflits[]` des coûts garde son sens et son nom.

`nature` est un paramètre de requête obligatoire de la liste, valant `CONFLIT` ou `FIN_AUTOMATIQUE` (schéma
`NatureDAnomalie` dans le contrat). Absent ou inconnu — la casse compte —, il est refusé en 400 par un `ProblemDetail` au code stable
des erreurs métier, sans ligne de page :

```json
{
  "type": "urn:glm:erreur:atelier:nature-d-anomalie-invalide",
  "title": "nature d'anomalie invalide",
  "status": 400,
  "message": "La nature d'anomalie 'INCONNUE' est inconnue. Valeurs possibles : CONFLIT, FIN_AUTOMATIQUE."
}
```

Le `message` d'une nature absente est « La nature d'anomalie est obligatoire. Valeurs possibles : CONFLIT, FIN_AUTOMATIQUE. ». Le client
teste `type`, jamais `message` (voir [les codes d'erreur](codes-erreur.md)). Ce refus ne passe pas par le 400 de Bean
Validation, qui ne porte pas de `type` et ne traite pas un paramètre de requête manquant.

### Liste des anomalies

`GET /api/atelier/anomalies?nature=…` rend une page `RestPageDesAnomalies` : `lignes`, `total`, `complete`, `page`
(à partir de 0) et `size`. `total` et `lignes` viennent d'une même acquisition SQL sur les projections ; `complete`
vaut `true` pour toute lecture réussie, même sans ligne. Les droits, la pagination et les filtres sont les mêmes pour
les deux natures : `operateur` et `element` cherchent du texte partiel sans casse, y compris dans les identifiants, et
se combinent avant `page` et `size` ; `%`, `_` et `\` restent littéraux.

Chaque ligne est discriminée par sa `nature`, celle demandée : le contrat est une union `oneOf` de
`RestConflitEnListe` (`nature: CONFLIT`) et de `RestFinAutomatiqueEnListe` (`nature: FIN_AUTOMATIQUE`), que
`openapi-typescript` rend en union TypeScript discriminée. Côté serveur, les deux lignes restent deux types de domaine
(`ConflitEnListe`, `FinAutomatiqueEnListe`) ; l'union n'existe que dans la réponse.

| Nature            | Une ligne est…                       | Tri                                 |
| ----------------- | ------------------------------------ | ----------------------------------- |
| `CONFLIT`         | une séquence en conflit              | premier pointage, puis suivi, ancre |
| `FIN_AUTOMATIQUE` | une activité terminée à son échéance | `debut`, puis suivi, puis ouvrant   |

Une **fin automatique** est lue dans `activite_d_atelier`, sans rejouer aucun journal : sans fin réelle (`fin` nulle),
hors des activités à résoudre, avec une `echeance` inférieure ou égale à l'instant de lecture, borne comprise et à la
nanoseconde (les instants sont des décimaux exacts, `:evaluation` est converti de la même façon). L'instant de lecture
est celui de l'horloge du service, lu une seule fois pour toute la page ; la route ne prend pas de paramètre
d'évaluation. Une fin réelle pointée après l'échéance ne retire pas l'activité de la liste : elle garde sa fin
automatique tant que le gestionnaire ne l'a pas corrigée. Une activité en cours, à résoudre ou terminée — fin réelle,
fin régularisée, clôture avant l'échéance — n'y figure pas. Une fin automatique n'est jamais stockée.

Une ligne de fin automatique porte :

- `adresse` : le `suivi` et le `pointage` de l'**ouvrant actif** (`activite_d_atelier.ouverture_id`), qui adresse le
  dossier ;
- `activite` : l'identité d'**origine** de l'activité (`ActiviteId`), que visent les actes. Elle diffère de
  `adresse.pointage` dès qu'une correction a remplacé l'ouvrant : un client ne confond jamais les deux ;
- `revision` du suivi, `elementId` et `designation`, `operateurId` et `operateur` (fiche, absente si inconnue),
  `posteId` et `poste` facultatifs ;
- `debut` et `echeance`, instants exacts à la nanoseconde. Aucune durée n'est calculée ni exposée.

```json
{
  "complete": true,
  "lignes": [
    {
      "nature": "FIN_AUTOMATIQUE",
      "activite": "aaaaaaaa-0000-4000-8000-000000000001",
      "adresse": {
        "suivi": "97379b3a-1f98-4f92-97f2-a4b4d66449ac",
        "pointage": "aaaaaaaa-0000-4000-8000-000000000001"
      },
      "debut": "2026-01-12T08:26:00Z",
      "designation": "OF M24-0655",
      "echeance": "2026-01-12T21:26:00Z",
      "elementId": "0abc06ce-a050-4265-91fa-75f73785fa41",
      "operateur": { "id": "33333333-3333-4333-8333-333333333333", "nom": "Dupont", "prenom": "Jean" },
      "operateurId": "33333333-3333-4333-8333-333333333333",
      "poste": { "id": "55555555-5555-4555-8555-555555555555", "libelle": "Fraiseuse 1" },
      "posteId": "55555555-5555-4555-8555-555555555555",
      "revision": 0
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

Une ligne de conflit garde ses champs (`datePremierPointage`, `nombrePointages`, sans `activite`, `debut` ni
`echeance`) et porte en plus `"nature": "CONFLIT"`.

### Le dossier d'une adresse

L'adresse d'un dossier est le couple suivi/pointage ; une identité technique de projection n'est pas
une adresse. Le résultat porte `EN_CONFLIT`, `INTROUVABLE`, `ANCRE_ANNULEE`, `FIN_AUTOMATIQUE` ou `SANS_ANOMALIE` (ancre
active sans anomalie ; ce résultat s'appelait `HORS_CONFLIT`). L'ordre de décision est celui de cette phrase :
`FIN_AUTOMATIQUE` vaut pour une ancre active qui ouvre une activité que l'évaluation lit terminée automatiquement,
sans séquence en conflit.
Un suivi absent du tenant courant répond 404 sans journal. Un suivi accessible conserve son journal
dans les résultats d'adresse sans conflit ; aucun de ces résultats ne redirige implicitement.

Le dossier et son avant/après portent la `revision` numérique du suivi évalué, son `evaluation`, le
journal complet, les activités concernées et les conflits restants. `sequence` décrit la séquence
active contenant l’ancre ; `perimetre` conserve les faits concernés après un acte, même si l’ancre
est annulée. Le booléen `enConflit` est calculé par le domaine sur ce périmètre : il peut rester vrai
sans intervalle d’activité, ou être faux avec d’autres conflits indépendants dans `continuations`.
Les continuations donnent les adresses actives explicites ; elles ne changent jamais l’adresse demandée. Chaque
élément de `continuations[]` est une ligne de conflit au schéma `RestConflitEnListe`, celui de la liste : il porte donc
désormais `"nature": "CONFLIT"`, champ requis ajouté au dossier avec la liste des anomalies.

### Le dossier d'une fin automatique

Le dossier porte aussi `borneDeFin` (instant, absent quand rien ne borne la fin) : le plus tôt du début suivant sur la
clé de l'activité et de la clôture, que la régularisation de sa fin ne peut pas dépasser.

Une activité que rien n'a terminée s'arrête à son échéance, treize heures écoulées après son début, borne incluse.
Ce n'est pas un conflit : le dossier de son ouvrant actif (`activite_d_atelier.ouverture_id`) répond
`FIN_AUTOMATIQUE`. L'échéance se juge à l'instant d'évaluation, à la nanoseconde, et n'est jamais stockée : liste et
dossier la jugent chacun au leur. Lue à 20:59:59.999999999 pour un début à 08:00, l'adresse est `SANS_ANOMALIE` ; lue à 21:00:00, elle
est `FIN_AUTOMATIQUE`.

Deux identifiants restent distincts. L'**adresse** du dossier est l'`EvenementDAtelierId` de l'ouvrant actif ; l'**activité
visée** par un acte est l'`ActiviteId` d'origine, que rend `activites[].activite`. Le front envoie le second dans
`activiteVisee` et jamais l'identifiant d'événement.

Ce dossier n'a pas de `sequence` : `perimetre` porte les faits qui ouvrent ou visent l'activité, gestes tardifs
compris, et `activites` l'activité échue (`etat` `ECHUE`, `fin` à l'échéance, `duree` de treize heures). Une activité
encore en cours n'entre pas dans le dossier d'une adresse `SANS_ANOMALIE`. `enConflit` reste calculé sur le périmètre :
il devient vrai si un geste tardif appartient à une autre séquence en conflit.

`finAutomatique` est calculé sur le même périmètre : il est vrai tant qu'une activité concernée reste terminée
automatiquement, quel que soit l'état de l'adresse. « Anomalie traitée » se lit donc `enConflit` faux **et**
`finAutomatique` faux, y compris quand l'adresse devient `ANCRE_ANNULEE` (un début corrigé qui repousse l'échéance
rend `finAutomatique` faux ; un début corrigé mais encore échu le laisse vrai, et le remplaçant ouvre son propre
dossier `FIN_AUTOMATIQUE`).

L'ensemble des activités concernées est stable avant et après un acte : c'est l'activité de l'ancre, identifiée par
son `ActiviteId` d'origine, que la correction de l'ouvrant conserve. Seul le dossier d'une séquence en conflit
s'élargit aux activités que les faits d'un acte ajoutent. La transition tardive qu'on corrige ouvre une autre
activité, avec sa propre adresse et sa propre ligne de liste : même échue à l'évaluation, elle n'entre pas dans ce
dossier, et `finAutomatique` ne juge que l'anomalie du dossier. `perimetre` liste des faits : `perimetre.activites`
peut encore nommer l'activité qu'ouvre ce geste, que `activites[]` ne contient pas.

Dossier d'une fin automatique :

```json
{
  "kind": "FIN_AUTOMATIQUE",
  "enConflit": false,
  "finAutomatique": true,
  "adresse": { "suivi": "3e1d8181-…", "pointage": "ab8f8dba-…" },
  "revision": 0,
  "evaluation": "2026-05-10T22:00:00Z",
  "diagnostics": [],
  "activites": [
    {
      "evenement": "ab8f8dba-…",
      "activite": "ab8f8dba-…",
      "operateurId": "33333333-…",
      "posteId": "55555555-…",
      "categorie": "TRAVAIL",
      "debut": "2026-05-10T08:00:00Z",
      "fin": "2026-05-10T21:00:00Z",
      "etat": "ECHUE",
      "duree": "PT13H"
    }
  ],
  "perimetre": { "activites": ["ab8f8dba-…"], "pointages": ["ab8f8dba-…"], "nombrePointages": 1, "…": "…" },
  "continuations": []
}
```

Le détail adressé lit le journal, la clôture et la révision d’une même version committée du suivi,
sans verrouiller les rédacteurs. Une écriture concurrente peut rendre cette version ancienne après sa lecture ;
l'écriture contrôle toujours la révision sous verrou. Cette garantie ne constitue pas un instantané entre
plusieurs appels ni avec les libellés du référentiel. Voir [l’ADR 0007](adr/0007-read-addressed-workshop-aggregates-coherently.md).

La révision commence à zéro à l'engagement et progresse à chaque modification effective du journal ou de la clôture, par toutes
les routes, pointages Pupitre compris. Un rejeu strict ou un geste absorbé ne la fait pas progresser.
La valeur Java est `RevisionDuSuivi`, séparée des identités de faits et du nombre d'événements.

Les faits portent leurs IDs bruts opérateur/poste indépendamment de la résolution des fiches, leur
activité créée et visée, auteur, survenue, enregistrement, origine, annulation et lien `remplace`.
Le poste absent est distinct d'une fiche absente pour un poste identifié. Les activités exposent
`EN_COURS`, `TERMINEE`, `ECHUE` ou `A_RESOUDRE` ; seuls les états terminés
portent une durée définitive ISO 8601. Les neuf décimales d'un instant sont conservées.

Le diagnostic vient de l'interprétation du domaine au moment de la contradiction. Il identifie le
geste, sa cible, l'ouvrant actif ou annulé, le fait qui a terminé ou remplacé la cible et une raison
structurée. Les premières familles sont cible remplacée, déjà terminée, pas encore ouverte, ouvrant
annulé, même catégorie, cible échue avec une autre activité en cours et contradiction avec une
régularisation. Le mapper REST ne rejoue aucun automate.
