# Bounded context `syntheseheures`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Relever les heures travaillées d'un opérateur, semaine par semaine, pour alimenter la paie.** Un seul acte : lire
le journal de présence d'un opérateur sur une semaine ISO donnée, jour par jour — le journal brut des pointages
(arrivée, pause, reprise, départ), et la durée travaillée qui en découle.

C'est une **projection transverse**, comme `feuilledetemps` et `coutderevient` : un contexte purement lecteur, qui
ne possède aucune table, n'écrit rien, et recalcule tout à chaque appel.

## Ce dont il ne s'occupe pas

- **Le pointage et sa correction** — arrivée, pause, départ, régularisation, annulation appartiennent à `atelier`.
  Ce contexte ne propose aucune écriture.
- **La feuille de paie, ou tout calcul de paie.** Ce contexte relève des heures ; il ne dit pas ce qui est payé, à
  quel taux, ni ce qui compte comme heure supplémentaire. « Synthèse des heures », pas « rapport de paie » — le nom
  compte.
- **Le catalogue transverse des anomalies de journal et l'écran récapitulatif du gestionnaire.** Une anomalie de
  transition est une propriété du **journal**, pas d'une semaine ni d'un rapport demandé. `atelier` possède déjà
  l'écran de correction (régularisation, annulation, correction de présence) — c'est lui qui portera ce catalogue,
  pas ce contexte.
- **La valorisation** — ni taux horaire, ni coût. Il affiche du temps, il ne le multiplie par rien.
- **Le référentiel** — identité de l'opérateur lue par port, jamais possédée.

## Agrégat de lecture

`SyntheseDesHeures` : un opérateur résolu, une `SemaineCalendaire`, sept `JourDeSynthese`. Aucune identité, aucune
persistance — l'objet naît et meurt dans l'appel.

Chaque `JourDeSynthese` porte ses `Pointage` (un `EvenementDePresence` et un booléen `valide`) et sa `duree`
(`java.time.Duration`, somme des fenêtres de présence closes de ce jour, pauses exclues).

`SyntheseDesHeuresService` est la fabrique : elle demande les journées qui **recouvrent** la semaine, replie chacune
en pointages classés et en fenêtres de présence via `JourneeDeTravail`, puis ramène le tout aux jours du calendrier
avec `DecoupageCalendaire`.

## Invariants à ne pas casser

- **Le journal reste la source de vérité.** La présence se rejoue toujours depuis `evenement_de_presence`.
- **Les sept jours sont toujours rendus**, vides compris.
- **Un pointage fautif ne bloque jamais la lecture.** `JourneeDeTravail.pointages()` rend **tous** les pointages,
  valides ou non ; `JourneeDeTravail.fenetres()` n'utilise que les valides. `JourDeSynthese.aUneAnomalie()` et
  `SyntheseDesHeures.aUneAnomalie()` sont des méthodes **dérivées** des `Pointage` — jamais un champ stocké à part,
  qui pourrait diverger de ce qu'ils portent réellement.
- **Une fenêtre encore ouverte ne contribue rien à la durée.** Sans départ pointé et sans horloge dans ce contexte,
  aucune extrapolation jusqu'à « maintenant » n'est possible — transposition de la règle que `feuilledetemps`
  applique déjà à l'affichage d'une plage ouverte.
- **La semaine est toujours explicite.** Aucune « semaine courante » implicite, donc **aucune horloge** dans ce
  contexte : deux appels identiques rendent toujours la même chose.
- **Aucun import de `atelier`, `feuilledetemps`, `operateur` ni `postedetravail`**, tous annotés `@BusinessContext`.
  Ce contexte déclare ses propres entités JPA `@Immutable` sur leurs tables.

## Une divergence assumée par rapport à `feuilledetemps`

`feuilledetemps.JourneeDeTravail` lève `TransitionDePresenceInterditeException` sur une transition impossible : le
repli échoue plutôt que de mentir sur la présence affichée. Ici, la même situation ne doit **jamais** empêcher la
génération du relevé — le gestionnaire doit pouvoir voir le pointage fautif pour le corriger côté `atelier`, pas
recevoir une erreur 500. `JourneeDeTravail` de ce contexte n'a donc **pas** cette exception : son repli est total,
il classe chaque pointage valide/invalide au lieu d'échouer dessus.

C'est le même automate dupliqué deux fois (`feuilledetemps` et ce contexte), avec des comportements différents sur
le même cas limite. Rien n'oblige les deux implémentations à rester identiques passé la frontière de contexte —
c'est même tout l'intérêt de la frontière : chacune répond au besoin de son propre rapport.

## Ports sortants

`PresenceDeLOperateur`, `OperateursConnus`, `FuseauHoraireDeLEntreprise`.

## État d'avancement

Domaine seul livré pour l'instant (`syntheseheures/domain`, entièrement testé). `application`, `infrastructure`
(adapters JPA en lecture seule, contrôleur REST, OpenAPI) et le scénario Cucumber qui tiendrait ce repli aligné avec
celui d'`atelier` restent à faire — sur le patron de `feuilledetemps`.
