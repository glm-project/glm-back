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
| `USER`         | L'opérateur : pointer sa présence et son travail, lire.                                               |
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

Aucun état, aucun compteur, aucun intervalle n'est stocké. `etat`, `activitesEnCours`, `amplitude`, `fenetres` et le
temps effectif sont **recalculés du journal à chaque lecture**.

Conséquence directe pour le front : après toute écriture, la réponse contient déjà l'agrégat entièrement recalculé.
**Ne jamais reconstruire l'état côté client** en appliquant l'événement localement — re-rendre depuis la réponse.

### L'horodatage est bitemporel

Chaque événement porte deux dates :

- `dateDeSurvenue` — l'heure **métier**, celle où le fait a eu lieu ;
- `dateDEnregistrement` — l'heure de la **saisie**.

Une régularisation se reconnaît à l'écart entre les deux, exposé par le booléen `estUneRegularisation` — jamais à
l'identité de l'auteur. Un affichage honnête montre l'heure métier, et signale la saisie différée (« pointé le 11/05
à 9 h 15 pour le 10/05 à 17 h »).

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

### La présence est écrite une seule fois

C'est le parti pris structurant. Pause, reprise et départ sont des faits de la **journée de travail de l'opérateur**,
jamais recopiés dans le journal des éléments sur lesquels il travaille.

**Ne jamais boucler sur les éléments en cours pour répercuter une pause.** Un seul `POST /api/atelier/journees/pointages`
suffit, quel que soit le nombre d'éléments : c'est ce qui donne au client son bouton unique. Le croisement est fait à la
lecture, par `GET /api/atelier/suivis/{id}/temps-effectif`.

C'est aussi ce qui permet à une seule régularisation de départ de refermer d'un coup tous les éléments qu'un opérateur
avait laissés ouverts en rentrant chez lui.

---

## 3. Les écrans et leurs appels

### Écran d'atelier (rôle `USER`)

Le tableau des éléments à faire :

```
GET /api/atelier/suivis?etats=EN_ATTENTE&etats=EN_COURS&etats=INTERROMPU
```

Les filtres sont **tous facultatifs** — cet écran ne défile pas et n'a aucune notion de date. `etats` absent ne filtre
rien. `debut`/`fin` ne servent qu'au back-office, et **une borne seule est ignorée** : il faut les deux.

La liste rend une page de **`RestSuiviDAtelierEnGrille`**, sans propriété `journal` (ni tableau vide, ni valeur
`null`). Tous les autres champs sont conservés : `id`, `element`, `nom`, `type`, `engagePar`, `engageLe`, `etat`,
`cloturePar`, `clotureLe` et `activitesEnCours`. L'état et les activités restent calculés par le serveur depuis
le journal ; ce changement allège la réponse HTTP et le cache du pupitre, pas la relecture en base.

Le journal complet, **événements annulés compris**, se lit via `GET /api/atelier/suivis/{id}`, qui conserve
`RestSuiviDAtelier`, comme les réponses des actes métier. Un consommateur qui lisait le journal dans la liste doit
migrer vers le détail. Côté `glm-front`, synchroniser le contrat avec `npm run api:sync && npm run api:types` ;
la réévaluation de la limite de pagination de la grille reste un suivi côté front.

Prise de poste, puis travail : chaque geste du pupitre porte un `id` UUID créé une fois par le front. Il peut aussi
porter `dateDeSurvenue`, l'heure réelle conservée quand le pupitre a été hors ligne.

```
POST /api/atelier/journees                 { "id": "<uuid geste>", "operateur": "<uuid operateur>" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid geste>", "type": "DEBUT", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid geste>", "type": "NON_CONFORMITE", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/journees/pointages       { "id": "<uuid geste>", "operateur": "<uuid>", "type": "PAUSE" }
POST /api/atelier/journees/pointages       { "id": "<uuid geste>", "operateur": "<uuid>", "type": "REPRISE" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid geste>", "type": "FIN", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/journees/pointages       { "id": "<uuid geste>", "operateur": "<uuid>", "type": "DEPART" }
```

Trois pièges :

- `POST /api/atelier/journees/pointages` **n'a pas d'identifiant de journée** : le serveur retrouve seul la journée
  ouverte de l'opérateur. Sans journée ouverte, il répond 404.
- **Une arrivée n'est jamais refusée parce qu'une journée est déjà ouverte.** Sous le seuil d'amplitude de
  l'entreprise, elle est absorbée : `200` et la journée en cours, rien d'ajouté — un poste de nuit peut se
  réidentifier à 3 h. Au-delà, la journée en cours est **abandonnée** et l'arrivée en ouvre une nouvelle (`201`).
