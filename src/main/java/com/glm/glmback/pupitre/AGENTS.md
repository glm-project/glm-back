# Bounded context `pupitre`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Alimenter le cache local du poste d'atelier, pour qu'il continue à collecter sans réseau.** Un seul acte : rendre,
en un appel et dans une transaction unique, tout ce que le pupitre doit garder sur disque — les opérateurs
désignables avec leurs habilitations, les éléments encore pointables avec leurs activités en cours — et la **date**
de cet instantané.

C'est une **projection transverse**, comme `feuilledetemps`, `coutderevient` et `syntheseheures` : un contexte
purement lecteur, qui ne possède aucune table, n'écrit rien, et recalcule tout à chaque appel.

## Ce dont il ne s'occupe pas

- **Le pointage lui-même et sa correction** : le pupitre écrit par l'API d'`atelier`, jamais par ici. Ce contexte ne
  propose aucune écriture, et n'en proposera pas — le chemin d'écriture idempotent existe déjà chez `atelier`
  (identifiants de geste créés au pupitre, rejeu à 200, registre `identite_evenement_atelier`).
- **La présence** — arrivée, pause, reprise, départ. Le pupitre n'a besoin d'aucune journée de travail pour afficher
  ses tuiles ni pour désigner un opérateur ; il ouvre la journée en pointant.
- **Le référentiel lui-même** : créer, modifier ou supprimer un opérateur, un poste ou un élément appartient à
  `operateur`, `postedetravail` et `elementdefabrication`.
- **La valorisation** — ni taux horaire d'opérateur, ni coût horaire de poste. Ces montants ne sont même pas mappés
  par les entités de lecture : un champ qu'on ne lit pas ne peut pas fuir par mégarde vers un écran d'atelier
  partagé, alors que `coutderevient` les réserve au gestionnaire.
- **L'identité du poste d'atelier et son enrôlement.** Le pupitre porte un compte d'appareil Keycloak et traverse la
  chaîne d'autorisation comme n'importe quel `USER` d'une entreprise ; rien de spécifique ne vit ici.

## Agrégat de lecture

`ReferentielDuPupitre` : un `genereLe`, une liste d'`OperateurDuPupitre`, une liste de `SuiviDuPupitre`. Aucune
identité, aucune persistance — l'objet naît et meurt dans l'appel.

`ReferentielsDuPupitreService` est la fabrique : elle assemble les deux collections et les date par le port `Clock`.

`JournalDuPupitre` porte le repli : les événements actifs d'un élément, groupés par `CleDActivite`, donnent les
activités encore ouvertes ; `SuiviDuPupitre.etat()` en déduit `EN_ATTENTE`, `EN_COURS` ou `INTERROMPU`.

## Invariants à ne pas casser

- **La réponse n'est jamais paginée.** La pagination est exactement ce qui empêche de prouver une version
  instantanée du référentiel : rien ne garantirait que deux pages viennent du même état de la base, et c'est le
  défaut que cette route existe pour supprimer. Le volume est borné par la taille de l'atelier. Ne pas « rétablir la
  cohérence » avec les autres lectures du projet en ajoutant une pagination ici.
- **L'instantané tient à l'isolation, pas à la transaction.** `ReferentielsDuPupitreApplicationService` demande
  `Isolation.REPEATABLE_READ` : sous `READ COMMITTED`, chaque requête prend son propre instantané, et la lecture des
  opérateurs pourrait ignorer un opérateur qu'une activité de la lecture suivante désigne.
- **`genereLe` vient du port `Clock`**, jamais d'un `Instant.now()` : c'est ce qui rend le scénario Cucumber capable
  de le figer. Cette date **change à chaque appel**, y compris quand rien n'a bougé — elle dit quand le serveur a
  produit la réponse, pas quand le référentiel a changé pour la dernière fois. Dater le dernier changement
  supposerait d'horodater les modifications d'`operateur`, `poste_de_travail` et `operateur_poste`, qui ne portent
  aucune colonne de modification.
