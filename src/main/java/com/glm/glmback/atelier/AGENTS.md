# Bounded context `atelier`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **pointage et la régularisation de la fin d'une activité**, et rien d'autre. Trois actes :

1. **Engager** un élément de fabrication en atelier — geste métier explicite du back-office, distinct de la création de
   l'élément — puis le **clôturer** ou rouvrir la clôture.
2. **Recevoir les pointages du pupitre** : `DEBUT`, `NON_CONFORMITE` et `FIN`. Un pointage ne désigne aucune activité.
   Le serveur le juge à son arrivée (voir « La règle de réception ») : il entre au journal, ou il part en audit. La
   pause n'est pas un pointage du serveur : le pupitre la traduit en fins, puis en ouvertures
   ([ADR 0002](../../../../../../../documentation/adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)).
3. **Régulariser la fin** d'une activité échue — le seul acte du gestionnaire sur le journal. Il n'existe ni correction
   ni annulation : un fait du journal n'est jamais modifié après coup.

Il interprète les faits du journal et projette les activités à chaque écriture. Les lectures évaluent l'expiration à un
instant explicite ; le journal reste la source de vérité.

## Ce dont il ne s'occupe pas

Ne rien ajouter ici qui relève de :

- **le calcul du coût de revient monétaire** — temps réparti valorisé, agrégation par élément ou par période. Le
  contexte capture, sans les calculer, le coût horaire du poste et le taux horaire de l'opérateur : copiés du
  référentiel sur chaque événement du journal, exactement comme la nature de l'opération, ils ne servent qu'à figer
  une valeur qui pourrait changer chez le voisin — aucune arithmétique ne les combine ici ;
- **le référentiel des ressources** — opérateur → postes autorisés, taux ; poste → libellé, nature, coût horaire. Ces
  données sont **lues par port** (`OperateursConnus`, `PostesConnus`, `Habilitations`), jamais possédées ici. Le
  journal ne retient que `OperateurId` et `PosteDeTravailId` ;
- **le cycle de vie de l'élément de fabrication** lui-même, qui appartient à `elementdefabrication` ;
- **le fuseau horaire et le jour calendaire** — les lecteurs découpent les activités ; aucun `ZoneId` ni `LocalDate`
  dans ce contexte.

## Agrégat

`SuiviDAtelier` porte un élément engagé et son `JournalDAtelier`. Seuls les faits d'activité, la clôture et l'échéance
fixent les bornes de ses activités.

Le parcours de gestion des anomalies de pointage est regroupé sous `gestionanomalies/` dans chaque couche d'Atelier :
`domain/gestionanomalies`, `application/gestionanomalies`, `infrastructure/primary/gestionanomalies` et
`infrastructure/secondary/gestionanomalies` portent le dossier et la liste. Ces sous-packages appartiennent au même
bounded context et n'ont pas d'`AGENTS.md` propre : la section « Gestion des anomalies » ci-dessous les décrit.

## La règle de réception

Le serveur juge chaque pointage du pupitre par **clé** : l'opérateur, le suivi (l'OF) et le poste. `CleDActivite` porte
l'opérateur et le poste ; le suivi qui la contient complète la clé. Il y a au plus une activité en cours par clé ; un
opérateur qui mène plusieurs éléments ou plusieurs postes a une clé par élément et par poste, jugées à part. La règle
vit dans le domaine : `SuiviDAtelier.juge` appelle `RegleDeReception`, qui rend un `VerdictDeReception` — `Accepte`, ou
`Ignore` avec sa raison et le dernier pointage accepté auquel il a été comparé. Un pointage ignoré ne change pas
l'agrégat.

Les contrôles existants viennent d'abord, dans `SuivisDAtelierService.pointe` : corps invalide, opérateur ou poste
introuvable, habilitation, clôture. Un `DEBUT` ou une `NON_CONFORMITE` sur un suivi clôturé est refusé
(409 `suivi-d-atelier-cloture`), seul refus que le pupitre affiche à l'opérateur. Un `DEBUT` ou une `NON_CONFORMITE`
antérieur à l'engagement reste refusé (409 `evenement-anterieur-a-l-engagement`) ; une `FIN` antérieure à l'engagement
n'a rien à fermer et part en audit.

Ensuite, dans cet ordre :

