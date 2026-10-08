# Bounded context `atelier`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **pointage et sa correction**, et rien d'autre. Trois actes :

1. **Engager** un élément de fabrication en atelier — geste métier explicite du back-office, distinct de la création de
   l'élément — puis le **clôturer** ou rouvrir la clôture.
2. **Enregistrer les pointages d'activité** sur un élément engagé (début, non conformité, fin). Chaque pointage d'élément dit son intention — ouverture,
   transition ou fin — et la transition comme la fin visent l'activité qu'elles remplacent ou terminent. La pause
   n'est pas un pointage du serveur : le pupitre la traduit en fins, puis en ouvertures
   ([ADR 0002](../../../../../../../documentation/adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)).
3. **Corriger** ces saisies : `regularise` (saisie oubliée), `annule` (saisie en trop), `corrige` (saisie fausse).

Il interprète les faits du journal et projette les activités et les séquences en conflit à chaque écriture.
Les lectures évaluent l'expiration à un instant explicite ; le journal reste la source de vérité.

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

`SuiviDAtelier` porte un élément engagé et son `JournalDAtelier`. `TempsDAtelierService` lit les intervalles
interprétés de ses activités ; seuls les faits d'activité, la clôture et l'échéance en fixent les bornes.

Le parcours de gestion des anomalies de pointage est regroupé sous `gestionanomalies/` dans chaque couche d'Atelier :
`domain/gestionanomalies`, `application/gestionanomalies`, `infrastructure/primary/gestionanomalies` et
`infrastructure/secondary/gestionanomalies` portent les dossiers et la liste. Ces sous-packages appartiennent au même bounded context Atelier.

**Anomalie de pointage** : ce que le gestionnaire doit trancher. Elle porte une nature (`NatureDAnomalie`) :
`CONFLIT`, une séquence en conflit, et `FIN_AUTOMATIQUE`, une activité terminée à son échéance faute de fin réelle,
toutes deux listées par `GET /api/atelier/anomalies?nature=…` (réponse `oneOf` discriminée par `nature`). La liste des fins
automatiques se juge en SQL sur `activite_d_atelier` (sans fin, hors à résoudre, échéance atteinte, borne comprise), sans
rejouer de journal. `nature` est obligatoire : absente ou
inconnue, elle sort en 400 `urn:glm:erreur:atelier:nature-d-anomalie-invalide`. L'adresse d'un dossier est le couple
suivi/pointage, et son état `SANS_ANOMALIE` dit que l'ancre est active et ne porte aucune anomalie. Le vocabulaire de
l'interprétation ne change pas : « séquence en conflit », `SequenceEnConflit`, `conflits[]` de `RestSuiviDAtelier`,
`ConflitsDAtelier` et `atelier_conflits.feature` gardent leur nom, parce qu'ils décrivent la contradiction des
faits, pas le parcours qui la traite. L'ancien sens restreint d'« anomalie » — l'activité terminée automatiquement à
son échéance — est désormais la nature `FIN_AUTOMATIQUE`, portée par `finAutomatique`. Le coût de revient emploie
« anomalie » dans un sens voisin mais non identique (`AnomalieDuPointage` : ce qui rend un pointage suspect ou
incomplet) : seul `FIN_AUTOMATIQUE` y porte le même nom, `CONFLIT` correspond à `A_RESOUDRE` et `PARTAGE_INCONNU`
n'a pas d'équivalent ici. Aucun type n'est partagé entre les contextes. Les routes `/conflits` sont supprimées, sans
redirection.
**Dossier** : l'adresse d'une anomalie à traiter. Il couvre une séquence en conflit (`EN_CONFLIT`) ou une fin
automatique (`FIN_AUTOMATIQUE`) : sans séquence, ses activités concernées sont celle de l'ancre terminée
automatiquement, et son périmètre ses faits ouvrants et visants, gestes tardifs compris. L'ancre d'une fin automatique
est l'ouvrant actif ; l'activité visée par un acte reste l'`ActiviteId` d'origine. `finAutomatique` dit qu'une activité
concernée reste échue, même quand l'ancre est annulée ; la fin n'est jamais stockée.
L'agrégat, le journal, leurs transitions, le repository et les types communs d'interprétation, diagnostics compris,
restent dans les couches d'Atelier ; le parcours les utilise sans déplacer leurs invariants. Ses tests et fixtures
suivent leurs propriétaires dans les mêmes sous-packages.

