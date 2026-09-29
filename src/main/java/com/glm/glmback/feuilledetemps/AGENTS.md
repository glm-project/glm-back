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

`FeuillesDeTempsService` est la fabrique : elle lit les activités interprétées par atelier, les évalue à l'instant
courant relevé une seule fois, puis les coupe avec `DecoupageCalendaire`, seul détenteur du fuseau horaire. La présence
est encore lue séparément et n'intervient pas dans ces activités.

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
- **Atelier possède l'interprétation.** `ActivitesDeLOperateur` rend sa projection `activite_d_atelier`, lue par
  une entité propre `@Immutable` : aucun journal d'activité ni automate n'est rejoué ici.
- **La sélection se fait par recouvrement**, sur le début et la fin réelle ou l'échéance, jamais par un pointage
  de la semaine ou une arrivée. Aucune borne basse fixe du début : une régularisation peut établir plus de 13 h.
- **L'état est explicite** : `TERMINEE`, `TERMINEE_AUTOMATIQUEMENT`, `EN_COURS`, `A_RESOUDRE`. Une fin réelle
  est conservée, même au-delà de l'échéance ; à défaut, l'échéance atteinte termine automatiquement l'activité.
  Une activité à résoudre reste sans fin, et l'échéance ne résout pas son conflit.
- **Chaque portion conserve l'activité entière** : son identité stable, son état, son début et sa fin éventuelle.
  La fin n'existe que pour une activité terminée ou terminée automatiquement. La portion est coupée aux minuits et
  aux limites de la semaine, sans déplacer ces bornes entières.
- **Une activité en cours rend une indication sans fin sur chaque jour atteint à l'instant de lecture**,
  dans la semaine. Son début entier permet de lire « en cours depuis dimanche » sur lundi, sans durée à compter.
- **Une activité porte l'élément, jamais le suivi** : un élément réengagé après clôture reste le même élément.
- **La semaine est toujours explicite.** Aucune « semaine courante » implicite. L'horloge sert à évaluer
  l'expiration des activités et l'abandon d'une journée : deux appels espacés peuvent donc différer.
- **Aucun import de `atelier`, `operateur` ni `postedetravail`**, tous annotés `@BusinessContext`. Ce contexte
  déclare ses propres entités JPA `@Immutable` sur leurs tables.

## La couture avec atelier

`src/test/features/feuille_de_temps.feature` écrit réellement par l'API d'atelier puis lit la feuille : relances,
transitions ciblées, fins reçues tardivement, régularisations, corrections, annulations et clôtures doivent restituer
les faits projetés par leur propriétaire. Le calendrier est prouvé par le service et `DecoupageCalendaireTest`.
La présence conserve pour l'instant son repli propre.

## Les adapters ne peuvent pas porter le nom de ceux d'atelier

Spring nomme un bean d'après le nom **simple** de sa classe : deux `@Repository` nommés `OperateursDuReferentiel`
dans deux paquets différents refusent de démarrer (`ConflictingBeanDefinitionException`). Les adapters de ce
contexte prennent donc le préfixe `Referentiel...` là où l'atelier utilise `...DuReferentiel` — `ReferentielDesOperateurs`
ici. La contrainte ne vaut que pour les classes annotées : les records du domaine
(`Nom`, `OperateurId`, `JourneeDeTravail`…) portent volontairement le même nom que leurs jumeaux d'atelier, puisque
c'est le même mot du langage métier.

Hibernate enregistre de même chaque entité sous son nom simple : ce contexte a pris `ActivitesDeLaFeuilleDeTemps`,
`ActiviteDeLaFeuilleDeTempsEntity` et `SuiviDeLaFeuilleDeTempsEntity`, dont les colonnes reprennent une à une
le nommage des entités propriétaires d'`atelier`.

## Ports sortants

`PresenceDeLOperateur`, `OperateursConnus`, `FuseauHoraireDeLEntreprise`, `SeuilDAmplitude`, `PointagesDAtelier`,
`ActivitesDeLOperateur`, `Clock`.

`ActivitesDeLOperateur` est servi par une requête qui joint les activités à leur suivi pour lire l'identifiant
d'élément. Le tri final porte sur le début de portion, l'élément puis l'identité stable de l'activité.

`FuseauHoraireDeLEntreprise` est une **donnée de paramétrage**, donc un port : `FuseauHoraireFixe` rend
`Europe/Paris` pour l'instant, sur le patron assumé d'`InMemoryPrefixesDElementsDeFabrication`. Le jour où une
entreprise cliente vit ailleurs, seul l'adapter change.

## État d'avancement

Lot 1 livré : la **présence**, semaine par semaine et jour par jour, jusqu'à `GET
/api/feuilles-de-temps/{operateurId}?annee={}&semaine={}`.

Lot 2 livré : le **travail par élément** — chaque jour rend ses `activites`, avec l'élément, le poste, la nature, la
catégorie selon les invariants ci-dessus.