- **Un geste de présence sans journée ouverte en ouvre une** (`201`), comme sur une journée abandonnée : arrivée
  implicite puis geste. **Un geste redondant** — pause déjà en pause, reprise déjà présent — **est absorbé** (`200`,
  rien d'ajouté). Restent refusés, définitivement, l'opérateur, le poste ou l'élément inconnu (404), le geste rejoué dans le désordre
  (409) et l'UUID réutilisé avec un autre contenu (409).
- **Arrêter une activité qui n'est pas en cours, ou un élément clôturé, est absorbé** (`200`). Démarrer ou pointer
  une non conformité sur un élément clôturé reste refusé (`409 suivi-d-atelier-cloture`) : c'est le seul refus à
  afficher à l'opérateur, « OF clôturé, vous ne pouvez plus pointer dessus ».
- **Deux saisies simultanées ne sont plus un refus** : le serveur rejoue lui-même l'écriture devancée.
- **Un geste reçu pour une journée abandonnée ouvre une nouvelle journée** (`201`) : une arrivée implicite à l'heure
  du geste, sous un identifiant du serveur, puis le geste. Une reprise s'y réduit à l'arrivée ; un départ tardif
  donne une journée de durée nulle. Le seuil se juge sur l'heure du geste (`dateDeSurvenue`), pas sur sa réception.
- **Une reprise après non conformité se pointe comme un `DEBUT`.** Il n'existe pas de type « reprise ». Ce qui change,
  c'est la `categorie` de l'activité, qui repasse de `NON_CONFORMITE` à `TRAVAIL`.
- **Un `DEBUT` sur une activité déjà en cours la relance** au lieu d'être refusé, de même qu'une `NON_CONFORMITE` sur
  une non conformité en cours : la période précédente s'arrête à l'heure du geste, une nouvelle commence. L'opérateur
  qui revient sur un élément resté ouvert la veille n'est jamais bloqué, et un double appui n'ajoute aucun temps.
- `poste` est **toujours facultatif**, comme la `nature`. Une entreprise sans parc machine les laisse vides et doit
  retrouver un comportement nominal, pas un cas dégradé. Ne jamais rendre le champ obligatoire côté formulaire.
- Un poste fourni doit être **habilité pour cet opérateur**, sans quoi 409. Filtrer la liste des postes sur la fiche de
  l'opérateur choisi évite d'avoir à traiter ce refus.
- Un envoi accepté répond **201**. Rejouer exactement le même corps répond **200**, sans créer de second événement ;
  conserver donc l'UUID dans la file offline jusqu'à l'acquittement. Réutiliser cet UUID avec un autre contenu répond
  409 (`identifiant-evenement-reutilise`). Une date future répond 400 et ne réserve pas l'UUID.

Les états d'un élément :

| `etat`       | Sens                                                               |
| ------------ | ------------------------------------------------------------------ |
| `EN_ATTENTE` | Engagé, aucun pointage actif. Personne n'y a encore touché.        |
| `EN_COURS`   | Au moins une activité ouverte — **y compris en non conformité**.   |
| `INTERROMPU` | Il y a eu du travail, mais plus personne n'y est.                  |
| `CLOTURE`    | Clôturé. N'accepte plus de pointage (409), mais reste corrigeable. |

Attention : une non conformité **ne fait pas** passer à `INTERROMPU`. L'activité reste ouverte — ce temps-là se compte
aussi —, seule sa `categorie` change. Pour signaler visuellement une non conformité, lire
`activitesEnCours[].categorie`, pas `etat`.

### Le référentiel du pupitre, en un seul appel (rôle `USER`)

```
GET /api/pupitre/referentiel
```

Un pupitre hors ligne ne reconstitue plus son cache en paginant `GET /api/operateurs` puis
`GET /api/atelier/suivis`. Cette route rend **tout d'un coup** : les opérateurs désignables avec leur matricule et
leurs postes habilités, et les éléments encore pointables avec leurs activités en cours.

```json
{
  "genereLe": "2026-09-14T09:31:02.418Z",
  "operateurs": [
    {
      "id": "…",
      "nom": "Dupont",
      "prenom": "Jean",
      "matricule": "049",
      "etat": "PRESENT",
      "presentJusqua": "2026-09-14T20:00:00Z",
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
      "activites": [{ "operateur": "…", "poste": "…", "categorie": "TRAVAIL", "depuis": "2026-09-14T08:02:00Z" }]
    }
  ]
}
```

Six choses à savoir avant de brancher un cache dessus :

- **Elle n'est pas paginée, et c'est le point.** Tout est lu en un appel et une transaction unique : plus de boucle
  de pages à écrire, ni de gardes sur les totaux, les doublons ou les pages vides — ces trois gardes existaient
  contre le recousage de pages, qui n'existe plus. En revanche la réponse **n'est pas un instantané strict** : les
  opérateurs et les suivis sont lus par deux requêtes successives sous l'isolation par défaut de PostgreSQL, et un
  changement validé entre les deux se voit dans la seconde. Concrètement, une activité en cours peut désigner un
  opérateur absent de la liste jointe — afficher l'identifiant brut plutôt que planter, l'appel suivant recollera.
  Une version antérieure de ce document annonçait une lecture répétable : elle n'a jamais fonctionné et a été
  retirée, la route répondait `500` à chaque appel.
- **`genereLe` est la version, et c'est une date.** Elle dit quand le serveur a produit la réponse — de quoi
  afficher « référentiel du 14/09 à 09:31 » et mesurer un retard. Elle **change à chaque appel**, y compris quand
  rien n'a bougé : ce n'est pas la date du dernier changement, et s'en servir pour décider d'un rafraîchissement
  n'aurait aucun sens. Il n'y a ni `ETag` ni `304`.
- **`etat` dit quelles commandes de présence proposer.** `ABSENT`, `PRESENT` ou `EN_PAUSE` : c'est l'état de la
  journée en cours de l'opérateur, **tant qu'elle n'est pas abandonnée**. Une journée sans départ dont l'amplitude
  dépasse le seuil de l'entreprise (13 h par défaut) est abandonnée, et l'opérateur redevient `ABSENT` : le pupitre ne
  lui propose plus que l'arrivée. `ABSENT` vaut aussi pour qui n'a aucune journée en cours ; il reste dans la liste,
  qui rend les opérateurs **désignables**, pas les opérateurs présents. C'est ce champ qui évite d'offrir hors ligne
  une transition que le serveur refusera (`409`, une pause ne suit pas `EN_PAUSE`).
  **`presentJusqua`** dit jusqu'à quand : l'arrivée plus le seuil. Hors ligne, le pupitre bascule seul l'opérateur à
  `ABSENT` passé cet instant, sans attendre le référentiel suivant. Il est absent quand l'opérateur est `ABSENT`.
  Ce que l'état ne porte pas, volontairement : **aucun instant de début** — pas de « en pause depuis 10 h 12 »,
  l'écran n'affiche que l'état — et **aucun marqueur d'idempotence** : les gestes locaux pas encore reflétés se replient avec
  le marqueur que le pupitre tient déjà lui-même, comme pour les pointages.
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
POST   /api/atelier/suivis/{id}/regularisations                rattraper une saisie oubliée
POST   /api/atelier/suivis/{id}/evenements/{evtId}/annulation  annuler une saisie en trop
PUT    /api/atelier/suivis/{id}/evenements/{evtId}             corriger une saisie fausse
```

Les mêmes trois actes existent sur `/api/atelier/journees/{id}/...` pour la présence.

**La clôture ne fige rien pour le gestionnaire** : régularisation, annulation et correction restent possibles ensuite,
et la clôture elle-même se déplace (`PUT`) ou s'annule (`DELETE`). Ne pas griser les actions de correction sur un
élément clôturé.

`PUT .../evenements/{evtId}` est une **correction** : une annulation et une régularisation en un seul appel. Le journal
en ressort avec deux événements de plus, pas un — l'ancien annulé, le nouveau à l'heure corrigée.

### Paramétrage de l'entreprise

```
GET /api/parametrage                          USER, GESTIONNAIRE   { "amplitudeMaximale": "PT13H", "derniereModification": { "auteur", "date" } }
PUT /api/parametrage/amplitude-maximale       GESTIONNAIRE         { "valeur": "PT12H30M" }
```

L'**amplitude maximale** est la durée, depuis l'arrivée, au-delà de laquelle une journée sans départ sera abandonnée.
Elle vaut 13 h par défaut, se saisit à la minute et reste strictement sous 24 h : toute autre valeur répond 400.
`derniereModification` est absente tant que personne ne l'a changée.

### Anomalies de présence (rôle `GESTIONNAIRE`)

```
GET /api/atelier/anomalies?operateur={uuid}&type={JOURNEE_SANS_DEPART|AMPLITUDE_EXCESSIVE}&page=0&size=20
```

Les journées que le gestionnaire doit regarder, la plus récente d'abord : `type`, `journee` (l'identifiant à
régulariser ou corriger), `operateur`, `arrivee`, et pour une amplitude excessive `depart` et `amplitude`. Rien n'est
stocké : une ligne disparaît dès que la régularisation la résout. Un type inconnu répond 400, un opérateur 403.

### Lire le temps passé

```
GET /api/atelier/suivis/{id}/temps-effectif
```

Rend les intervalles bruts **ramenés aux fenêtres de présence** des opérateurs. Un `DEBUT` à 8 h suivi d'une pause de
midi et d'une reprise à 13 h produit **deux** intervalles, alors qu'un seul pointage a eu lieu. Un intervalle sans
`fin` est encore en cours — c'est un affichage « depuis 8 h 00 », pas une donnée manquante.

Un intervalle **`presume: true`** repose sur une fin de journée présumée : l'opérateur n'a pas pointé son départ, et sa
journée, abandonnée au-delà de l'amplitude maximale, a été fermée à son dernier fait connu. L'afficher comme « à
confirmer » ; il redevient pointé dès que le gestionnaire régularise le départ.

### Présence et paie

`GET /api/atelier/journees/{id}` expose **à la fois** :

- `amplitude` — de l'arrivée au départ, pauses comprises ;
- `fenetres` — les intervalles de présence effective, pauses retirées.

Le back-end **ne choisit pas** laquelle compte pour la paie : la question est ouverte côté client. Ne pas en câbler une
en dur dans un écran de synthèse sans l'avoir tranchée.

`amplitude` est **absente tant que la journée est ouverte** (pas de départ), et une `fenetre` sans `fin` est en cours.

Enfin : une journée de travail est **une venue**, pas un jour calendaire. Le contexte ne connaît ni fuseau horaire ni
date — un poste de nuit à cheval sur deux jours est une seule journée de travail. Ne jamais grouper par date côté
client en supposant l'inverse.

---

## 4. Erreurs

Toutes les erreurs métier sont des `ProblemDetail` (RFC 7807) portant un `type`, un `title`, un `status` et une
propriété `message` lisible.

Le `type` est un **code stable** de la forme `urn:glm:erreur:<contexte>:<code>` — c'est **lui** qu'on teste pour
brancher, jamais le couple `status` + `title` : le titre est une phrase française qui se reformule, et le même titre
sort de plusieurs contextes. Le catalogue complet est dans [documentation/codes-erreur.md](codes-erreur.md).

Deux statuts du tableau ci-dessous n'en portent pas : le **400** de Bean Validation, qui se lit par son `errors`
(`Map<champ, message>`), et le **403**, qui vient de la chaîne de filtres sans corps du tout.

| Statut | Cas                                                                                                                                                                                            |
| ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400    | Corps invalide (Bean Validation) — détail par champ dans `errors` — ou date de survenue future.                                                                                                |
| 403    | Jeton sans entreprise connue, ou rôle insuffisant.                                                                                                                                             |
| 404    | Suivi, journée, événement ou élément de fabrication introuvable ; ou aucune journée ouverte pour cet opérateur.                                                                                |
| 409    | Élément déjà engagé, élément clôturé, événement déjà annulé, transition impossible, événement antérieur à l'engagement, UUID réutilisé, **chevauchement de journées**, **saisie concurrente**. |

Les **409 de transition** sont les plus fréquents à l'usage : une `REPRISE` sans `PAUSE`, une `FIN` sans activité en
cours, un `DEPART` sur une journée déjà fermée. Ils portent un `message` explicite — l'afficher plutôt que
le remplacer par un texte générique.

Le **chevauchement de journées** ne vient que d'un acte du gestionnaire : une régularisation ou une correction de
présence qui ferait se toucher deux journées du même opérateur, jugées du premier au dernier fait connu. Régulariser
le départ oublié de lundi à 17:00 passe ; le saisir à mardi 08:00 alors que mardi est ouvert depuis 07:00 est refusé.

Sur les routes de pointage, la **saisie concurrente** est rejouée par le serveur et ne remonte plus qu'après trois
échecs. Sur les actes du gestionnaire, elle reste le seul 409 qui ne dit rien de la saisie elle-même : elle était valide, mais quelqu'un a
pointé sur le même élément ou la même journée entre la lecture et l'écriture. C'est le seul cas où **rejouer** l'appel
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
- **Ni un opérateur ni un poste ayant servi à pointer ne se supprime** : `DELETE /api/operateurs/{id}` et
  `DELETE /api/postes-de-travail/{id}` répondent 409. Un écran d'administration doit le prévoir plutôt que le
  découvrir.