## Invariants à ne pas casser

- **Le journal est la source de vérité.** L'agrégat se reconstruit par son repli ; les projections décrites ci-dessous
  sont réconciliées à chaque écriture. C'est la correction qui l'impose — une saisie rattrapée doit compter à l'heure
  où elle a eu lieu.
- **Horodatage bitemporel** sur chaque événement : date de survenue (métier) et date d'enregistrement (technique).
- **Une régularisation d'atelier se lit sur l'origine persistée de l'événement** (`OrigineDuPointage`) :
  `REGULARISATION` pour la régularisation et le remplaçant d'une correction, `POINTAGE` pour toute la route des
  pointages, même rejouée hors ligne avec l'heure du geste. C'est `SuivisDAtelierService` qui la fixe, d'après l'acte ;
  ni l'écart des dates ni l'auteur ne la déduisent.
- **Un événement annulé reste au journal**, porteur de son `Annulation`. Le repli l'écarte ; personne ne le supprime.
- **La pause n'existe pas pour le serveur.** Le pupitre la traduit en fins ciblées d'activité puis en nouvelles
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
  comme dans les réponses de pointage et de rejeu ; la persistance et la valorisation conservent les faits complets.
- **L'intention d'un pointage est explicite, jamais déduite de son type** (`IntentionDePointage`). Une ouverture crée
  une activité ; une transition remplace l'activité qu'elle vise par une activité distincte de l'autre catégorie ; une
  fin termine l'activité qu'elle vise. Seule une fin se pointe `FIN`, et seules la transition et la fin portent une
  activité visée : `EvenementDAtelier` refuse toute autre combinaison. Aucune intention par défaut.
- **Une activité s'identifie par son pointage ouvrant d'origine** (`ActiviteId`), que portent l'ouverture et la
  transition. Le remplaçant d'une correction d'un ouvrant reprend l'`ActiviteId` du fait corrigé
  (`EvenementDAtelier.enRemplacementDe`) : un geste vise une `ActiviteId`, jamais l'identifiant technique de l'ouvrant
  actif, et se résout sur l'ouvrant actif qui la porte.
- **Un geste ne touche que sa cible** (`SequenceDActivites`). Une cible absente de ce suivi est refusée
  (`ActiviteViseeIntrouvableException`, 404), celle d'un autre opérateur ou d'un autre poste aussi
  (`ActiviteViseeIncoherenteException`, 409), avant toute autre décision, absorption comprise. Un geste qui contredit
  le journal — cible déjà terminée, remplacée ou annulée, transition vers sa propre catégorie — n'est jamais refusé :
  il ne termine jamais une autre activité que sa cible, et sa séquence est en conflit (`SequenceEnConflit`).
  Corriger l'ouvrant vers un autre couple opérateur/poste conserve son `ActiviteId` : si un geste actif vise encore
  cette activité depuis l'ancienne clé, la correction est refusée (409). Annuler ou corriger d'abord ce geste permet
  ensuite de déplacer l'ouvrant sans laisser une cible incohérente et une durée chiffrable sur la nouvelle clé.
- **Aucune contradiction n'est refusée, par aucune écriture** : pointage, régularisation, correction et annulation
  enregistrent le fait, et l'interprétation rend une séquence en conflit au lieu de lever. Une exception levée par
  l'interprétation bloquerait aussi la relecture du suivi. Sont contradictoires : un geste qui vise une activité
  remplacée strictement avant lui, déjà terminée par une fin réelle — double appui compris —, pas encore ouverte à son
  heure ou dont l'ouverture est annulée ; une transition vers sa propre catégorie, vers une cible échue pendant qu'une
  autre activité est en cours, ou vers une cible que seul le gestionnaire a prolongée au-delà de son échéance. Une
  cible seulement échue ne contredit rien.
- **Une séquence en conflit est dérivée, jamais stockée comme état.** Elle ne dépend que de l'ensemble des faits
  actifs, jamais de leur ordre de réception, et disparaît au recalcul quand une correction ou une annulation rend les
  faits cohérents. Une contradiction couvre la zone qui va du début de sa cible à l'heure du geste : la cible,
  l'activité qu'ouvre le geste et toute activité de la clé qui chevauche la zone sont **à résoudre** ; les autres
  activités, de la clé comme des autres clés, gardent leur interprétation. Les contradictions qui partagent une
  activité, ou une même cible annulée, forment une seule séquence ; ses pointages sont ses gestes contradictoires et
  tout fait actif qui ouvre ou vise l'une de ses activités.
