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
n'est pas valorisée, ou quand aucun poste n'est fourni pour `coutHoraire`.

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
- `GET …/temps-effectif` rend chaque intervalle avec son `activite` et `finAutomatique` : vrai quand l'activité est
  terminée automatiquement à son échéance, faute de fin réelle. C'est l'anomalie à signaler ; `fin` vaut alors
  l'échéance.
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

Le gestionnaire et l'opérateur peuvent consulter `GET /api/atelier/conflits`. Cette page lit les projections
courantes sans charger les journaux : une ligne désigne une séquence par `adresse.suivi` et `adresse.pointage`,
avec la révision du suivi, les références brutes, les fiches disponibles, le premier instant métier exact et
le nombre de pointages. `operateur` et `element` cherchent du texte partiel sans casse, y compris dans les
identifiants, et se combinent avant `page` et `size`. Les caractères `%`, `_` et `\` restent littéraux.
Le tri suit le premier pointage, puis les identifiants du suivi et de l'ancrage. `total` et les `lignes` proviennent
d'une même acquisition SQL. `complete: true` caractérise une lecture réussie, même vide ; un échec d'acquisition
remonte en erreur HTTP. Les explications détaillées appartiennent au dossier de la séquence.

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

- `conflits[]`, dans `RestSuiviDAtelier` — le détail et la réponse de chaque écriture —, une entrée par séquence :
  `operateur` et `poste` résolus, `activites`, les identités des activités **à résoudre** dans l'ordre de leur
  ouverture, et `pointages`, les identifiants des faits de la séquence dans l'ordre du journal. Tableau vide quand le
  journal est cohérent. La grille (`GET /api/atelier/suivis`) ne le porte pas : il se lit sur le détail.
- Une activité à résoudre n'est **ni en cours ni terminée** : absente d'`activitesEnCours`, sans fin, sans durée, et
  son échéance ne la termine pas. L'`etat` du suivi se juge sur les seules activités interprétables : une nouvelle
  ouverture après le conflit est en cours, et l'élément avec elle.
- `GET …/temps-effectif` rend son intervalle avec `aResoudre: true`, sans `fin` : aucune durée n'est à présenter comme
  définitive, et elle ne vaut pas zéro. `finAutomatique` y est toujours faux.
- Hors de la séquence, les activités du même poste gardent leur lecture : ce qui précède la contradiction, et
  l'ouverture pointée après elle.

Les séquences et leurs deux listes sont projetées par atelier à chaque écriture, sans dépendre de l'instant de
lecture. Une séquence sans activité à résoudre reste conservée dans cette projection ; la résolution la retire.
Le journal demeure la source de vérité. La plage possible d'une activité à résoudre est bornée par sa fin au plus
tard : échéance ou régularisation recevable plus tardive, limitée par la clôture qui ne la prolonge jamais.
Cette borne vient des faits et la projection est réécrite à chaque correction, annulation, résolution ou clôture.

**Pour le pupitre, un pointage conservé en conflit est un succès.** Il est acquitté `201` — `200` au rejeu, sans second
fait — et son identifiant figure dans `conflits[].pointages` : c'est ce qui le distingue d'un refus (`4xx`), et il ne
doit pas être republié. Sur un poste dont une séquence est en conflit, seule une nouvelle **ouverture** a un sens : ne
viser par une fin ou une transition aucune activité listée dans `conflits[].activites`.

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

La liste rend une page de **`RestSuiviDAtelierEnGrille`**, sans propriétés `journal` ni `conflits` (ni tableau vide, ni
valeur `null`). Tous les autres champs sont conservés : `id`, `element`, `nom`, `type`, `engagePar`, `engageLe`,
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

### Le référentiel du pupitre, en un seul appel (rôle `USER`)

```
GET /api/pupitre/referentiel
```

Un pupitre hors ligne ne reconstitue plus son cache en paginant `GET /api/operateurs` puis
`GET /api/atelier/suivis`. Cette route rend **tout d'un coup** : les opérateurs désignables avec leur identifiant et
leurs postes habilités, et les éléments encore pointables avec leurs activités en cours.
Les séquences en conflit sont également rendues sur chaque suivi.

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
      "type": "ORDRE_DE_FABRICATION",
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
      ],
      "conflits": []
    }
  ]
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

Chaque suivi du référentiel porte aussi `conflits[]` : `operateur`, `poste` facultatif, `activites[]`
(identités stables, éventuellement aucune) et `pointages[]` (identités des faits dans l’ordre métier).
Les activités en conflit ne figurent jamais dans `activites[]` du suivi. Une ouverture cohérente peut y être
en cours alors que le conflit reste rendu ; le gestionnaire le résout par les actes décrits ci-dessous.

L'écriture, elle, ne change pas : ce sont toujours les `POST` de l'écran d'atelier ci-dessus, avec l'UUID de geste
créé par le pupitre et la `dateDeSurvenue` conservée hors ligne.

### Écran back-office (rôle `GESTIONNAIRE`)

```
POST   /api/atelier/suivis                                    engager un élément
PUT    /api/atelier/suivis/{id}/cloture                        clôturer, ou déplacer la clôture
DELETE /api/atelier/suivis/{id}/cloture                        rouvrir
POST   /api/atelier/suivis/{id}/regularisations                rattraper une saisie oubliée
POST   /api/atelier/suivis/{id}/evenements/{evtId}/annulation  annuler une saisie en trop
PUT    /api/atelier/suivis/{id}/evenements/{evtId}             corriger une saisie fausse
```

La régularisation et la correction portent `intention` et `cible` comme un pointage. Une fin oubliée se régularise
donc sur l'activité qu'elle termine.

**La clôture ne fige rien pour le gestionnaire** : régularisation, annulation et correction restent possibles ensuite,
et la clôture elle-même se déplace (`PUT`) ou s'annule (`DELETE`). Ne pas griser les actions de correction sur un
élément clôturé.

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

### Lire le temps passé

```
GET /api/atelier/suivis/{id}/temps-effectif
```

Rend les intervalles des activités de l’élément, bornés par les faits d’activité, la clôture et leur échéance.
La pause de midi, pointée par un `FIN` ciblé puis une ouverture `DEBUT`, en produit deux. Un
intervalle sans `fin` est encore en cours — c'est un affichage « depuis 8 h 00 », pas une donnée manquante — sauf s'il
est à résoudre.

Chaque intervalle porte son `activite`, l'identité de l'activité dont il vient. `finAutomatique: true` signale une
activité terminée automatiquement à son échéance, faute de fin réelle : `fin` vaut l'échéance, 13 h après le début. Un
`DEBUT` à 8 h que l'opérateur n'arrête jamais donne ainsi un intervalle terminé à 21 h, avec cette anomalie, que la fin
régularisée par le gestionnaire remplace. `aResoudre: true` signale une activité d'une séquence en conflit : rendue
telle quelle, sans `fin`, elle n'a aucune durée à compter tant que le gestionnaire n'a pas tranché.
Cette route relève l'instant sur l'horloge du serveur ; elle ne prend pas de paramètre `evaluation`.

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

Une `dateDeSurvenue` strictement postérieure à l'instant courant répond 400 avec le code stable
`date-de-survenue-future`. Le pupitre peut alors corriger son contenu et réutiliser le même UUID.

---

## 5. Limites de l'implémentation actuelle

- **`nature` est vide dès qu'aucun poste n'est pointé**, puisqu'elle vient du poste. Un pointage sans poste n'a pas de
  nature, et c'est le comportement nominal d'une entreprise sans parc machine.
- **`coutHoraire` et `tauxHoraire` ne sont exposés que sur les événements du journal**, pas sur `temps-effectif`
  (les intervalles rendus par `GET /api/atelier/suivis/{id}/temps-effectif`) : l'atelier capture ces valeurs, il ne
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
"valeur": ... }` pour une valeur connue, y compris zéro ; `{ "complete": false }` pour une valeur à
résoudre, sans chiffre ni somme partielle. Travail, non conformité, machine et main d'œuvre sont indépendants.
Une activité à résoudre rend ses valeurs propres concernées inconnues. Le temps et la machine d'une
activité terminée restent chiffrés, même si son partage humain dépend d'un conflit.

