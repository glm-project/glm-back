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

`SynthesesDesHeuresService` est la fabrique : elle demande les journées qui **recouvrent** la semaine, replie chacune
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

**Ce cas n'est aujourd'hui pas atteignable par l'API réelle.** `atelier` rejoue et valide **tout** le journal à
chaque écriture, y compris une annulation — vérifié en pratique : annuler une pause qui laisserait une reprise
orpheline est refusé par `atelier` lui-même (`409 transition-de-presence-interdite`), avant même d'atteindre ce
contexte. La résilience reste une défense en profondeur légitime (données migrées, évolution future de la validation
d'`atelier`, accès direct à la base) et reste vérifiée par les tests unitaires du domaine, qui construisent
l'incohérence directement sans passer par `atelier` — mais `src/test/features/synthese_des_heures.feature` ne porte
aucun scénario pour ce cas, faute de moyen de le déclencher via l'API.

## Ports sortants

`PresenceDeLOperateur`, `OperateursConnus`, `FuseauHoraireDeLEntreprise`, implémentés par
`infrastructure/secondary` sur les mêmes tables que `feuilledetemps` (`evenement_de_presence`, `journee_de_travail`,
`operateur`) — troisième lecteur de ces tables après `atelier` (propriétaire) et `feuilledetemps`.

## Les adapters ne peuvent porter ni le nom de ceux d'atelier, ni ceux de feuilledetemps

Même contrainte que documentée côté `feuilledetemps` : Spring nomme un bean d'après le nom **simple** de sa classe,
et Hibernate enregistre une entité JPA sous son nom simple par défaut — deux classes homonymes dans des packages
différents refusent de démarrer ensemble (`ConflictingBeanDefinitionException`, ou collision de nom d'entité JPA).
`feuilledetemps` a déjà pris `ReferentielDesOperateurs`, `FuseauHoraireFixe`, `JourneesDeTravailDAtelier` et leurs
entités `*LectureEntity` ; `coutderevient` a pris ses `*ValoriseEntity`. Ce contexte prend donc son propre
vocabulaire, distinct des deux : `OperateursDeLaSynthese`, `FuseauHoraireDeLaSynthese`,
`JourneesDeTravailPourLaSynthese`, entités `*SyntheseEntity`. Un quatrième lecteur des mêmes tables devra choisir un
quatrième nom.

## État d'avancement

Les quatre couches sont livrées : `domain`, `infrastructure/secondary` (adapters JPA en lecture seule),
`application` (`SynthesesDesHeuresApplicationService`, ouvert à `USER` et `GESTIONNAIRE` — ce relevé affiche du
temps, pas un montant, rien ne justifie de le réserver au gestionnaire comme `coutderevient`) et
`infrastructure/primary` (`GET /api/syntheses-des-heures/{operateurId}?annee=&semaine=`).

`infrastructure/secondary` n'a **aucun test dédié** — ni unitaire, ni d'intégration : c'est le patron déjà suivi par
`feuilledetemps` et `coutderevient`, leur correction étant vérifiée par le scénario Cucumber qui traverse toute la
pile (écriture côté `atelier`, lecture par ce contexte). `src/test/features/synthese_des_heures.feature` tient ce
rôle ici, sur le même principe que `feuille_de_temps.feature` : il pointe par l'API d'`atelier` et relit par celle
de ce contexte, donc échoue dès que les deux cessent de lire les mêmes colonnes.

**Piège de nommage Cucumber rencontré** : le glue Cucumber est scanné depuis la racine `com.glm.glmback` — un même
texte de step défini dans deux classes lève une erreur de démarrage (`DuplicateStepDefinition`) qui fait échouer
**toute** la suite, pas seulement ce fichier. `feuilledetemps` avait déjà pris « `{string} est arrive a {string}` »
et « `{string} a pointe {string} a {string}` » ; ce contexte a dû inventer son propre phrasé (« pointe son arrivee
a », « enregistre le pointage ... a »), sur le même principe que `CoutDeRevientSteps` (« prend son poste a »,
« pointe sa presence ... a »). Un texte de step générique se choisit donc en vérifiant d'abord qu'aucune autre
classe de `src/test/java` ne le porte déjà.