- **Une activité à résoudre n'est ni en cours ni terminée** (`Activite.aResoudre`) : sans fin, ni réelle ni
  automatique, que ni l'échéance ni la clôture ne lui donnent, et hors du temps effectif chiffré. L'état du suivi se
  juge sur ses seules activités interprétables ; le conflit se lit à part, sans valeur d'état propre.
- **Une fin sur un suivi clôturé se juge sur son heure métier** : survenue avant la clôture, elle est enregistrée et
  appliquée, la clôture restant acquise ; survenue après, elle est absorbée, une fois sa cible contrôlée.
- **Une activité que rien n'a terminée se termine automatiquement à son échéance** : son début plus 13 heures
  écoulées (`Echeance`), neutres au changement d'heure. Ce délai est la règle de l'atelier, pas une donnée de
  paramétrage : il reste une constante du domaine. Rien n'est écrit ni planifié : `Activite` ne dépend que des faits
  actifs, et seule sa lecture à un instant d'évaluation la dit en cours, terminée à sa fin réelle, ou
  terminée automatiquement à l'échéance avec une anomalie, que seule une fin réelle retire. Cette règle a trois
  lecteurs : le domaine (`Activite.a`), la supervision (`ActiviteDeSupervision.a`) et le SQL de la liste des fins
  automatiques, qui recopie la comparaison faute de pouvoir appeler `Echeance`. Leur parité est tenue par
  l'exécution, comme celle des critères de suivi : `ListeDesFinsAutomatiquesDAtelierIT` confronte ce SQL à
  `Activite.a` et à `AnomaliesDAtelierCriteria.matches`. Toute évolution de la règle les modifie ensemble. L'instant vient de
  l'horloge du service applicatif (`LectureDuSuivi`, `TempsDAtelierService.tempsEffectif`), jamais d'une horloge
  enfouie dans le domaine.
- **L'interprétation applique l'échéance sans instant de lecture**, sur les seules heures métier
  (`SequenceDActivites`). Un geste pointé au plus tard à l'échéance de sa cible la termine à son heure, un geste pile à
  l'échéance l'emportant sur la fin automatique. Pointée après, une fin conserve la borne automatique et son anomalie,
  sans conflit ni qualification supplémentaire dans le journal ; une transition ouvre sa nouvelle activité à son
  heure, en laissant un trou. Une relance ou une clôture ne prolonge jamais une
  activité échue. Seule une régularisation — fin ou transition — termine une activité au-delà de son échéance.
- **Une ouverture relance une activité interprétable sans bloquer le pupitre.** Avant son échéance, elle
  termine l’ancienne à l’heure du geste ; après, elle conserve sa fin automatique et le trou jusqu’à la nouvelle
  ouverture. Une transition de même catégorie conserve sa cible et met la séquence en conflit. Les lecteurs consomment les activités et conflits projetés par atelier avec leurs propres entités immuables ; chacun
  évalue l'échéance projetée à son instant de lecture et applique ses règles de calendrier ou de valorisation.
- **À heure métier égale, le journal range la fin, puis la transition, puis l'ouverture**, et départage enfin par
  l'identifiant : jamais par la date d'enregistrement, qui ferait dépendre le journal de l'ordre de réception.
- **L'habilitation, elle, bloque** : pointer sur un poste où l'opérateur n'est pas déclaré est refusé (409). C'est la
  seule règle dure du contexte. Elle ne joue que lorsqu'un poste est fourni, et elle joue sur les **trois** écritures
  du journal — pointage, régularisation, correction — sans quoi le back-office contournerait le pupitre.
- **Rien d'autre n'est copié du référentiel des ressources.** Le journal ne stocke qu'un identifiant, et les libellés
  sont relus à chaque lecture : une fiche corrigée doit s'afficher corrigée sur tout l'historique. La contrepartie vit
  chez les voisins — ni un opérateur ni un poste ayant servi à pointer ne se supprime.
- **Aucun import de `elementdefabrication`**, annoté `@BusinessContext`. L'atelier déclare sa propre identité
  `ElementEngage`, dont le nom et la catégorie sont **copiés à l'engagement**. La catégorie est une valeur
  libre (`CategorieDElement`), jamais une liste fermée : chaque entreprise nomme les siennes.