1. **`ANTERIEUR`** : l'heure du geste est strictement plus ancienne que celle du dernier pointage accepté de la clé,
   régularisations comprises. Une heure égale passe. Une `FIN` qui n'est pas postérieure au début de l'activité en cours
   qu'elle fermerait est aussi `ANTERIEUR` : **il n'existe pas d'activité de durée nulle**. Un geste composé reste
   accepté, car sa `FIN` à t ferme une activité ouverte avant t, puis l'ouverture part à t.
2. **L'échéance**, jugée sur l'heure du geste : atteinte quand elle est supérieure ou égale au début plus la durée
   maximale. Une activité échue compte comme terminée.
3. **Le tableau** :

| État de la clé                                              | `DEBUT`                  | `NON_CONFORMITE`         | `FIN`                                                                                                      |
| ----------------------------------------------------------- | ------------------------ | ------------------------ | ---------------------------------------------------------------------------------------------------------- |
| Rien en cours (jamais ouverte, terminée, clôturée ou échue) | accepté                  | accepté                  | ignoré : `APRES_ECHEANCE` si la dernière activité de la clé est échue et sans fin, sinon `AUCUNE_ACTIVITE` |
| Activité en cours (travail ou NC)                           | ignoré : `DEJA_EN_COURS` | ignoré : `DEJA_EN_COURS` | accepté, ferme l'activité en cours                                                                         |

Les quatre raisons (`RaisonDePointageIgnore`) sont `DEJA_EN_COURS`, `AUCUNE_ACTIVITE`, `APRES_ECHEANCE` et `ANTERIEUR`.
La clôture ferme les activités : une `FIN` postérieure à la clôture est ignorée (`AUCUNE_ACTIVITE`). Une `FIN` survenue
avant la clôture et reçue après elle est acceptée à son heure.

Un pointage est jugé à son arrivée : premier arrivé, premier servi. Rien n'est jamais rejugé.

**Le pointage ignoré n'entre pas au journal.** `PointagesIgnores` l'écrit dans la table d'audit
`pointage_ignore_d_atelier` : une ligne par pointage ignoré (identifiant, suivi, opérateur, poste, type, heure du geste,
heure de réception, raison, identifiant du dernier pointage accepté comparé). Elle se consulte en base, sans endpoint ni
écran. **Cette table n'a ni clé ni contrainte** — pas de clé primaire, pas de clé étrangère, aucune colonne obligatoire —,
seulement un index sur l'identifiant : deux lignes pour un même renvoi sont acceptées, et rien ne doit empêcher un
pointage ignoré de s'écrire. `PointageIgnoreEntity` ne déclare un `@Id` que parce que JPA l'exige ; elle n'est jamais
relue, seulement écrite.

Le refus est la réponse au pupitre : 409 `urn:glm:erreur:atelier:pointage-ignore` (`PointageIgnoreException`). Il est
levé par `SuivisDAtelierApplicationService.pointeDuPupitre` **après** la validation de la transaction qui a écrit
l'audit : le lever dedans l'aurait annulée, audit compris. Un pointage ignoré prend, comme tout pointage, le verrou du
suivi (`getForUpdate`) dès l'entrée du jugement.

**Idempotence**, avant toute règle :

1. l'identifiant est dans `evenement_d_atelier` — toute la table, le journal de n'importe quel suivi — : réponse 200,
   rien n'est écrit, quel que soit le contenu renvoyé ;
2. sinon, pour un pointage, il est dans l'audit : même refus `pointage-ignore`, sans nouvelle ligne ;
3. sinon le pointage est jugé.

La régularisation applique la première étape seule. Il n'y a pas de registre d'identités ni de refus pour un
identifiant réutilisé avec un autre contenu : le premier arrivé fait foi.

## Le journal et sa lecture

**Le journal est la source de vérité.** L'agrégat se reconstruit par son repli ; les projections d'activités sont
réconciliées à chaque écriture. C'est l'insertion rétroactive qui l'impose — une fin pointée hors ligne doit compter à
l'heure où elle a eu lieu.

Le journal se **trie par heure du geste, la `FIN` avant l'ouverture à heure égale, puis par identifiant** — jamais par
la date d'enregistrement, qui ferait dépendre le journal de l'ordre de réception. Cet ordre est sans ambiguïté parce que
le journal ne porte jamais une fin et l'ouverture qu'elle fermerait à la même heure : la règle de réception ignore une
`FIN` du pupitre à l'heure du début de son activité (`ANTERIEUR`), et la régularisation refuse une fin qui n'est pas
postérieure à ce début (`fin-avant-debut`). Cette garantie vient de ces deux contrôles, pas du journal, qui ne refuse
rien.