Pour la main d'œuvre, l'incertitude occupe toute la plage possible factuelle `[debut, finAuPlusTard)`,
bornée à `evaluation`. Elle ne propage que si elle change réellement le nombre de postes distincts : un
poste déjà certainement occupé ne le change pas ; un taux absent produit zéro indépendamment du diviseur.

`conflits` rend les séquences de l'élément et toutes celles responsables des valeurs inconnues, même sur
un autre élément, avec `element`, `operateur`, `poste` facultatif, les identités originales `activites`
et les faits actifs `pointages`. Une séquence sans activité à résoudre reste visible pour son élément
sans rendre les montants incomplets. Résoudre les faits par annulation ou correction recalcule les valeurs.
Cette route ne prend pas de paramètre d'évaluation et ne garantit pas un instantané face aux écritures concurrentes.

## Contrat de résolution manuelle en préparation

Les coutures suivantes sont arrêtées pour la livraison des dossiers de conflit. Cette section décrit
le contrat à implémenter ; les routes correspondantes ne sont pas encore publiées dans OpenAPI.
Leur disponibilité sera établie par les scénarios REST et le contrat généré, jamais par ce tableau seul.

| Capacité               | Route                                                                    | Droit                                 |
| ---------------------- | ------------------------------------------------------------------------ | ------------------------------------- |
| Liste paginée          | `GET /api/atelier/conflits?operateur=…&element=…&page=0&size=5`          | `USER` ou `GESTIONNAIRE`              |
| Dossier adressé        | `GET /api/atelier/suivis/{suivi}/conflits/{pointage}`                    | `USER` ou `GESTIONNAIRE`              |
| Aperçu sans écriture   | `POST /api/atelier/suivis/{suivi}/conflits/{pointage}/apercus`           | `GESTIONNAIRE`                        |
| Confirmation           | `POST /api/atelier/suivis/{suivi}/confirmations-de-resolution`           | `GESTIONNAIRE`                        |
| Vérification canonique | `GET /api/atelier/suivis/{suivi}/confirmations-de-resolution/{commande}` | `GESTIONNAIRE`, auteur de la commande |