- **Domaine immuable** : toute transition rend un nouvel agrégat, suivie d'un `update` explicite sur le repository.

## Ports sortants

`SuiviDAtelierRepository`, `ElementsEngageables`, `OperateursConnus`, `PostesConnus`, `Habilitations`,
`IdentitesDEvenements`, `LecturesDeSupervision`, `Clock`.

`OperateursConnus.get` résout la fiche pour copier le taux horaire au fait ; `parIds` résout les libellés d'une page.
Le registre `IdentitesDEvenements` réserve durablement les UUID par entreprise, pupitre et hors pupitre compris.
L'association au suivi et la cible d'activité sont distinctes ; date absente et date fournie le restent.

La supervision utilise `LecturesDeSupervision` pour lire les projections à l'évaluation reçue du service applicatif.
Avant de modifier cette lecture, consulter son
[contrat dans atelier-api.md](../../../../../../../documentation/atelier-api.md#la-supervision-de-latelier-en-une-lecture-complète-rôles-user-et-gestionnaire) :
les anomalies après relance ou clôture et les conflits sans activité restent des données à rendre.

## État d'avancement

Les quatre couches existent. L'API REST est décrite par OpenAPI (`/swagger-ui.html`) et par
[documentation/atelier-api.md](../../../../../../../documentation/atelier-api.md), le guide d'intégration du développeur
front — le tenir à jour avec le contrat.

`infrastructure/secondary/` persiste en PostgreSQL, dans le schéma de l'entreprise courante :

- `JpaSuiviDAtelierRepository` écrit son agrégat sur deux tables — la
  ligne de l'agrégat et son journal —, l'agrégat étant reconstruit en entier par le domaine mais **rapproché par
  identifiant** côté persistance : un pointage coûte l'insertion d'une ligne, jamais la réécriture du journal. Le
  suivi y ajoute la projection de ses activités, `activite_d_atelier`, rapprochée de la même façon ;
- `ElementsDeFabricationEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à
  l'atelier : aucun import de `elementdefabrication`, l'invariant tient ;
- `OperateursDuReferentiel`, `PostesDeTravailDuReferentiel` et `HabilitationsDuReferentiel` lisent de la même façon
  `operateur`, `poste_de_travail` et `operateur_poste`. Le journal
  d'atelier résout la fiche entière pour recopier coût et taux horaires ; la lecture d'une page, elle, résout un
  journal entier par `parIds`, jamais une requête par événement, et `AnnuaireDAtelier` matérialise ce résultat le
  temps d'une lecture.

### Les colonnes de projection ne contredisent pas « le journal est la source de vérité »

Les projections d'activité et de conflit sont **dérivées du journal et écrites depuis le domaine à chaque
enregistrement**. `toDomain()` rejoue toujours le journal et les ignore pour reconstruire le suivi. Les requêtes et
les contextes lecteurs les lisent avec leurs propres entités ; l'expiration est évaluée séparément à la lecture.

`activite_d_atelier` porte, par activité de `SuiviDAtelier.activites()`, son identité, son ouvrant actif, sa clé, sa
nature, sa catégorie, son début, son échéance, sa fin réelle et sa fin au plus tard si elle est à résoudre, rapprochés par identité à
chaque écriture. Jamais de fin automatique ni d'anomalie, qui dépendent de l'instant : le filtre `etats` juge l'état à
l'instant d'évaluation de `SuiviDAtelierCriteria`, une activité interprétable sans fin réelle étant en cours tant que
son échéance n'est pas atteinte. `feuilledetemps`, `syntheseheures`, `coutderevient` et `pupitre` lisent cette
interprétation, avec leurs modèles et ports propres, sans import du domaine d'atelier.

La `fin_au_plus_tard` borne la plage possible d'une activité à résoudre : le maximum de son échéance et des fins
ou transitions régularisées qui la visent, limité par la clôture. La clôture ne prolonge jamais cette borne.
Elle vient des faits, sans horloge de lecture, et est réécrite à correction, annulation et clôture ; elle disparaît
quand la résolution rend l'activité interprétable. Ce n'est ni une fin réelle ni une durée.

Les séquences en conflit sont projetées dans `sequence_en_conflit` et leurs pointages dans
`pointage_en_conflit`. L'identité technique d'une séquence est celle de son premier pointage dans l'ordre du
journal ; elle n'est pas une identité métier exposée. Les positions conservent l'ordre des pointages et des
activités, ces dernières rattachées par `activite_d_atelier.sequence_id`. Une séquence sans activité à résoudre
reste projetée. Le rapprochement à chaque écriture retire les séquences résolues ; `toDomain()` les ignore.
`JpaSuiviDAtelierRepositoryIT` confronte leurs clés et leurs deux listes au domaine, puis vérifie réécriture et
résolution. Les contextes lecteurs peuvent les lire par leurs propres entités immuables.

Leur contrepartie : `SuiviDAtelierCriteria.matches` n’est plus appelée par la
production, qui traduit les mêmes règles en SQL. C'est `PariteDesRepositoriesDAtelierIT` qui rétablit par l'exécution
la garantie que donnait le code partagé — le modifier en même temps que l'une des deux expressions de la règle.
`ListeDesFinsAutomatiquesDAtelierIT` joue le même rôle pour la liste des fins automatiques, contre `Activite.a` et
`AnomaliesDAtelierCriteria.matches`.

### Concurrence

Les lectures adressées acquièrent le parent et son journal dans une même requête, dans leur propre
contexte JPA en lecture seule : un parent déjà chargé par un appelant peut sinon rester ancien malgré le fetch join.
Conserver cette frontière de lecture de données committées, décrite dans
[l’ADR 0007](../../../../../../../documentation/adr/0007-read-addressed-workshop-aggregates-coherently.md).

Toute écriture transporte la `RevisionDuSuivi` lue avec l'agrégat ; l'update compare cette révision sous
verrou pessimiste puis rend le suivi avec sa nouvelle révision. Garder ce retour pour toute écriture suivante :
les transitions immuables conservent la révision lue jusqu'à leur persistance. Le journal et la clôture partagent
la même révision, même si une annulation ou une clôture ne change aucun identifiant de fait.

Après la prise du verrou, rafraîchir la ligne et ses collections : une entité déjà chargée dans le contexte JPA
peut rester périmée malgré la requête verrouillée. `RevisionDuSuiviIT` synchronise deux transactions pour
éprouver ce cas. Une saisie périmée est refusée par `SaisieConcurrenteException` (409) ; seuls les pointages
Pupitre réessaient dans une nouvelle transaction en conservant leur UUID, intention et cible.

Un `@Version` posé seul ne protège pas le journal : sa collection d'événements est le côté inverse de
l'association, donc l'insertion d'un événement ne salit pas la ligne parente. La révision avance explicitement
dans la transaction qui réconcilie les faits et leurs projections. Un suivi inchangé garde sa révision ; le rejeu
strict d'un pointage et une fin absorbée ne modifient aucun fait.

L'`Auteur` d'une saisie vient toujours du jeton (`AuteurConnecte`), jamais du corps de la requête ; l'opérateur, lui,
reste dans le corps, sous forme d'identifiant. Les deux ne sont pas comparables tant que rien ne relie un utilisateur
authentifié à une fiche du référentiel : c'est pourquoi `estSaisiParUnTiers` a été retiré plutôt que rendu faux, et
pourquoi il reviendra avec le lot « utilisateur connecté ».

Deux scénarios métier de référence, à lire avant toute modification du modèle :

- `src/test/java/com/glm/glmback/atelier/domain/VieDeLAtelierTest.java` — une journée complète en appels directs, avec
  le verbatim client en javadoc de chaque assertion ;
- `src/test/features/atelier_suivi.feature` — la même journée rejouée en HTTP, avec `atelier_intentions.feature` pour l'intention et l'activité visée des pointages,
  `atelier_echeance.feature` pour l'échéance et la fin automatique des activités, et `atelier_conflits.feature` pour
  les séquences en conflit et leur résolution.

Les scénarios écrits avant l'intention la font déduire du journal par `EcrituresDuJournalDAtelier`, comme le ferait le
pupitre ; tout nouveau scénario donne son intention et sa cible.

Les scénarios pilotent l'horloge (`CucumberClock`, step `Given il est "..."`). Deux pièges s'y rappellent seuls :
**faire avancer l'horloge entre deux événements** — à horodatage identique, l'ordre du journal se départage sur
l'intention puis sur l'identifiant, donc au hasard entre deux gestes de même intention — et **ne jamais corriger vers
une date postérieure à l'instant courant**, que `Horodatage` refuse.

Les points encore ouverts sont listés en fin de section `atelier` dans `documentation/contexte-metier.md`.
