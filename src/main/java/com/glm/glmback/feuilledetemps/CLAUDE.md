# Bounded context `feuilledetemps`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/CLAUDE.md](../../../../../../../CLAUDE.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Ramener le temps de l'atelier au calendrier de l'entreprise**, et rien d'autre. Un seul acte : lire l'historique
d'un opérateur sur une semaine ISO donnée, jour par jour.

C'est la première **projection transverse** du projet : un contexte purement lecteur, qui ne possède aucune table,
n'écrit rien, et recalcule tout à chaque appel.

## Ce dont il ne s'occupe pas

- **Le pointage et sa correction** — arrivée, pause, départ, régularisation, annulation appartiennent à `atelier`.
  Ce contexte ne propose aucune écriture.
- **La valorisation** — ni taux horaire, ni coût horaire, ni temps réparti. Il affiche du temps, il ne le
  multiplie par rien. Le coût de revient sera un autre contexte lecteur, sur la même couture.
- **Le référentiel** — identité de l'opérateur lue par port, jamais possédée.
- **La paie** — il expose la présence découpée par jour, il ne choisit pas ce qui compte.

## Agrégat de lecture

`FeuilleDeTemps` : un opérateur résolu, une `SemaineCalendaire`, sept `JourDeLaSemaine`. Aucune identité, aucune
persistance — l'objet naît et meurt dans l'appel.

`FeuillesDeTempsService` est la fabrique : elle demande les journées qui **recouvrent** la semaine, les replie en
fenêtres de présence, puis passe chaque fenêtre au `DecoupageCalendaire`, seul détenteur du fuseau horaire.

## Invariants à ne pas casser

- **Le journal reste la source de vérité.** Les colonnes `journee_de_travail.debut` et `.fin` ne servent qu'à borner
  la requête SQL ; la présence se rejoue toujours depuis `evenement_de_presence`.
- **Les sept jours sont toujours rendus**, vides compris. Un trou dans la liste obligerait le lecteur à deviner s'il
  manque une journée ou si l'opérateur n'était pas là.
- **Une plage ouverte ne dépasse pas son propre jour.** Sans départ pointé, rien ne dit que l'opérateur était encore
  là le lendemain ; l'étaler jusqu'à la fin de la semaine affirmerait une présence que personne n'a saisie. C'est la
  transposition de la règle qu'`atelier` applique déjà à un travail jamais arrêté.
- **La semaine est toujours explicite.** Aucune « semaine courante » implicite, donc **aucune horloge** dans ce
  contexte : deux appels identiques rendent toujours la même chose.
- **Aucun import de `atelier`, `operateur` ni `postedetravail`**, tous annotés `@BusinessContext`. Ce contexte
  déclare ses propres entités JPA `@Immutable` sur leurs tables.

## La duplication du repli est assumée

`EtatDePresence`, `TypeDEvenementDePresence` et le repli en fenêtres de `JourneeDeTravail` sont une **seconde
implémentation** de ce qu'`atelier` fait déjà. C'est le prix de la frontière : le partage passerait soit par un
import interdit, soit par le shared kernel, qui est en anglais et ne peut pas accueillir du vocabulaire d'atelier.

Deux filets tiennent les deux implémentations alignées :

- les tests unitaires de chaque côté, écrits sur les mêmes transitions ;
- `src/test/features/feuille_de_temps.feature`, qui **pointe par l'API d'atelier** et **relit par celle-ci**, donc
  échoue dès que les deux contextes cessent de lire les mêmes colonnes.

Modifier l'automate d'un côté sans l'autre est un bug : le scénario Cucumber est là pour le dire.

## Les adapters ne peuvent pas porter le nom de ceux d'atelier

Spring nomme un bean d'après le nom **simple** de sa classe : deux `@Repository` nommés `OperateursDuReferentiel`
dans deux paquets différents refusent de démarrer (`ConflictingBeanDefinitionException`). Les adapters de ce
contexte prennent donc le préfixe `Referentiel...` là où l'atelier utilise `...DuReferentiel` — `ReferentielDesOperateurs`
ici, `ReferentielDesPostes` au lot 2. La contrainte ne vaut que pour les classes annotées : les records du domaine
(`Nom`, `OperateurId`, `JourneeDeTravail`…) portent volontairement le même nom que leurs jumeaux d'atelier, puisque
c'est le même mot du langage métier.

## Ports sortants

`PresenceDeLOperateur`, `OperateursConnus`, `FuseauHoraireDeLEntreprise`.

`FuseauHoraireDeLEntreprise` est une **donnée de paramétrage**, donc un port : `FuseauHoraireFixe` rend
`Europe/Paris` pour l'instant, sur le patron assumé d'`InMemoryPrefixesDElementsDeFabrication`. Le jour où une
entreprise cliente vit ailleurs, seul l'adapter change.

## État d'avancement

Lot 1 livré : la **présence**, semaine par semaine et jour par jour, jusqu'à `GET
/api/feuilles-de-temps/{operateurId}?annee={}&semaine={}`.

Lot 2 à faire : le **travail par élément** — les intervalles d'activité réduits aux fenêtres de présence, découpés
par jour de la même façon, avec l'élément, le poste et la nature. Décision déjà prise pour ce lot : un pointage dont
le début ne tombe dans aucune journée de présence est **écarté**, là où `TempsDAtelierService` le rend intact.
Ce contexte répond à « qu'a fait cette personne cette semaine » — sans présence, aucun jour ne peut l'accueillir
sans arbitraire, et l'anomalie reste visible sur `GET /api/atelier/suivis/{id}/temps-effectif`.