- **Le journal reste la source de vérité.** L'état et les activités se déduisent du repli. La colonne de projection
  `suivi_d_atelier.etat` n'est même pas mappée : c'est `cloture_date_de_survenue` qui écarte les éléments clôturés,
  parce que la clôture est un fait et non une projection.
- **Un élément clôturé est absent**, et `EtatDuSuivi` ne porte donc pas de valeur `CLOTURE` : elle n'aurait aucun
  porteur.
- **Le nom de l'élément vient du suivi, sa référence du référentiel.** Le nom est copié à l'engagement — un élément
  renommé ne réécrit pas l'histoire de l'atelier — tandis que la référence est relue à chaque lecture, comme les
  identités d'opérateurs. Un élément supprimé du référentiel laisse donc sa tuile intacte, privée de sa seule
  référence.
- **Aucun import d'`atelier`, `operateur`, `postedetravail` ni `elementdefabrication`**, tous annotés
  `@BusinessContext`. Ce contexte déclare ses propres entités JPA `@Immutable` sur leurs tables.

## Une tolérance assumée, comme `syntheseheures`

`atelier` refuse un pointage que l'automate d'activité n'admet pas ; ici, un tel pointage est **ignoré**, sans
exception ni marqueur. Le cas n'est pas atteignable par l'API — `atelier` valide tout le journal à chaque écriture —
mais un écran d'atelier ne doit jamais s'éteindre parce qu'un journal est bizarre. La résilience reste une défense
en profondeur (donnée migrée, accès direct à la base), vérifiée par `JournalDuPupitreTest`, qui construit
l'incohérence directement.

C'est le **quatrième** rejeu du repli du journal d'atelier dans le projet, après `feuilledetemps` et
`coutderevient`. La duplication est assumée pour la même raison : le partage passerait soit par un import interdit,
soit par le shared kernel, qui est en anglais.

## Les adapters ne peuvent porter aucun nom déjà pris

Spring nomme un bean d'après le nom **simple** de sa classe, et Hibernate enregistre une entité JPA sous son nom
simple par défaut : deux classes homonymes dans des packages différents refusent de démarrer ensemble. `atelier` a
pris `OperateurConnuEntity`, `PosteConnuEntity`, `ElementEngageableEntity`, `SuiviDAtelierEntity` et
`EvenementDAtelierEntity` ; `feuilledetemps` ses `*LectureEntity`, `coutderevient` ses `*ValoriseEntity`,
`syntheseheures` ses `*SyntheseEntity`. Ce contexte prend `*DuPupitreEntity`, et ses adapters
`OperateursDuReferentielDuPupitre` / `SuivisOuvertsDuReferentielDuPupitre`.

Même règle côté Cucumber : le glue est scanné depuis la racine `com.glm.glmback`, et un même texte de step défini
dans deux classes fait échouer **toute** la suite. `PupitreSteps` porte donc son propre phrasé (« au pupitre, … »,
« … au referentiel du pupitre »).

## Ports sortants

`OperateursDuPupitre`, `SuivisOuvertsDuPupitre`, `Clock`.

Les deux premiers rendent une liste, sans critères ni pagination : c'est leur raison d'être. Les adapters lisent
`operateur`, `operateur_poste`, `poste_de_travail`, `suivi_d_atelier`, `evenement_d_atelier` et
`element_de_fabrication`, et **écartent les événements annulés dès le SQL** — les rapporter pour les filtrer ensuite
ferait porter au domaine une correction qui ne le regarde pas.

## État d'avancement

Les quatre couches sont livrées, pour la seule route `GET /api/pupitre/referentiel`, ouverte à `USER` et
`GESTIONNAIRE`.

`infrastructure/secondary` n'a **aucun test dédié**, comme chez `feuilledetemps`, `coutderevient` et
`syntheseheures` : sa correction est vérifiée par `src/test/features/pupitre_referentiel.feature`, qui engage,
pointe, annule et clôture par l'API d'`atelier` puis relit par celle-ci — il échoue dès que les deux contextes
cessent de lire les mêmes colonnes.

Ce que le front en attend, et ce qu'il en fait, est décrit dans
[documentation/atelier-api.md](../../../../../../../documentation/atelier-api.md), section « Le référentiel du
pupitre ».