Le journal se **lit par clé** (`SequenceDActivites`, la seule interprétation) : les faits d'une clé, dans l'ordre du
journal, donnent ses activités. Un `DEBUT` ou une `NON_CONFORMITE` ouvre une activité, qui s'identifie par son pointage
ouvrant (`ActiviteId`). Une `FIN` du pupitre ferme l'activité en cours de sa clé, sans la désigner. La `FIN` d'une
régularisation ferme l'activité qu'elle cible. La clôture ferme l'activité restée en cours. La lecture suppose au plus
une activité en cours par clé, ce que la règle de réception garantit ; un fait qui n'a rien à fermer n'a aucun effet, et
rien n'est refusé à la lecture.

`EvenementDAtelier` impose son invariant : seul un `DEBUT` ou une `NON_CONFORMITE` porte une activité, et **seule la
`FIN` d'une régularisation porte une cible** (`activiteVisee`).

## L'échéance

Une activité que rien n'a terminée se termine automatiquement à son échéance : son début plus la durée maximale
d'activité, 13 heures écoulées (`Echeance`), neutres au changement d'heure. La durée vient du noyau partagé
`shared/activityduration` (`MaximumActivityDuration`), que lit aussi le référentiel du pupitre (`dureeMaximaleDActivite`,
`"PT13H"`) : le contexte `pupitre` n'importe pas `atelier`. `MaximumActivityDuration.standard()` rend la valeur de
l'atelier en attendant le port de paramétrage ; la durée est strictement positive. Le domaine ne code aucun autre 13.

L'échéance est **atteinte** quand l'instant est supérieur ou égal au début plus la durée. Rien n'est écrit ni planifié :
`Activite` ne dépend que des faits, et seule sa lecture à un instant d'évaluation (`Activite.a`) la dit en cours,
terminée à sa fin réelle, ou terminée automatiquement à l'échéance. Cette fin automatique est l'anomalie que le
gestionnaire régularise ; seule une fin réelle la retire. L'instant vient de l'horloge du service applicatif
(`LectureDuSuivi`), jamais d'une horloge enfouie dans le domaine.

La règle a trois lecteurs : le domaine (`Activite.a`), la supervision (`ActiviteDeSupervision.a`) et le SQL de la liste
des fins automatiques, qui recopie la comparaison faute de pouvoir appeler `Echeance`. Leur parité est tenue par
l'exécution : `ListeDesFinsAutomatiquesDAtelierIT` confronte ce SQL à `Activite.a`. Toute évolution de la règle les
modifie ensemble.

La même échéance gouverne la lecture du journal, sur les seules heures métier : un fait dont l'heure atteint l'échéance
de l'activité en cours la trouve déjà échue. Un `DEBUT` pile à l'échéance est accepté et laisse à l'activité précédente
sa fin automatique, jamais une fin réelle. Une clôture ne prolonge jamais une activité échue. Seule une régularisation
établit une fin réelle au-delà de l'échéance.

## Gestion des anomalies

Une **anomalie de pointage** est la **fin automatique** : une activité terminée à son échéance faute de fin réelle. Il
n'y a plus d'autre nature, ni conflit, ni discriminant. Elle n'est jamais stockée.

**La liste** (`GET /api/atelier/anomalies`) est une `RestPage<RestFinAutomatiqueEnListe>`, sans paramètre `nature`.
Elle se juge en SQL sur `activite_d_atelier` (`JpaFinsAutomatiquesDAtelier`) : sans fin réelle, échéance atteinte à
l'instant de la lecture, borne comprise, sans rejouer de journal. Le tri porte sur le début, puis le suivi et l'ouvrant.
Une ligne porte l'adresse du dossier, l'activité, son début et son échéance, sans durée.

**L'adresse d'un dossier** est le couple suivi/pointage, où le pointage est celui qui ouvre l'activité (`ActiviteId` et
identifiant de l'ouvrant sont aujourd'hui la même valeur).

**Le dossier** (`GET /api/atelier/suivis/{id}/anomalies/{pointage}`, `LectureDossierAnomalie`) répond 200 pour une fin
automatique non régularisée, et 404 `fin-automatique-introuvable` sinon : le front revient à la liste, sans écran
intermédiaire. Il porte :

