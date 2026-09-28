# Bounded context `feuilledetemps`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Ramener le temps de l'atelier au calendrier de l'entreprise**, et rien d'autre. Un seul acte : lire l'historique
d'un opérateur sur une semaine ISO donnée, jour par jour — sa présence, et son travail élément par élément.

C'est la première **projection transverse** du projet : un contexte purement lecteur, qui ne possède aucune table,
n'écrit rien, et recalcule tout à chaque appel.

## Ce dont il ne s'occupe pas

- **Le pointage et sa correction** — arrivée, départ, régularisation, annulation appartiennent à `atelier`.
  Ce contexte ne propose aucune écriture.
- **La valorisation** — ni taux horaire, ni coût horaire, ni temps réparti. Il affiche du temps, il ne le
  multiplie par rien. Le coût de revient sera un autre contexte lecteur, sur la même couture.
- **Le référentiel** — identité de l'opérateur lue par port, jamais possédée. Ni le libellé du poste ni la fiche de
  l'élément ne sont lus : la feuille ne rend que leurs identifiants, et la synthèse des heures porte leurs libellés.
- **Les durées** — la feuille expose des périodes ; `syntheseheures` expose les durées, qu'il calcule de son côté.
- **La paie** — il expose la présence découpée par jour, il ne choisit pas ce qui compte.

## Agrégat de lecture

`FeuilleDeTemps` : un opérateur résolu, une `SemaineCalendaire`, sept `JourDeLaSemaine`. Aucune identité, aucune
persistance — l'objet naît et meurt dans l'appel. Chaque jour porte sa `presence` (des `Plage`) et ses `activites`
(des `IntervalleDActivite` : une `Activite` — élément, poste, nature, catégorie — et sa `Plage`).

`FeuillesDeTempsService` est la fabrique : elle demande les journées qui **recouvrent** la semaine, les lit à
l'instant présent (fin présumée comprise), les replie en fenêtres de présence, puis passe chaque fenêtre au
`DecoupageCalendaire`, seul détenteur du fuseau horaire. Le travail suit le même chemin : les `SuiviDuTravail` de
l'opérateur sont repliés par leur `JournalDAtelier`, réduits par `ReductionALaPresence`, puis coupés par le même
découpage.

## Invariants à ne pas casser

- **Le journal reste la source de vérité.** Les colonnes `journee_de_travail.debut` et `.fin` ne servent qu'à borner
  la requête SQL ; la présence se rejoue toujours depuis `evenement_de_presence`.
- **Les sept jours sont toujours rendus**, vides compris. Un trou dans la liste obligerait le lecteur à deviner s'il
  manque une journée ou si l'opérateur n'était pas là.
- **Une plage ouverte ne dépasse pas son propre jour.** Sans départ pointé, rien ne dit que l'opérateur était encore
  là le lendemain ; l'étaler jusqu'à la fin de la semaine affirmerait une présence que personne n'a saisie. C'est la
  transposition de la règle qu'`atelier` applique déjà à un travail jamais arrêté.
- **Une journée abandonnée est fermée à sa fin présumée.** Au-delà du seuil (`SeuilDAmplitude`, table
  `parametrage`), sa dernière plage se ferme au dernier fait connu, pointage d'OF compris (`PointagesDAtelier`, table
  `evenement_d_atelier`, interrogé pour une journée abandonnée seulement), et porte `presumee`.
