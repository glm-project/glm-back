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

| Rôle           | Ce qu'il ouvre                                                                                   |
| -------------- | ------------------------------------------------------------------------------------------------ |
| `USER`         | L'opérateur : pointer ses activités, lire.                                                       |
| `GESTIONNAIRE` | Tout ce que fait un `USER`, plus engager, clôturer et régulariser la fin d'une activité échue.   |
| `ADMIN`        | Administration technique (`/api/admin/**`, `/management/**`) uniquement. **Aucun accès métier.** |

Un `admin.*` qui appelle une route de gestion reçoit 403 : c'est voulu. Utilisateurs de développement (mot de passe
égal au login) : `gestionnaire.impeccmold`, `user.impeccmold`, `gestionnaire.katilys`, `user.katilys`.

**L'auteur d'une saisie n'est jamais dans le corps de la requête** : il est déduit du jeton. Ne pas prévoir de champ
« auteur » dans les formulaires. L'**opérateur**, lui, est bien dans le corps : sur un poste d'atelier partagé, celui
qui saisit n'est pas forcément celui dont on compte le temps.

---

## 2. Les quatre idées à comprendre avant de coder

### Le journal est la seule vérité

Le détail d'atelier reconstruit son agrégat depuis les faits du journal. Atelier réconcilie aussi ses projections
d'activités à chaque écriture ; la feuille, la synthèse, le coût et le référentiel pupitre lisent
cette interprétation et évaluent l'expiration à leur instant explicite. Une régularisation recalcule donc les bornes
et leurs conséquences dans les rapports.

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
(`POST …/regularisations`), même saisie à l'heure du fait ; faux pour tout ce qui passe par `POST …/pointages`, quels que soient le rôle de l'utilisateur et la
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

Pointer sur un poste où l'opérateur n'est pas déclaré répond **409**. C'est vrai du pointage comme de la régularisation. Un écran de pupitre doit donc **ne proposer que les postes de l'opérateur choisi**, lisibles dans
`GET /api/operateurs/{id}`, plutôt que laisser le serveur refuser.

La règle ne joue que si un poste est fourni : sans parc machine, il n'y a rien à habiliter.

### Le journal ne se réécrit jamais