- l'adresse, la révision, l'instant d'évaluation, et l'élément concerné (`elementId`, `designation`, comme la ligne de la
  liste) ;
- l'activité échue ;
- les pointages de sa clé, du plus ancien au plus récent ;
- `borneDeFin`, le plus tôt du début suivant sur la clé et de la clôture, ou rien.

**La régularisation directe** (`POST /api/atelier/suivis/{id}/regularisations`, réservée au `GESTIONNAIRE`) a pour corps
`{id, activite, dateDeSurvenue}`. L'`id` est généré une fois par saisie, par le client. L'opérateur, le poste et le type
(`FIN`) se déduisent de l'activité ; la nature, le coût et le taux horaires sont figés comme pour un pointage.
**Elle ne passe pas par la règle de réception**, et son heure peut dépasser l'échéance. Elle s'écrit avec l'origine
`REGULARISATION`. Elle répond 201, ou 200 pour un renvoi (identifiant déjà dans `evenement_d_atelier`, avant toute règle).
`SuiviDAtelier.exigeUneFinRegularisable` vérifie, dans cet ordre :

- `activite-visee-introuvable` (404) : aucun pointage de ce suivi n'a ouvert cette activité ;
- `activite-deja-regularisee` (409) : une régularisation vise déjà l'activité ;
- `activite-non-echue` (409) : l'activité n'est pas une fin automatique, échéance non atteinte ou terminée par un
  pointage. Ce refus n'est pas définitif tant que l'échéance n'est pas atteinte ;
- `date-de-survenue-future` (400) : l'heure dépasse maintenant ;
- `fin-avant-debut` (409) : l'heure n'est pas postérieure au début de l'activité — aucune activité n'a une durée nulle ;
- `fin-apres-borne` (409) : l'heure dépasse la borne, le début suivant sur la clé ou la clôture.

L'habilitation est ensuite vérifiée comme pour un pointage (409). `saisie-concurrente` (409) dit qu'un pointage s'est
glissé entre la lecture et l'écriture : le front relit le dossier.

## Invariants à ne pas casser

- **Horodatage bitemporel** sur chaque événement : date de survenue (métier) et date d'enregistrement (technique).
- **Une régularisation se lit sur l'origine persistée de l'événement** (`OrigineDuPointage`) : `REGULARISATION` pour la
  fin du gestionnaire, `POINTAGE` pour toute la route des pointages, même rejouée hors ligne avec l'heure du geste.
  C'est `SuivisDAtelierService` qui la fixe, d'après l'acte ; ni l'écart des dates ni l'auteur ne la déduisent.
- **Le journal n'est jamais réécrit.** Aucun événement n'est annulé, corrigé ni supprimé ; un pointage ignoré n'y entre
  pas.
- **La pause n'existe pas pour le serveur.** Le pupitre la traduit en une `FIN` par activité, puis en nouvelles
  ouvertures ; une activité oubliée se termine à son échéance ou à la fin régularisée par le gestionnaire.
- **Le poste de travail et la `NatureDOperation` sont toujours facultatifs.** L'application vise un maximum
  d'entreprises clientes ; celles qui n'ont ni parc machine ni métiers distincts laissent les deux vides et retrouvent
  un comportement cohérent, pas un cas dégradé.
- **La nature ne bloque rien**, et elle vient **du poste**, jamais de la personne. Elle n'est qu'un axe d'agrégation
  pour la synthèse ; avec le coût horaire du poste et le taux horaire de l'opérateur, c'est tout ce que le journal
  copie du référentiel, pour qu'un poste requalifié ou un tarif révisé ne réécrivent pas les heures déjà passées.
- **Le coût horaire du poste et le taux horaire de l'opérateur sont copiés au moment de la saisie**, sur le même
  patron que la nature : jamais relus depuis le référentiel après coup. Ils restent, comme la nature, entièrement
  facultatifs, et ne servent qu'à figer une valeur qui pourrait changer chez le voisin — le calcul lui-même n'entre
  pas dans ce contexte.
- **Les tarifs du journal restent réservés au `GESTIONNAIRE`.** La politique applicative
  `HourlyRatesAuthorization` retire coût horaire et taux horaire des projections REST `USER`, en lecture
  comme dans les réponses de pointage ; la persistance et la valorisation conservent les faits complets.
- **L'habilitation, elle, bloque** : pointer ou régulariser sur un poste où l'opérateur n'est pas déclaré est refusé
  (409). Elle ne joue que lorsqu'un poste est fourni, et elle joue sur les **deux** écritures du journal — pointage et
  régularisation —, sans quoi le back-office contournerait le pupitre.