- **Une journée fermée plus de 24 h après son arrivée se lit comme abandonnée** (D13, issue #59) : `estPresumeePour`
  la ferme à sa fin présumée, le dernier fait **de la fenêtre de recherche**, départ exclu. Entre le seuil et 24 h,
  une journée fermée compte entière. 24 h est une borne physique, jamais un paramètre : la constante vit dans
  `JourneeDeTravail`, recopiée dans `atelier`, `feuilledetemps`, `syntheseheures` et `coutderevient`.
- **Le repli du travail rejoue l'automate d'atelier par poste**, l'opérateur étant fixé : un début sur une activité
  en cours la relance, une non-conformité ouvre une reprise, une fin sans activité est ignorée. Un intervalle court
  jusqu'au pointage suivant sur le même poste, sinon jusqu'à la clôture du suivi, sinon il reste ouvert. Les
  événements annulés sont écartés dès le SQL.
- **Le port rend tout le journal des suivis touchés**, restreint à l'opérateur : une non-conformité de la semaine peut
  suivre un début de la semaine d'avant. La période ne sert qu'à choisir les suivis ; elle part de la plus précoce des
  arrivées des journées lues, ou du lundi s'il est antérieur.
- **Un intervalle est réduit aux fenêtres de la journée où il a commencé**, lue comme la présence. La journée qui
  contient un instant est, comme dans `atelier`, la plus récente dont l'arrivée précède l'instant et que son départ
  pointé ne finit pas avant lui. Un départ referme ce qui n'a pas été arrêté ; une fenêtre présumée rend l'intervalle
  présumé ; une intersection réduite à un instant ne rend rien.
- **Un début hors de toute journée est écarté**, là où `TempsDAtelierService` le rend intact : sans présence, aucun jour
  ne peut l'accueillir sans arbitraire, et l'anomalie reste visible sur `GET /api/atelier/suivis/{id}/temps-effectif`.
  L'API d'`atelier` ouvre une arrivée implicite à chaque geste : le cas n'est atteignable qu'en unitaire.
- **Une activité ouverte ne rend que son jour de début**, comme une plage de présence ouverte.
- **La réduction, l'écart hors journée et le découpage changent avec `syntheseheures`**, qui les applique aux mêmes
  intervalles pour en tirer les durées : l'écran « Temps opérationnel » du front dessine les unes et additionne les
  autres. Les tableaux parallèles de `feuille_de_temps.feature` et `synthese_des_heures.feature` sont le filet.
- **Une activité porte l'élément, jamais le suivi** : un élément réengagé après clôture reste le même élément.
- **La semaine est toujours explicite.** Aucune « semaine courante » implicite. L'horloge ne sert qu'à juger
  l'abandon d'une journée : deux appels espacés peuvent donc différer.
- **Aucun import de `atelier`, `operateur` ni `postedetravail`**, tous annotés `@BusinessContext`. Ce contexte
  déclare ses propres entités JPA `@Immutable` sur leurs tables.

## La duplication du repli est assumée

`EtatDePresence`, `TypeDEvenementDePresence` et le repli en fenêtres de `JourneeDeTravail` sont une **seconde
implémentation** de ce qu'`atelier` fait déjà. `EtatDActivite`, `TypeDEvenementDAtelier` et `JournalDAtelier` en sont
une autre, celle du journal d'un élément : cinq automates d'atelier vivent désormais dans le projet (`atelier`,
`pupitre`, `coutderevient`, `feuilledetemps`, `syntheseheures`) et changent ensemble. C'est le prix de la
frontière : le partage passerait soit par un import interdit, soit par le shared kernel, qui est en anglais et ne peut
pas accueillir du vocabulaire d'atelier.

Deux filets tiennent les deux implémentations alignées :

- les tests unitaires de chaque côté, écrits sur les mêmes transitions ;
- `src/test/features/feuille_de_temps.feature`, qui **pointe par l'API d'atelier** et **relit par celle-ci**, donc
  échoue dès que les deux contextes cessent de lire les mêmes colonnes ou de rejouer le même automate.

Modifier l'automate d'un côté sans l'autre est un bug : le scénario Cucumber est là pour le dire.

## Les adapters ne peuvent pas porter le nom de ceux d'atelier

Spring nomme un bean d'après le nom **simple** de sa classe : deux `@Repository` nommés `OperateursDuReferentiel`
dans deux paquets différents refusent de démarrer (`ConflictingBeanDefinitionException`). Les adapters de ce
contexte prennent donc le préfixe `Referentiel...` là où l'atelier utilise `...DuReferentiel` — `ReferentielDesOperateurs`
ici. La contrainte ne vaut que pour les classes annotées : les records du domaine
(`Nom`, `OperateurId`, `JourneeDeTravail`…) portent volontairement le même nom que leurs jumeaux d'atelier, puisque
c'est le même mot du langage métier.

Hibernate enregistre de même chaque entité sous son nom simple : ce contexte a pris `TravailDeLaFeuilleDeTemps`,
`SuiviDeLaFeuilleDeTempsEntity` et `PointageDAtelierDeLaFeuilleDeTempsEntity`, dont les colonnes reprennent une à une
le nommage des entités propriétaires d'`atelier`.

## Ports sortants

`PresenceDeLOperateur`, `OperateursConnus`, `FuseauHoraireDeLEntreprise`, `SeuilDAmplitude`, `PointagesDAtelier`,
`TravailDeLOperateur`, `Clock`.

`TravailDeLOperateur` est servi par trois requêtes, jamais une par suivi : les identifiants des suivis où l'opérateur a
pointé sur la période (index `ix_evenement_d_atelier_operateur`), ces suivis, puis leurs journaux triés par date et
identifiant.

`FuseauHoraireDeLEntreprise` est une **donnée de paramétrage**, donc un port : `FuseauHoraireFixe` rend
`Europe/Paris` pour l'instant, sur le patron assumé d'`InMemoryPrefixesDElementsDeFabrication`. Le jour où une
entreprise cliente vit ailleurs, seul l'adapter change.

## État d'avancement

Lot 1 livré : la **présence**, semaine par semaine et jour par jour, jusqu'à `GET
/api/feuilles-de-temps/{operateurId}?annee={}&semaine={}`.

Lot 2 livré : le **travail par élément** — chaque jour rend ses `activites`, avec l'élément, le poste, la nature, la
catégorie et `presumee`, selon les invariants ci-dessus.
