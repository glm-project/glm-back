# Bounded context `coutderevient`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/CLAUDE.md](../../../../../../../CLAUDE.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Chiffrer le temps que l'atelier a capturé**, et rien d'autre. Un seul acte : lire le rapport d'un élément de
fabrication, une ligne par nature d'opération.

C'est la seconde **projection transverse** du projet, après `feuilledetemps` : un contexte purement lecteur, qui ne
possède aucune table, n'écrit rien, et recalcule tout à chaque appel.

Chaque ligne porte le temps de bon travail, le temps de reprise de non conformité avec ses périodes datées, et le
coût séparé en **machine** et **main d'œuvre**.

## Ce dont il ne s'occupe pas

- **La capture du temps** — engagement, pointage, régularisation, annulation, correction, clôture appartiennent à
  `atelier`. Ce contexte ne propose aucune écriture.
- **Le tarif** — le coût horaire du poste et le taux horaire de l'opérateur sont lus **tels que le journal les a
  figés à la saisie**, jamais relus depuis `postedetravail` ni `operateur`. Un tarif révisé depuis ne réécrit pas le
  prix d'heures déjà passées.
- **Le calendrier** — aucun `ZoneId`, aucun `LocalDate`. Ce contexte mesure des durées ; c'est `feuilledetemps` qui
  ramène des instants à des jours.
- **La paie** — il chiffre ce qu'un élément a coûté, pas ce qu'une personne doit toucher.

## Agrégat de lecture

`CoutDeRevient` : un élément résolu, ses lignes par nature, ses totaux. Aucune identité, aucune persistance —
l'objet naît et meurt dans l'appel.

`CoutsDeRevientService` est la fabrique. Six étapes, dans cet ordre :

1. résoudre l'élément (`ElementsValorisables`) ;
2. replier ses suivis en intervalles bruts (`TravailDeLElement`) ;
3. ramener chaque intervalle aux fenêtres de présence de **la journée où il a commencé**
   (`ReductionALaPresence`) ;
4. arrêter le temps à `Clock.now()` — un intervalle ouvert devient une `Periode` mesurable ;
5. déduire le diviseur de chaque opérateur de **tout** ce qu'il menait de front (`OccupationDesOperateurs`,
   `ChargesDesOperateurs`) ;
6. valoriser et grouper par nature (`CoutDeRevient.de`).

## Invariants à ne pas casser

- **Le diviseur compte des postes, jamais des éléments ni des activités.** Le client énonce la règle deux fois de
  suite : coût horaire de chaque machine active non divisé, taux horaire de l'opérateur divisé par le nombre de
  machines qu'il utilise. Un opérateur sur trois éléments avec **une seule** machine n'est donc **pas** divisé.
  Un pointage sans poste compte pour un poste.
- **Le découpage se fait aux bornes de tous les pointages de l'opérateur.** Un diviseur pris sur l'intervalle entier
  serait faux dès que deux pointages ne commencent pas ensemble ; `ChargeDeLOperateur` ne pose un diviseur que là où
  il est constant.
- **Les charges sont bâties sur l'union de ce qui est valorisé et de ce qui a été lu.** C'est ce qui garantit
  qu'aucune tranche de l'élément ne se retrouve sans diviseur, sans avoir à supposer que la lecture d'occupation la
  recouvre.
- **L'arrondi se fait à la ligne, une seule fois.** Les tranches se somment à l'échelle de travail (6 décimales), la
  ligne construit son `Montant` qui arrondit au centime, et le rapport totalise des lignes **déjà arrondies** — sans
  quoi l'écran afficherait un total qui n'est pas la somme de ce qu'il montre.
- **Le journal reste la source de vérité.** `suivi_d_atelier.etat` n'est jamais lu ; seule
  `cloture_date_de_survenue` sert, comme fermeture finale des activités que personne n'a arrêtées.
- **Un intervalle dont le début ne tombe dans aucune journée connue est rendu intact**, puis fermé à l'horloge :
  c'est la présence qui manque, et le domaine ne masque pas l'anomalie derrière un temps amputé. C'est la règle
  d'`atelier`, et non celle de `feuilledetemps`, qui écarte ce cas.
- **Aucun import de `atelier`, `elementdefabrication`, `operateur` ni `postedetravail`**, tous annotés
  `@BusinessContext`. Ce contexte déclare ses propres entités JPA `@Immutable` sur leurs tables.

## Ce contexte porte une horloge, contrairement à `feuilledetemps`

C'est l'écart assumé entre les deux projections. `feuilledetemps` s'interdit toute horloge pour que deux appels
identiques rendent la même chose ; ici, un travail non terminé n'a pas de durée, et le coût de revient d'un élément
en cours n'a de sens qu'arrêté à maintenant. Deux appels espacés sur un élément en cours ne rendent donc pas la même
chose — c'est écrit dans la description OpenAPI de la route.

## La duplication des deux replis est assumée

`JournalDAtelier` (repli des activités) et `JourneeDeTravail` / `EtatDePresence` (repli de la présence) sont
réécrits ici : le second pour la **troisième** fois, après `atelier` et `feuilledetemps`. C'est le prix de la
frontière — le partage passerait soit par un import interdit, soit par le shared kernel, qui est en anglais et ne
peut pas accueillir du vocabulaire d'atelier.

Deux filets tiennent les implémentations alignées :

- les tests unitaires de chaque côté, écrits sur les mêmes transitions ;
- `src/test/features/cout_de_revient.feature`, qui **pointe par l'API d'atelier** et **relit par celle-ci**, donc
  échoue dès que les contextes cessent de lire les mêmes colonnes.

## Deux pièges de nommage, tous deux rencontrés

- **Beans Spring** : Spring nomme un bean d'après le nom _simple_ de sa classe. Les adapters portent donc le suffixe
  `...Valorise` là où l'atelier et la feuille de temps ont déjà pris les autres noms.
- **Entités JPA** : Hibernate exige un nom d'entité unique dans toute l'unité de persistance — d'où
  `JourneeDeTravailValoriseeEntity` plutôt que `JourneeDeTravailLectureEntity`, déjà pris.
- **Nom logique de colonne** : deux entités qui donnent à la même colonne physique deux noms logiques différents —
  l'un implicite, l'autre explicite — **empêchent Hibernate de démarrer**
  (`Table [suivi_d_atelier] contains physical column name [...] referred to by multiple logical column names`). Les
  entités de ce contexte reprennent donc exactement le style de nommage de celles de l'atelier, colonne par colonne.

## Ports sortants

`ElementsValorisables`, `TravailDeLElement`, `OccupationDesOperateurs`, `PresenceDesOperateurs`, `Clock`.

`OccupationDesOperateurs` est celui qui n'a pas d'équivalent ailleurs : il rend ce que les opérateurs menaient de
front **tous éléments confondus**, ce qu'aucune lecture par élément ne pourrait donner. Sa borne est haute
seulement — toute activité qui recouvre l'élément a commencé avant sa fin, alors qu'une borne basse ferait
disparaître du diviseur une activité ouverte avant l'élément et fermée après lui.

## État d'avancement

Livré : le rapport d'un élément, jusqu'à `GET /api/couts-de-revient/{elementId}`, réservé au rôle `GESTIONNAIRE`.

Aucun changelog Liquibase : les index posés à cette fin existaient déjà
(`ix_evenement_d_atelier_operateur`, `ix_evenement_d_atelier_poste`, `ix_suivi_d_atelier_element`).

Points ouverts, listés en fin de section `coutderevient` dans `documentation/contexte-metier.md`.