- **Rien d'autre n'est copié du référentiel des ressources.** Le journal ne stocke qu'un identifiant, et les libellés
  sont relus à chaque lecture : une fiche corrigée doit s'afficher corrigée sur tout l'historique. La contrepartie vit
  chez les voisins — ni un opérateur ni un poste ayant servi à pointer ne se supprime.
- **Aucun import de `elementdefabrication`**, annoté `@BusinessContext`. L'atelier déclare sa propre identité
  `ElementEngage`, dont le nom et la catégorie sont **copiés à l'engagement**. La catégorie est une valeur
  libre (`CategorieDElement`), jamais une liste fermée : chaque entreprise nomme les siennes.
- **Domaine immuable** : toute transition rend un nouvel agrégat, suivie d'un `update` explicite sur le repository.

## Ports sortants

`SuiviDAtelierRepository`, `ElementsEngageables`, `OperateursConnus`, `PostesConnus`, `Habilitations`,
`PointagesIgnores`, `LecturesDeSupervision`, `Clock`.

`OperateursConnus.get` résout la fiche pour copier le taux horaire au fait ; `parIds` résout les libellés d'une page.
`SuiviDAtelierRepository.contientEvenement` répond sur toute la table des événements ; `getForUpdate` prend le verrou du
suivi pour juger un pointage.

