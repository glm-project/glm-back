# Bounded context `pupitre`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Alimenter le cache local du poste d'atelier, pour qu'il continue à collecter sans réseau.** Un seul acte : rendre,
en un appel et dans une transaction unique, tout ce que le pupitre doit garder sur disque — les opérateurs
désignables avec leurs habilitations, les éléments encore pointables avec leurs
activités en cours — et l'**instant d'évaluation** `genereLe`. Les requêtes successives sous
`READ COMMITTED` peuvent lire des écritures intervenues pendant cet appel.

C'est une **projection transverse**, comme `feuilledetemps`, `coutderevient` et `syntheseheures` : un contexte
purement lecteur, qui ne possède aucune table, n'écrit rien, et recalcule tout à chaque appel.

## Ce dont il ne s'occupe pas

- **Le pointage lui-même et son jugement** : le pupitre écrit par l'API d'`atelier`, jamais par ici. Ce contexte ne
  propose aucune écriture, et n'en proposera pas — le chemin d'écriture existe déjà chez `atelier` : identifiants de
  geste créés au pupitre, rejeu à 200 quand l'identifiant figure déjà dans les événements, et règle de réception qui
  ignore (409 `pointage-ignore`) un pointage qui ne s'accorde pas à l'état de sa clé. Le pupitre n'applique pas cette
  règle, il s'y recale : il retire l'effet local d'un pointage ignoré et relit le référentiel.
- **Le référentiel lui-même** : créer, modifier ou supprimer un opérateur, un poste ou un élément appartient à
  `operateur`, `postedetravail` et `elementdefabrication`.
- **La valorisation** — ni taux horaire d'opérateur, ni coût horaire de poste. Ces montants ne sont même pas mappés
  par les entités de lecture : un champ qu'on ne lit pas ne peut pas fuir par mégarde vers un écran d'atelier
  partagé, alors que `coutderevient` les réserve au gestionnaire.
- **L'identité du poste d'atelier et son enrôlement.** Le pupitre porte un compte d'appareil Keycloak et traverse la
  chaîne d'autorisation comme n'importe quel `USER` d'une entreprise ; rien de spécifique ne vit ici.

## Agrégat de lecture

`ReferentielDuPupitre` : un `genereLe`, une liste d'`OperateurDuPupitre`, une liste de `SuiviDuPupitre`, la liste
ordonnée des `CategorieDElement` de l'entreprise, qui range les tuiles, et la durée maximale d'une activité
(`dureeMaximaleDActivite`, `"PT13H"` en ISO 8601). Elle vient du noyau partagé `shared/activityduration` que lit aussi
l'échéance d'atelier : le pupitre n'importe pas `atelier`, et le serveur n'a qu'une source de ce délai. Le pupitre la lit
ici au lieu de coder 13 h. Aucune identité, aucune persistance — l'objet naît et meurt dans l'appel.

`ReferentielsDuPupitreService` assemble les opérateurs, les suivis et les catégories, et les date par le port `Clock`.

`SuiviDuPupitre` lit les activités sans fin projetées par atelier. `ActiviteSansFin` transmet leur identité (celle de
leur pointage ouvrant) et leur échéance ; `etatA` et `activitesEnCoursA` évaluent leur expiration à `genereLe`.
Un pointage au journal distingue `INTERROMPU` de `EN_ATTENTE` quand aucune activité n'est en cours.

## Invariants à ne pas casser

- **La réponse rend le référentiel entier.** Un seul appel évite l'assemblage de pages issues de lectures
  différentes. Le volume est borné par la taille de l'atelier ; la limite de concurrence ci-dessous reste applicable.
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
- **L'interprétation appartient à atelier.** Ce lecteur relit `activite_d_atelier`, sans replier le journal.
  Les activités ayant une fin réelle sont écartées ; l'échéance inclusive est jugée à `genereLe`. L'identité rendue
  dans `ouverture` vient d'`activite_d_atelier.id`, celle du pointage ouvrant. `cloture_date_de_survenue` continue
  d'écarter les suivis clôturés.
- **Un opérateur sans activité n'est jamais omis.** La liste rend l'identité, l'identifiant éventuel et les postes
  habilités des opérateurs désignables, indépendamment des pointages.
- **Les lectures se font par ensembles.** Opérateurs et habilitations, activités et références se lisent
  sans requête par opérateur ou par suivi.
- **Un élément clôturé est absent**, et `EtatDuSuivi` ne porte donc pas de valeur `CLOTURE` : elle n'aurait aucun
  porteur.
- **Le nom de l'élément vient du suivi, sa référence du référentiel.** Le nom est copié à l'engagement — un élément
  renommé ne réécrit pas l'histoire de l'atelier — tandis que la référence est relue à chaque lecture, comme les
  identités d'opérateurs. Un élément supprimé du référentiel laisse donc sa tuile intacte, privée de sa seule
  référence.
- **Aucun import d'`atelier`, `operateur`, `postedetravail` ni `elementdefabrication`**, tous annotés
  `@BusinessContext`. Ce contexte déclare ses propres entités JPA `@Immutable` sur leurs tables.

## Les adapters ne peuvent porter aucun nom déjà pris

Spring nomme un bean d'après le nom **simple** de sa classe, et Hibernate enregistre une entité JPA sous son nom
simple par défaut : deux classes homonymes dans des packages différents refusent de démarrer ensemble.
Nommer les entités et adapters d'après leur contexte lecteur, et reprendre les noms logiques des colonnes de
leur propriétaire ; le package seul ne suffit pas à les distinguer.

Même règle côté Cucumber : le glue est scanné depuis la racine `com.glm.glmback`, et un même texte de step défini
dans deux classes fait échouer **toute** la suite. `PupitreSteps` porte donc son propre phrasé (« au pupitre, … »,
« … au referentiel du pupitre »).

## Ports sortants

`OperateursDuPupitre`, `SuivisOuvertsDuPupitre`, `CategoriesDuPupitre`, `Clock`.

Les trois lecteurs rendent tout d'un coup, sans critères ni pagination. Leurs entités propres `@Immutable`
lisent `operateur`, `operateur_poste`, `poste_de_travail`, `suivi_d_atelier`, `activite_d_atelier`,
`element_de_fabrication` et `categorie_de_produit`. Les catégories
sont triées par rang puis par code, comme dans `categoriedeproduit` : le domaine ne connaît pas le rang. Une requête scalaire sur
`evenement_d_atelier` relève les suivis portant au moins un pointage, sans rapporter leur journal.

## État d'avancement

Les quatre couches sont livrées, pour la seule route `GET /api/pupitre/referentiel`, ouverte à `USER` et
`GESTIONNAIRE`.

`infrastructure/secondary` n'a **aucun test dédié**, comme chez `feuilledetemps`, `coutderevient` et
`syntheseheures`, hormis `CategoriesDuReferentielDuPupitreIT`, qui tient l'ordre et l'entreprise sans catégorie,
deux cas que le schéma partagé des scénarios ne sait pas isoler. Pour le reste, sa correction est vérifiée par `src/test/features/pupitre_referentiel.feature`, qui engage,
pointe et clôture par l'API d'`atelier` puis relit par celle-ci — il échoue dès que les deux contextes
cessent de lire les mêmes colonnes.

Ce que le front en attend, et ce qu'il en fait, est décrit dans
[documentation/atelier-api.md](../../../../../../../documentation/atelier-api.md), section « Le référentiel du
pupitre ».