L'adresse d'un dossier est le couple suivi/pointage ; une identité technique de projection n'est pas
une adresse. Le résultat porte `EN_CONFLIT`, `INTROUVABLE`, `ANCRE_ANNULEE` ou `HORS_CONFLIT`.
Un suivi absent du tenant courant répond 404 sans journal. Un suivi accessible conserve son journal
dans les trois résultats d'adresse sans conflit ; aucun de ces résultats ne redirige implicitement.

Le dossier et son avant/après portent la même `revision` numérique du suivi, son `evaluation`, le
journal complet, les activités concernées et les conflits restants. La révision commence à zéro à
l'engagement et progresse à chaque modification effective du journal ou de la clôture, par toutes
les routes, pointages Pupitre compris. Un rejeu strict ou un geste absorbé ne la fait pas progresser.
La valeur Java est `RevisionDuSuivi`, séparée des identités de faits et du nombre d'événements.

Les faits portent leurs IDs bruts opérateur/poste indépendamment de la résolution des fiches, leur
activité créée et visée, auteur, survenue, enregistrement, origine, annulation et lien `remplace`.
Le poste absent est distinct d'une fiche absente pour un poste identifié. Les activités exposent
`EN_COURS`, `TERMINEE`, `TERMINEE_AUTOMATIQUEMENT` ou `A_RESOUDRE` ; seuls les états terminés
portent une durée définitive ISO 8601. Les neuf décimales d'un instant sont conservées.

Le diagnostic vient de l'interprétation du domaine au moment de la contradiction. Il identifie le
geste, sa cible, l'ouvrant actif ou annulé, le fait qui a terminé ou remplacé la cible et une raison
structurée. Les premières familles sont cible remplacée, déjà terminée, pas encore ouverte, ouvrant
annulé, même catégorie, cible échue avec une autre activité en cours et contradiction avec une
régularisation. Le mapper REST ne rejoue aucun automate. Les propositions portent un code, les faits
qui les étayent et un acte explicite ; aucune n'est sélectionnée et aucun motif n'est prérempli.

Le corps d'aperçu porte `commande` (UUID créé par le client), `revision` et `acte`. L'acte porte
`kind` (`CORRECTION`, `ANNULATION`, `REGULARISATION`) et les champs propres à cette intention :
`pointage` visé et `motif` pour l'annulation ; `pointage` visé, `fait` et `motif` pour la correction ;
`fait` pour la régularisation, sans justificatif obligatoire. Le fait conserve les UUID
`operateur`/`poste`, `type`, `intention`, `activiteVisee` éventuelle et chaîne exacte `instant` ; auteur et tarifs restent
des valeurs serveur. L'aperçu rend la commande, l'acte repris, avant/après, évaluation, révision,
`reference` opaque et `expireLe`. Il ne réserve aucune identité et n'enregistre rien.

La confirmation reçoit uniquement `commande` et `reference`. Le reçu rend la commande, l'adresse,
l'acte exact, la révision avant/après, l'instant d'enregistrement, le résultat au dossier d'origine
et les continuations explicites vers les ancrages actifs des conflits restants. L'enregistrement
d'un acte laissant un conflit est une réussite. La vérification rend `ENREGISTREE` avec le reçu
canonique, ou `NON_ATTESTEE` : l'absence momentanée d'un reçu ne permet pas de conclure à un rollback.

La référence autoportante, son intégrité, sa rotation et sa validité sont arrêtées dans
[l'ADR 0004](adr/0004-authenticate-stateless-resolution-previews.md). Les codes de refus seront
publiés avec leurs handlers et les scénarios REST dans [le catalogue](codes-erreur.md).