La supervision utilise `LecturesDeSupervision` pour lire les projections à l'évaluation reçue du service applicatif.
Avant de modifier cette lecture, consulter son
[contrat dans atelier-api.md](../../../../../../../documentation/atelier-api.md#la-supervision-de-latelier-en-une-lecture-complète-rôles-user-et-gestionnaire) :
une fin automatique reste une donnée à rendre après une reprise ou une clôture.

## État d'avancement

Les quatre couches existent. L'API REST est décrite par OpenAPI (`/swagger-ui.html`) et par
[documentation/atelier-api.md](../../../../../../../documentation/atelier-api.md), le guide d'intégration du développeur
front — le tenir à jour avec le contrat.

`infrastructure/secondary/` persiste en PostgreSQL, dans le schéma de l'entreprise courante :

- `JpaSuiviDAtelierRepository` écrit son agrégat sur deux tables — la
  ligne de l'agrégat et son journal —, l'agrégat étant reconstruit en entier par le domaine mais **rapproché par
  identifiant** côté persistance : un pointage coûte l'insertion d'une ligne, jamais la réécriture du journal. Le
  suivi y ajoute la projection de ses activités, `activite_d_atelier`, rapprochée de la même façon ;
- `JpaPointagesIgnores` écrit l'audit `pointage_ignore_d_atelier` et dit si un identifiant y figure ;
  `JpaPointagesIgnoresIT` en épingle les colonnes ;
- `ElementsDeFabricationEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à
  l'atelier : aucun import de `elementdefabrication`, l'invariant tient ;
- `OperateursDuReferentiel`, `PostesDeTravailDuReferentiel` et `HabilitationsDuReferentiel` lisent de la même façon
  `operateur`, `poste_de_travail` et `operateur_poste`. Le journal
  d'atelier résout la fiche entière pour recopier coût et taux horaires ; la lecture d'une page, elle, résout un
  journal entier par `parIds`, jamais une requête par événement, et `AnnuaireDAtelier` matérialise ce résultat le
  temps d'une lecture.

### Les colonnes de projection ne contredisent pas « le journal est la source de vérité »

Les projections d'activité sont **dérivées du journal et écrites depuis le domaine à chaque enregistrement**.
`toDomain()` rejoue toujours le journal et les ignore pour reconstruire le suivi. Les requêtes et les contextes lecteurs
les lisent avec leurs propres entités ; l'expiration est évaluée séparément à la lecture.

`activite_d_atelier` porte, par activité de `SuiviDAtelier.activites()`, son identité, son ouvrant, sa clé, sa nature,
sa catégorie, son début, son échéance et sa fin réelle, rapprochés par identité à chaque écriture. Jamais de fin
automatique ni d'anomalie, qui dépendent de l'instant : le filtre `etats` juge l'état à l'instant d'évaluation de
`SuiviDAtelierCriteria`, une activité sans fin réelle étant en cours tant que son échéance n'est pas atteinte.
`feuilledetemps`, `syntheseheures`, `coutderevient` et `pupitre` lisent cette projection, avec leurs modèles et ports
propres, sans import du domaine d'atelier.

Leur contrepartie : `SuiviDAtelierCriteria.matches` n'est plus appelée par la production, qui traduit les mêmes règles
en SQL. C'est `PariteDesRepositoriesDAtelierIT` qui rétablit par l'exécution la garantie que donnait le code partagé —
le modifier en même temps que l'une des deux expressions de la règle. `ListeDesFinsAutomatiquesDAtelierIT` joue le même
rôle pour la liste des fins automatiques, contre `Activite.a`.

### Concurrence

Les lectures adressées acquièrent le parent et son journal dans une même requête, dans leur propre
contexte JPA en lecture seule : un parent déjà chargé par un appelant peut sinon rester ancien malgré le fetch join.
Conserver cette frontière de lecture de données committées, décrite dans
[l'ADR 0007](../../../../../../../documentation/adr/0007-read-addressed-workshop-aggregates-coherently.md).

Toute écriture transporte la `RevisionDuSuivi` lue avec l'agrégat ; l'update compare cette révision sous
verrou pessimiste puis rend le suivi avec sa nouvelle révision. Garder ce retour pour toute écriture suivante :
les transitions immuables conservent la révision lue jusqu'à leur persistance. Le journal et la clôture partagent
la même révision.

Après la prise du verrou, rafraîchir la ligne et ses collections : une entité déjà chargée dans le contexte JPA
peut rester périmée malgré la requête verrouillée. `RevisionDuSuiviIT` synchronise deux transactions pour
éprouver ce cas. Une saisie périmée est refusée par `SaisieConcurrenteException` (409) ; seuls les pointages
du pupitre sont rejoués par le serveur, dans une nouvelle transaction, en conservant leur UUID
(`SaisieConcurrenteRejouee`).

Un `@Version` posé seul ne protège pas le journal : sa collection d'événements est le côté inverse de
l'association, donc l'insertion d'un événement ne salit pas la ligne parente. La révision avance explicitement
dans la transaction qui réconcilie les faits et leurs projections. Un suivi inchangé garde sa révision ; un renvoi et un
pointage ignoré ne modifient aucun fait du journal.

L'`Auteur` d'une saisie vient toujours du jeton (`AuteurConnecte`), jamais du corps de la requête ; l'opérateur, lui,
reste dans le corps, sous forme d'identifiant. Les deux ne sont pas comparables tant que rien ne relie un utilisateur
authentifié à une fiche du référentiel : c'est pourquoi `estSaisiParUnTiers` a été retiré plutôt que rendu faux, et
pourquoi il reviendra avec le lot « utilisateur connecté ».

### Tests de référence

Deux scénarios métier de référence, à lire avant toute modification du modèle :

- `src/test/java/com/glm/glmback/atelier/domain/VieDeLAtelierTest.java` — une journée complète en appels directs, avec
  le verbatim client en javadoc de chaque assertion ;
- `src/test/features/atelier_suivi.feature` — la même journée rejouée en HTTP.

La règle de réception est prouvée par `atelier_reception.feature` (les huit situations de la page de règle, plus la `FIN`
pile à l'échéance, la `FIN` à l'heure du début, les clés jugées à part, la clôture et la lecture de la table d'audit en
SQL), `SuiviDAtelierReceptionTest` (l'agrégat) et `SuivisDAtelierReceptionTest` (le service). L'idempotence l'est par
`atelier_rejeu.feature`, l'échéance par `atelier_echeance.feature`, la régularisation et le dossier par
`atelier_regularisation.feature` et `atelier_anomalies_*.feature`.

Le glue `EcrituresDuJournalDAtelier` n'en déduit rien : le corps d'un pointage part tel que le scénario le donne. Un
scénario qui passe du travail à la non conformité pointe une `FIN`, puis la non conformité, à la même heure. Les
pointages de mise en place doivent être acceptés ; un tableau dit `reponse = ignore` pour ceux que la règle ignore.

Les scénarios pilotent l'horloge (`CucumberClock`, step `Given il est "..."`). Un piège s'y rappelle seul : **un
pointage dont l'heure précède celle du dernier accepté de sa clé est ignoré** (`ANTERIEUR`) — poser les pointages de
mise en place dans l'ordre des heures.

Les points encore ouverts sont listés en fin de section `atelier` dans `documentation/contexte-metier.md`.
