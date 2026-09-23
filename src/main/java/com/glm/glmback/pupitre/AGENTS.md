# Bounded context `pupitre`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Alimenter le cache local du poste d'atelier, pour qu'il continue à collecter sans réseau.** Un seul acte : rendre,
en un appel et dans une transaction unique, tout ce que le pupitre doit garder sur disque — les opérateurs
désignables avec leurs habilitations et leur **état de présence courant**, les éléments encore pointables avec leurs
activités en cours — et la **date** de cet instantané.

C'est une **projection transverse**, comme `feuilledetemps`, `coutderevient` et `syntheseheures` : un contexte
purement lecteur, qui ne possède aucune table, n'écrit rien, et recalcule tout à chaque appel.

## Ce dont il ne s'occupe pas

- **Le pointage lui-même et sa correction** : le pupitre écrit par l'API d'`atelier`, jamais par ici. Ce contexte ne
  propose aucune écriture, et n'en proposera pas — le chemin d'écriture idempotent existe déjà chez `atelier`
  (identifiants de geste créés au pupitre, rejeu à 200, registre `identite_evenement_atelier`).
- **L'écriture de la présence** — arrivée, pause, reprise, départ s'écrivent par l'API d'`atelier`. Ce contexte en
  **lit** l'état courant, et rien d'autre : ni l'instant du dernier événement — « en pause depuis 10 h 12 »
  supposerait de replier le journal de présence de tous les opérateurs à chaque appel, et donnerait une seconde
  source de durée en désaccord visible avec celles que le pupitre fige déjà —, ni aucun marqueur d'idempotence, que
  le pupitre tient lui-même pour replier ses gestes locaux, comme il le fait des pointages.
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

`ReferentielsDuPupitreService` est la fabrique : elle relève les présences, les remet à la lecture des opérateurs,
assemble les deux collections et les date par le port `Clock`. C'est le seul endroit où les deux lectures se
rejoignent — l'adapter des opérateurs ne fait qu'interroger le relevé, il ne décide de rien.

`PresencesDesOperateurs` porte la règle de correspondance : qu'aucune journée en cours ne nomme est `ABSENT`.

`JournalDuPupitre` porte le repli : les événements actifs d'un élément, groupés par `CleDActivite`, donnent les
activités encore ouvertes ; `SuiviDuPupitre.etat()` en déduit `EN_ATTENTE`, `EN_COURS` ou `INTERROMPU`.

## Invariants à ne pas casser

- **La réponse n'est jamais paginée.** La pagination est exactement ce qui empêche de prouver une version
  instantanée du référentiel : rien ne garantirait que deux pages viennent du même état de la base, et c'est le
  défaut que cette route existe pour supprimer. Le volume est borné par la taille de l'atelier. Ne pas « rétablir la
  cohérence » avec les autres lectures du projet en ajoutant une pagination ici.
- **La réponse n'est pas un instantané, et c'est assumé.** `ReferentielsDuPupitreApplicationService` a demandé
  `Isolation.REPEATABLE_READ` de sa livraison à la correction de #36. Sous `READ COMMITTED`, chaque requête prend son
  propre instantané : la lecture des opérateurs peut ignorer un opérateur qu'une activité de la lecture suivante
  désigne. L'écart est réel, il se résorbe au rafraîchissement suivant du cache, et il est préféré au prix de la
  garantie — voir la javadoc de la méthode. En deux mots : Hibernate pose le schéma du tenant par
  `Connection.setSchema` dès l'acquisition de la connexion, ce qui prend un instantané, après quoi PostgreSQL refuse
  tout changement d'isolation ; la route répondait donc `500` **à chaque appel**. Seul l'ordre inverse fonctionne, et
  il exige un `MultiTenantConnectionProvider` maison dans le chemin qui garantit l'étanchéité entre entreprises
  clientes. **Ne pas remettre `isolation =` ici sans corriger d'abord cette chaîne** : `pupitre_referentiel.feature`
  rougirait aussitôt — c'est aujourd'hui le seul filet sur ce point.
- **`genereLe` vient du port `Clock`**, jamais d'un `Instant.now()` : c'est ce qui rend le scénario Cucumber capable
  de le figer. Cette date **change à chaque appel**, y compris quand rien n'a bougé — elle dit quand le serveur a
  produit la réponse, pas quand le référentiel a changé pour la dernière fois. Dater le dernier changement
  supposerait d'horodater les modifications d'`operateur`, `poste_de_travail` et `operateur_poste`, qui ne portent
  aucune colonne de modification.
- **Le journal reste la source de vérité.** L'état et les activités **d'un élément** se déduisent du repli. La
  colonne de projection `suivi_d_atelier.etat` n'est même pas mappée : c'est `cloture_date_de_survenue` qui écarte
  les éléments clôturés, parce que la clôture est un fait et non une projection.
- **L'état de présence, lui, se lit sur la projection `journee_de_travail.etat`**, et c'est la seule exception. Ce
  qu'on demande ici est l'état courant de **tous** les opérateurs à la fois : le replier supposerait de rapporter
  tous les journaux de présence ouverts à chaque synchronisation, pour n'en garder que la dernière valeur. La
  journée en cours est choisie comme l'atelier la choisit — la plus récemment commencée parmi celles dont l'état
  n'est pas `ABSENT`, **sans aucune borne de date** : une journée ouverte hier et jamais fermée est toujours la
  journée en cours. Le filet reste `pupitre_referentiel.feature`, qui pointe la présence par l'API d'`atelier`.
- **Un opérateur sans journée en cours n'est jamais omis.** La liste rend les opérateurs _désignables_, pas les
  opérateurs présents : son état vaut `ABSENT`, et il reste offert au pupitre.
- **Le relevé des présences est une requête, pas une par opérateur.** Il entre par le paramètre de
  `OperateursDuPupitre.tous`, de sorte que l'opérateur naisse complet — c'est cette forme qui rend impossible la
  lecture par opérateur, qu'aucun test ne rattraperait.
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
pris `OperateurConnuEntity`, `PosteConnuEntity`, `ElementEngageableEntity`, `SuiviDAtelierEntity`,
`JourneeDeTravailEntity` et `EvenementDAtelierEntity` ; `feuilledetemps` ses `*LectureEntity`, `coutderevient` ses
`*ValoriseEntity`, `syntheseheures` ses `*SyntheseEntity`. Ce contexte prend `*DuPupitreEntity`, et ses adapters
`OperateursDuReferentielDuPupitre` / `SuivisOuvertsDuReferentielDuPupitre` / `PresencesDuReferentielDuPupitre`.

Même règle côté Cucumber : le glue est scanné depuis la racine `com.glm.glmback`, et un même texte de step défini
dans deux classes fait échouer **toute** la suite. `PupitreSteps` porte donc son propre phrasé (« au pupitre, … »,
« … au referentiel du pupitre »).

## Ports sortants

`OperateursDuPupitre`, `SuivisOuvertsDuPupitre`, `PresencesDuPupitre`, `Clock`.

Les trois premiers rendent tout d'un coup, sans critères ni pagination : c'est leur raison d'être. Les adapters
lisent `operateur`, `operateur_poste`, `poste_de_travail`, `suivi_d_atelier`, `evenement_d_atelier`,
`journee_de_travail` et `element_de_fabrication`, et **écartent les événements annulés dès le SQL** — les rapporter pour les filtrer ensuite
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
