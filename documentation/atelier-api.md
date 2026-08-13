# API atelier — guide d'intégration front

Ce document accompagne la spécification OpenAPI (`/swagger-ui.html`, `/v3/api-docs`). La spec dit **ce que** chaque
route accepte et rend ; ce guide dit **pourquoi**, et dans quel ordre les appeler. Les deux sont nécessaires : le
modèle de l'atelier a trois ou quatre partis pris qui rendent une implémentation naïve fausse sans jamais lever
d'erreur.

Le détail métier et sa justification par le verbatim client sont dans
[contexte-metier.md](contexte-metier.md) ; les règles de code, dans [glm-back/CLAUDE.md](../CLAUDE.md).

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

Prise de poste, puis travail :

```
POST /api/atelier/journees                 { "operateur": "<uuid operateur>" }
POST /api/atelier/suivis/{id}/pointages    { "type": "DEBUT", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/suivis/{id}/pointages    { "type": "NON_CONFORMITE", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/journees/pointages       { "operateur": "<uuid>", "type": "PAUSE" }
POST /api/atelier/journees/pointages       { "operateur": "<uuid>", "type": "REPRISE" }
POST /api/atelier/suivis/{id}/pointages    { "type": "FIN", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/journees/pointages       { "operateur": "<uuid>", "type": "DEPART" }
```

Trois pièges :

- `POST /api/atelier/journees/pointages` **n'a pas d'identifiant de journée** : le serveur retrouve seul la journée
  ouverte de l'opérateur. Sans journée ouverte, il répond 404.
- **Une reprise après non conformité se pointe comme un `DEBUT`.** Il n'existe pas de type « reprise ». Ce qui change,
  c'est la `categorie` de l'activité, qui repasse de `NON_CONFORMITE` à `TRAVAIL`.
- `poste` est **toujours facultatif**, comme la `nature`. Une entreprise sans parc machine les laisse vides et doit
  retrouver un comportement nominal, pas un cas dégradé. Ne jamais rendre le champ obligatoire côté formulaire.
- Un poste fourni doit être **habilité pour cet opérateur**, sans quoi 409. Filtrer la liste des postes sur la fiche de
  l'opérateur choisi évite d'avoir à traiter ce refus.

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

### Lire le temps passé

```
GET /api/atelier/suivis/{id}/temps-effectif
```

Rend les intervalles bruts **ramenés aux fenêtres de présence** des opérateurs. Un `DEBUT` à 8 h suivi d'une pause de
midi et d'une reprise à 13 h produit **deux** intervalles, alors qu'un seul pointage a eu lieu. Un intervalle sans
`fin` est encore en cours — c'est un affichage « depuis 8 h 00 », pas une donnée manquante.

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

Toutes les erreurs métier sont des `ProblemDetail` (RFC 7807) portant `title`, `status` et une propriété `message`
lisible.

| Statut | Cas                                                                                                                                                                   |
| ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400    | Corps invalide (Bean Validation) — détail par champ dans `errors`.                                                                                                    |
| 403    | Jeton sans entreprise connue, ou rôle insuffisant.                                                                                                                    |
| 404    | Suivi, journée, événement ou élément de fabrication introuvable ; ou aucune journée ouverte pour cet opérateur.                                                       |
| 409    | Élément déjà engagé, journée déjà ouverte, élément clôturé, événement déjà annulé, transition impossible, événement antérieur à l'engagement, **saisie concurrente**. |

Les **409 de transition** sont les plus fréquents à l'usage : une `REPRISE` sans `PAUSE`, deux `DEBUT` consécutifs sur
la même activité, un `DEPART` sur une journée déjà fermée. Ils portent un `message` explicite — l'afficher plutôt que
le remplacer par un texte générique.

La **saisie concurrente** est le seul 409 qui ne dit rien de la saisie elle-même : elle était valide, mais quelqu'un a
pointé sur le même élément ou la même journée entre la lecture et l'écriture. C'est le seul cas où **rejouer** l'appel
tel quel est la bonne réaction — relire l'agrégat, et reproposer la saisie.

> Limite connue : une `dateDeSurvenue` postérieure à l'instant courant (corriger un événement « dans le futur »)
> viole un invariant du domaine et ressort aujourd'hui en **500**, faute de mapping pour `AssertionException`. Côté
> front, borner les sélecteurs de date à l'instant présent.

---

## 5. Limites de l'implémentation actuelle

- **`nature` est vide dès qu'aucun poste n'est pointé**, puisqu'elle vient du poste. Un pointage sans poste n'a pas de
  nature, et c'est le comportement nominal d'une entreprise sans parc machine.
- **Régulariser sur un poste dont l'opérateur a été dé-habilité depuis est refusé** (409), l'habilitation étant
  vérifiée sur les trois actes de correction. Retirer une habilitation ferme donc aussi la porte au rattrapage des
  saisies passées sur ce poste.
- **Ni un opérateur ni un poste ayant servi à pointer ne se supprime** : `DELETE /api/operateurs/{id}` et
  `DELETE /api/postes-de-travail/{id}` répondent 409. Un écran d'administration doit le prévoir plutôt que le
  découvrir.