Aucun événement n'est annulé, corrigé ni supprimé : le `journal` rendu par l'API contient tous les faits acceptés, et
rien d'autre. Un pointage ignoré n'y entre pas (voir [Un pointage est jugé à sa
réception](#un-pointage-est-jugé-à-sa-réception)). Le gestionnaire ne fait qu'une chose sur le journal : régulariser la
fin d'une activité échue.

### La pause se traduit en faits d'activité

La pause n'existe pas pour le serveur ([ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)).
Le pupitre envoie une `FIN` par activité en cours ; la reprise ouvre une nouvelle activité en `DEBUT`, ou en
`NON_CONFORMITE` pour celle qui l'était, sur le même poste. La mémoire de reprise est locale au pupitre.

### Une activité oubliée se termine automatiquement à son échéance

Une activité que rien n'a terminée se termine automatiquement à son **échéance** : son début plus 13 heures écoulées,
sans fuseau — le passage à l'heure d'été ne l'allonge ni ne la raccourcit. Rien n'est écrit au journal : la fin
automatique se juge à l'instant d'évaluation de la lecture. Un travail commencé à 8 h et jamais arrêté est en cours à 20 h 59 ;
à 21 h, et à toute lecture ultérieure, il est terminé à 21 h, avec une anomalie.

Ce que les réponses en montrent :

- `activitesEnCours[]`, du détail comme de la grille, ne contient que les activités **en cours à l'instant de la
  lecture** : une activité échue en sort d'elle-même, et l'élément passe `INTERROMPU` si plus rien n'y est en cours.
  Chaque activité porte son `ouverture` — l'identité de son pointage ouvrant, que cible la fin régularisée — et son `echeance`.
- Deux lectures espacées peuvent différer au voisinage d'une échéance : c'est l'instant de lecture qui tranche.

La même échéance vaut pour les gestes, jugés sur leur heure métier, quel que soit le moment où ils arrivent (voir
[Un pointage est jugé à sa réception](#un-pointage-est-jugé-à-sa-réception)) :

- l'échéance est atteinte quand l'heure du geste est **supérieure ou égale** au début plus 13 h. Une fin pointée
  **avant** l'échéance termine l'activité à son heure, même reçue le lendemain : la fin pointée à 17 h et publiée après
  une coupure réseau remplace la fin automatique et retire l'anomalie ;
- une **fin pointée à l'échéance ou après** est ignorée (`APRES_ECHEANCE`, `409 pointage-ignore`) : l'activité garde ses
  13 h et son anomalie, et la ligne d'audit le dit. Un geste pile à l'échéance n'emporte plus ;
- un **début ou une non conformité** pointé à l'échéance ou après est accepté : l'activité échue compte comme terminée,
  la nouvelle activité s'ouvre à son heure, et rien n'est compté entre les deux ;
- une **clôture** postérieure à l'échéance ne prolonge rien ;
- seul le gestionnaire établit une fin réelle au-delà de l'échéance, en **régularisant** la fin
  (`POST …/regularisations`).

### Un pointage est jugé à sa réception

Le pupitre ne pointe que trois choses : `DEBUT`, `NON_CONFORMITE` et `FIN`. Un pointage **ne désigne aucune activité** et ne
porte ni `intention` ni `cible` : un début ou une non conformité ouvre une activité, une fin ferme celle qui est en cours
sur la clé. La **clé** est l'opérateur, l'élément (le suivi) et le poste ; il y a au plus une activité en cours par clé,
et un opérateur qui mène plusieurs éléments ou plusieurs postes a une clé par élément et par poste, jugées à part.

Le serveur juge chaque pointage à son arrivée, premier arrivé premier servi, dans cet ordre :

1. **les contrôles existants** : corps invalide (400), suivi, opérateur ou poste introuvable (404), opérateur non habilité
   (409), et un `DEBUT` ou une `NON_CONFORMITE` sur un élément clôturé (409 `suivi-d-atelier-cloture`, seul refus à
   afficher à l'opérateur) ;
2. **`ANTERIEUR`** : l'heure du geste est strictement plus ancienne que celle du dernier pointage accepté de la clé
   (régularisations comprises), ou bien c'est une `FIN` qui n'est pas postérieure au début de l'activité qu'elle fermerait
   (une activité de durée nulle n'existe pas : elle reste en cours). Pour un `DEBUT` ou une `NON_CONFORMITE`, une heure
   égale passe ; la `FIN` d'un geste composé, à t, ferme une activité ouverte avant t ;
3. **l'échéance**, jugée sur l'heure du geste : atteinte quand elle est supérieure ou égale au début plus 13 h. Une
   activité qui l'a atteinte compte comme terminée ;
4. **le tableau** :

| État de la clé                                              | `DEBUT`                   | `NON_CONFORMITE`         | `FIN`                                                                                                      |
| ----------------------------------------------------------- | ------------------------- | ------------------------ | ---------------------------------------------------------------------------------------------------------- |
| Rien en cours (jamais ouverte, terminée, clôturée ou échue) | accepté, ouvre un travail | accepté, ouvre une NC    | ignoré : `APRES_ECHEANCE` si la dernière activité de la clé est échue et sans fin, sinon `AUCUNE_ACTIVITE` |
| Activité en cours (travail ou NC)                           | ignoré : `DEJA_EN_COURS`  | ignoré : `DEJA_EN_COURS` | accepté, termine l'activité en cours                                                                       |

Les quatre raisons d'un pointage ignoré sont `DEJA_EN_COURS`, `AUCUNE_ACTIVITE`, `APRES_ECHEANCE` et `ANTERIEUR`.
La clôture ferme les activités : une `FIN` postérieure à la clôture est ignorée (`AUCUNE_ACTIVITE`), et une `FIN`
survenue avant la clôture mais reçue après elle est acceptée à son heure.

Ce que le pupitre reçoit :

| Statut  | Cas                                                                                                                                                           |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **201** | Pointage accepté : il entre au journal, le corps est le suivi recalculé.                                                                                      |
| **200** | Renvoi : l'`id` figure déjà dans la table des événements (le journal de n'importe quel élément). Rien n'est écrit, le corps est le suivi de la route.         |
| **409** | `pointage-ignore` : pointage ignoré, ou renvoi d'un pointage déjà ignoré. Ne s'affiche pas : le pupitre retire l'effet local et se recale sur le référentiel. |

Un pointage ignoré n'entre jamais au journal : il laisse une ligne dans la table d'audit `pointage_ignore_d_atelier`
(identifiant, opérateur, suivi, poste, type, heure du geste, heure de réception, raison, identifiant du dernier pointage
accepté comparé), consultée en base, sans endpoint ni écran. **Aucune clé, aucune contrainte** : deux lignes pour un même
renvoi sont acceptées. L'audit est écrit avant le refus (le refus est levé après la validation de la transaction) et
un pointage ignoré prend, comme tout pointage, le verrou du suivi.

**Idempotence**, avant toute règle : (1) l'`id` est dans la table des événements → 200 ; (2) sinon il est dans l'audit →
même refus `pointage-ignore` ; (3) sinon le pointage est jugé. Un `id` ne se réutilise donc jamais avec un autre contenu :
le premier arrivé fait foi. La régularisation applique le même contrôle global (étape 1).

Pour le gestionnaire, un pointage ignoré ne crée aucune anomalie : il ne traite que la fin automatique.

### Le journal se lit par clé, une seule fois

Le journal est la source de vérité, et sa lecture est la seule interprétation. Les faits de chaque clé (opérateur, poste) se lisent dans l'ordre de l'heure du geste, la `FIN` avant
l'ouverture à heure égale, puis par identifiant — jamais par l'ordre de réception :

- une `FIN` du pupitre ferme l'activité en cours de sa clé, sans la désigner ;
- la `FIN` d'une régularisation ferme l'activité qu'elle cible, seule `FIN` à porter une cible ;
- une activité atteint son échéance à son début plus 13 h, borne comprise : elle compte alors comme terminée
  automatiquement. Un `DEBUT` pile à l'échéance est accepté et laisse à l'activité précédente sa fin automatique, jamais
  une fin réelle ; une `FIN` pile à l'échéance n'a aucun effet, comme la règle de réception qui l'ignore.

La règle de réception n'a laissé entrer au journal que des faits qui s'accordent ; la lecture n'en refuse aucun et un fait
qui n'a rien à fermer n'a simplement aucun effet. Elle rend à chaque lecture les activités, leurs fins réelles et leurs
fins automatiques, que le gestionnaire régularise.

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

Le journal complet se lit via `GET /api/atelier/suivis/{id}`, qui conserve
`RestSuiviDAtelier`, comme les réponses des actes métier. Un consommateur qui lisait le journal dans la liste doit
utiliser le détail. Côté `glm-front`, générer le contrat depuis la révision backend épinglée avec
`npm run api:generate`, conformément à son guide API. La limite de pagination de la grille reste un suivi côté front.

Chaque geste d’activité du pupitre porte un `id` UUID créé une fois par le front, son `type`, l'`operateur` et le `poste`
(facultatif). Il peut aussi porter `dateDeSurvenue`, l'heure réelle conservée quand le pupitre a été hors ligne. Il ne
porte ni `intention` ni `cible` : voir [Un pointage est jugé à sa réception](#un-pointage-est-jugé-à-sa-réception).

```
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid A>", "type": "DEBUT", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid B>", "type": "FIN", "operateur": "<uuid>", "poste": "<uuid poste>" }
POST /api/atelier/suivis/{id}/pointages    { "id": "<uuid C>", "type": "NON_CONFORMITE", "operateur": "<uuid>", "poste": "<uuid poste>" }
```

À retenir :

- **Passer du travail à la NC (ou l'inverse) se pointe en deux gestes** à la même heure, la `FIN` d'abord : `FIN` puis
  `NON_CONFORMITE`, ou `FIN` puis `DEBUT`. Il n'existe ni transition ni type « reprise » ; la `categorie` de l'activité
  ouverte (`TRAVAIL` ou `NON_CONFORMITE`) est celle du type pointé.
- **Arrêter un élément après sa clôture est ignoré** (`409 pointage-ignore`, `AUCUNE_ACTIVITE`) : la clôture l'a déjà
  arrêté. Une fin survenue avant la clôture, mais reçue après elle, est acceptée à son heure (`201`). Démarrer ou pointer une non conformité sur un élément clôturé reste
  refusé (`409 suivi-d-atelier-cloture`) : c'est le seul refus à afficher à l'opérateur, « OF clôturé, vous ne pouvez
  plus pointer dessus ».
- **Deux saisies simultanées ne sont plus un refus** : le serveur juge les pointages d'un suivi l'un après l'autre.
- **Démarrer pendant une activité en cours est ignoré** (`DEJA_EN_COURS`) : il ne la relance plus ni ne la termine.
- **À heure métier égale**, le journal range la fin avant l'ouverture, puis départage par l'identifiant : jamais par
  l'heure de réception.
- `poste` est **toujours facultatif**, comme la `nature`. Une entreprise sans parc machine les laisse vides et doit
  retrouver un comportement nominal, pas un cas dégradé. Ne jamais rendre le champ obligatoire côté formulaire.
- Un poste fourni doit être **habilité pour cet opérateur**, sans quoi 409. Filtrer la liste des postes sur la fiche de
  l'opérateur choisi évite d'avoir à traiter ce refus.
- Un envoi accepté répond **201**. Renvoyer un UUID déjà présent dans la table des événements — le journal de n'importe
  quel élément, quel que soit le contenu renvoyé — répond **200**, sans créer de second événement ; conserver donc
  l'UUID dans la file offline jusqu'à l'acquittement. Renvoyer l'UUID d'un pointage ignoré répond de nouveau **409**
  `pointage-ignore`. Il n'y a plus de refus pour un UUID réutilisé avec un autre contenu. Une date future répond 400.

Les états d'un élément :

| `etat`       | Sens                                                                                        |
| ------------ | ------------------------------------------------------------------------------------------- |
| `EN_ATTENTE` | Engagé, aucun pointage. Personne n'y a encore touché.                                       |
| `EN_COURS`   | Au moins une activité en cours à l'instant de la lecture — **y compris en non conformité**. |
| `INTERROMPU` | Il y a eu du travail, mais plus aucune activité n'est en cours : terminée, ou échue.        |
| `CLOTURE`    | Clôturé. N'accepte plus d'ouverture (409), mais la régularisation reste possible.           |

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
`TERMINEE_AUTOMATIQUEMENT` et `finRetenue` porte cette échéance. Cette anomalie reste rendue après une reprise ou
la clôture du suivi : une ouverture de 8 h oubliée garde sa fin automatique de 21 h, même si un autre travail
commence le lendemain. Une fin réelle, pointée avant l'échéance ou régularisée, retire l'anomalie. Les activités terminées réellement sortent
de cette collection.

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
  "categories": ["MOULE", "OF"],
  "dureeMaximaleDActivite": "PT13H"
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
  terminée par un fait ou échue est absente d'`activites`. `ouverture` est l'identité stable de l'activité, celle de son
  pointage ouvrant, que cible la fin régularisée ; `echeance` permet l'expiration hors ligne, à cet
  instant inclus, sans fabriquer de fin. `etat` vaut `EN_COURS` s'il reste une activité interprétable en cours,
  sinon `INTERROMPU` s'il existe un pointage, sinon `EN_ATTENTE`.
- **`dureeMaximaleDActivite` est la durée maximale d'une activité** (`"PT13H"`, ISO 8601) : l'échéance de chaque activité
  est son début plus cette durée. Le pupitre la lit ici au lieu de coder 13 h ; le serveur n'en a qu'une source,
  qu'il partage avec sa règle de réception.
- **Aucun montant.** Ni `tauxHoraire` d'opérateur, ni `coutHoraire` de poste : un écran d'atelier partagé n'a pas à
  les recevoir, et `GET /api/couts-de-revient/{elementId}` reste réservé au `GESTIONNAIRE`.
- **Aucun élément clôturé, aucun journal.** `etat` ne vaut donc jamais `CLOTURE` ici. Le journal complet se lit
  toujours par `GET /api/atelier/suivis/{id}` — c'est aussi lui qu'on relit après un
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

**Idempotence.** La présence de `id` dans la table des événements — le journal de n'importe quel suivi, pas seulement
celui de la route — est vérifiée avant toute règle : un renvoi de la même saisie répond **200** (au lieu de 201), avec le
suivi tel qu'il est, et n'écrit rien — y compris quand l'activité est désormais régularisée.

**Refus**, par ordre de vérification (le code est dans `type`, voir [codes-erreur.md](codes-erreur.md)) :

| Statut | Code                         | Cas                                                                                                 |
| ------ | ---------------------------- | --------------------------------------------------------------------------------------------------- |
| 404    | `activite-visee-introuvable` | Aucun pointage de ce suivi n'a ouvert cette activité.                                               |
| 409    | `activite-deja-regularisee`  | Une régularisation vise déjà cette activité.                                                        |
| 409    | `activite-non-echue`         | L'activité n'est pas une fin automatique : échéance non atteinte, ou terminée par un pointage.      |
| 400    | `date-de-survenue-future`    | L'heure dépasse l'instant présent.                                                                  |
| 409    | `fin-avant-debut`            | L'heure n'est pas postérieure au début de l'activité (une activité n'a jamais une durée nulle).     |
| 409    | `fin-apres-borne`            | L'heure dépasse le début suivant sur la clé (opérateur et poste) ou la clôture : voir `borneDeFin`. |

`saisie-concurrente` (409) reste le refus de concurrence : un pointage s'est glissé entre la lecture et l'écriture,
relire le dossier. Le dossier d'une fin automatique nomme l'élément de fabrication (`elementId`, `designation`, comme la ligne de la liste) et donne la borne `borneDeFin` : le plus tôt du début suivant sur la
clé et de la clôture, ou rien ; l'instant présent borne toujours la fin.

**La clôture ne fige rien pour le gestionnaire** : la régularisation reste possible ensuite, et la clôture elle-même
se déplace (`PUT`) ou se rouvre (`DELETE`). Ne pas griser la régularisation sur un élément clôturé.

### Évaluer le relevé des heures

```
GET /api/feuilles-de-temps/{operateurId}?annee=2026&semaine=20&evaluation=2026-05-11T20:59:59Z
GET /api/syntheses-des-heures/{operateurId}?annee=2026&semaine=20&evaluation=2026-05-11T20:59:59Z
```

Les deux lectures acceptent un instant ISO-8601 facultatif et rendent l'instant effectivement utilisé dans
`evaluation`. Chaque lecture relève l'heure du serveur une seule fois : elle fournit l'instant par défaut et
vérifie la borne future. Cet instant d'évaluation gouverne l'expiration et les jours atteints par les activités
en cours. Passer le même instant à la feuille et à la synthèse assure la même
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

### Totaux dans la synthèse

Les quatre totaux — `jours[].dureeOperationnelle`, `dureeOperationnelleTotale`, `elements[].duree` et
`elements[].dureeNonConformite` — sont des objets :

```json
{ "valeur": "PT2H" }
```

`valeur` est toujours présente. La NC reste comprise dans le total ; sa part
séparée ne dépend que des NC. Une activité en cours ne contribue pas à la durée.

Le journal brut `jours[].pointages[]` porte `id`, `type`, `dateDeSurvenue`, `element` et `poste`. Son ordre est l'heure
métier, puis la fin avant l'ouverture, puis l'identité ; il ne suit jamais l'ordre de réception.
Une `FIN` pointée à l'échéance ou après est ignorée : la feuille garde les 13 h complètes et l'anomalie automatique.

## 4. Erreurs

Toutes les erreurs métier sont des `ProblemDetail` (RFC 7807) portant un `type`, un `title`, un `status` et une
propriété `message` lisible.

Le `type` est un **code stable** de la forme `urn:glm:erreur:<contexte>:<code>` — c'est **lui** qu'on teste pour
brancher, jamais le couple `status` + `title` : le titre est une phrase française qui se reformule, et le même titre
sort de plusieurs contextes. Le catalogue complet est dans [documentation/codes-erreur.md](codes-erreur.md).

Deux statuts du tableau ci-dessous n'en portent pas : le **400** de Bean Validation, qui se lit par son `errors`
(`Map<champ, message>`), et le **403**, qui vient de la chaîne de filtres sans corps du tout.

| Statut | Cas                                                                                                                                                                    |
| ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400    | Corps invalide (Bean Validation) — détail par champ dans `errors` — ou date de survenue future.                                                                        |
| 403    | Jeton sans entreprise connue, ou rôle insuffisant.                                                                                                                     |
| 404    | Suivi, opérateur, poste, élément de fabrication, activité visée (régularisation) ou fin automatique (dossier) introuvable.                                             |
| 409    | Élément déjà engagé, élément clôturé, **pointage ignoré** (`pointage-ignore`), événement antérieur à l'engagement, refus de la régularisation, **saisie concurrente**. |

Un pointage qui ne s'accorde pas à l'état de sa clé n'est pas une erreur à afficher : le serveur l'ignore (409
`pointage-ignore`, voir [Un pointage est jugé à sa réception](#un-pointage-est-jugé-à-sa-réception)) et l'audite.

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
  vérifiée sur les deux écritures du journal. Retirer une habilitation ferme donc aussi la porte au rattrapage des
  saisies passées sur ce poste.
- **Ni un opérateur ni un poste ayant un fait historique d’activité ne se supprime** : `DELETE /api/operateurs/{id}` et
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
modifier ce partage sur un autre élément. Les tarifs viennent du fait ouvrant, figés par atelier.

Les champs de durée et de montant des lignes et du rapport sont des objets `{ "valeur": ... }`, y compris zéro. Un taux absent produit zéro, indépendamment du diviseur.

Chaque pointage de
la ligne porte ses `anomalies` (`FIN_AUTOMATIQUE` ou aucune), sa `fin` et ses `parts`, dont le `diviseur` est
toujours connu.

Cette route ne prend pas de paramètre d'évaluation et ne garantit pas un instantané face aux écritures concurrentes.

## Anomalies de pointage

Une **anomalie de pointage** est une **fin automatique** : une activité que rien n'a terminée avant son échéance. C'est
la seule chose que le gestionnaire traite. Un pointage incohérent n'en crée aucune : le serveur l'ignore à la réception
(voir [Un pointage est jugé à sa réception](#un-pointage-est-jugé-à-sa-réception)).

Ces routes sont décrites dans le [contrat OpenAPI généré](openapi.json) et éprouvées par les scénarios REST.

| Capacité       | Route                                                             | Droit                    |
| -------------- | ----------------------------------------------------------------- | ------------------------ |
| Liste paginée  | `GET /api/atelier/anomalies?operateur=…&element=…&page=0&size=20` | `USER` ou `GESTIONNAIRE` |
| Dossier        | `GET /api/atelier/suivis/{suivi}/anomalies/{pointage}`            | `USER` ou `GESTIONNAIRE` |
| Régularisation | `POST /api/atelier/suivis/{suivi}/regularisations`                | `GESTIONNAIRE`           |

La liste n'a ni paramètre `nature` ni discriminant. Il n'y a ni conflit, ni aperçu, ni confirmation, ni reçu : la
régularisation est un seul appel (voir [Écran back-office](#écran-back-office-rôle-gestionnaire)).

### Liste des fins automatiques

`GET /api/atelier/anomalies` rend une page `Page` de `RestFinAutomatiqueEnListe` : `content`, `currentPage` (à partir de
0), `pageSize` et `totalElementsCount`. `operateur` et `element` cherchent du texte partiel sans casse, y compris dans
les identifiants, et se combinent avant `page` et `size` ; `%`, `_` et `\` restent littéraux. Le tri porte sur `debut`,
puis le suivi, puis l'ouvrant.

Une fin automatique est lue dans `activite_d_atelier`, sans rejouer aucun journal : sans fin réelle (`fin` nulle), avec
une `echeance` inférieure ou égale à l'instant de lecture, borne comprise et à la nanoseconde (les instants sont des
décimaux exacts, `:evaluation` est converti de la même façon). L'instant de lecture est celui de l'horloge du service, lu
une seule fois pour toute la page ; la route ne prend pas de paramètre d'évaluation. Une fin pointée à l'échéance ou
après est ignorée : l'activité reste listée tant que le gestionnaire ne l'a pas régularisée. Une activité en cours, ou
terminée — fin réelle, fin régularisée, clôture avant l'échéance —, n'y figure pas. Une fin automatique n'est jamais
stockée.

Une ligne porte :

- `adresse` : le `suivi` et le `pointage` qui ouvre l'activité, qui adressent le dossier ;
- `activite` : l'identité de l'activité, que vise la régularisation. C'est l'identifiant de son pointage ouvrant, donc
  la même valeur que `adresse.pointage` ;
- `revision` du suivi, `elementId` et `designation`, `operateurId` et `operateur` (fiche, absente si inconnue),
  `posteId` et `poste` facultatifs ;
- `debut` et `echeance`, instants exacts à la nanoseconde. Aucune durée n'est calculée ni exposée.

```json
{
  "content": [
    {
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
  "currentPage": 0,
  "pageSize": 20,
  "totalElementsCount": 1
}
```

### Le dossier d'une fin automatique

L'adresse d'un dossier est le couple suivi/pointage, où le pointage est celui qui ouvre l'activité. Le dossier répond
**200** pour une fin automatique non régularisée, et **404** `fin-automatique-introuvable` pour tout autre pointage :
activité en cours, déjà terminée, déjà régularisée, ou pointage qui n'ouvre rien. Un 404 ramène à la liste, sans écran
intermédiaire. Un suivi absent de l'entreprise courante répond 404 `suivi-d-atelier-introuvable`.

Il ne contient que ce qui sert à régulariser :

- `adresse`, `revision` (celle du suivi évalué), `evaluation`, `elementId` et `designation` ;
- `activite` : l'activité échue — `evenement` et `activite` (le même identifiant), `operateurId` et `operateur`, `posteId`
  et `poste`, `categorie`, `debut`, `fin` (l'échéance) et `duree` (`PT13H`) ;
- `pointages` : les pointages du suivi qui portent la clé de l'activité, du plus ancien au plus récent ;
- `borneDeFin` : le plus tôt du début suivant sur la clé (opérateur et poste) et de la clôture, absent quand rien ne
  borne la fin. L'instant présent borne toujours la fin.

```json
{
  "adresse": { "suivi": "3e1d8181-…", "pointage": "ab8f8dba-…" },
  "revision": 0,
  "evaluation": "2026-05-10T22:00:00Z",
  "elementId": "0abc06ce-…",
  "designation": "OF M24-0655",
  "activite": {
    "evenement": "ab8f8dba-…",
    "activite": "ab8f8dba-…",
    "operateurId": "33333333-…",
    "posteId": "55555555-…",
    "categorie": "TRAVAIL",
    "debut": "2026-05-10T08:00:00Z",
    "fin": "2026-05-10T21:00:00Z",
    "duree": "PT13H"
  },
  "pointages": [{ "id": "ab8f8dba-…", "type": "DEBUT", "dateDeSurvenue": "2026-05-10T08:00:00Z", "…": "…" }],
  "borneDeFin": "2026-05-11T07:00:00Z"
}
```

L'échéance se juge à l'instant d'évaluation, à la nanoseconde, et n'est jamais stockée : liste et dossier la jugent
chacun au leur. Pour un début à 08:00, l'adresse lue à 20:59:59.999999999 n'a pas de dossier (404) ; lue à 21:00:00, elle
en a un.

Les pointages portent leurs identifiants bruts d'opérateur et de poste indépendamment de la résolution des fiches,
l'activité qu'ils ouvrent (absente pour une fin), leur auteur, leurs dates de survenue et d'enregistrement et leur origine
(`estUneRegularisation`). Le poste absent est distinct d'une fiche absente pour un poste identifié. Les neuf décimales
d'un instant sont conservées.

Le détail adressé lit le journal, la clôture et la révision d'une même version committée du suivi, sans verrouiller les
rédacteurs. Une écriture concurrente peut rendre cette version ancienne après sa lecture ; l'écriture contrôle toujours la
révision sous verrou, et `saisie-concurrente` dit de relire le dossier. Cette garantie ne constitue pas un instantané
entre plusieurs appels ni avec les libellés du référentiel. Voir
[l'ADR 0007](adr/0007-read-addressed-workshop-aggregates-coherently.md).

La révision commence à zéro à l'engagement et progresse à chaque modification effective du journal ou de la clôture, par
toutes les routes, pointages du pupitre compris. Un renvoi ou un pointage ignoré ne la fait pas progresser. La valeur
Java est `RevisionDuSuivi`, séparée des identités de faits et du nombre d'événements.

### La régularisation

Le corps, les refus et l'idempotence sont décrits dans [Écran back-office](#écran-back-office-rôle-gestionnaire). La fin se
place après le début de l'activité (exclu) et jusqu'à `borneDeFin` (inclus), sans dépasser l'instant présent. Une fois
régularisée, l'activité n'a plus de dossier (404) et quitte la liste ; un 409 `saisie-concurrente` dit de relire le
dossier.
