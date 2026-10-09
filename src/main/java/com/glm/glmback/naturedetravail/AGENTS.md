# Bounded context `naturedetravail`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **référentiel des métiers exercés dans l'atelier** : soudage, tournage, fraisage, dessin. Déclarer, renommer et
supprimer les natures, et les lister par ordre alphabétique, en disant pour chacune si elle sert déjà.

## Ce dont il ne s'occupe pas

- **La nature d'un poste** — elle appartient à `postedetravail`, qui ne retient de ce contexte que l'identifiant et
  relit le libellé par la donnée.
- **Recopier un libellé ailleurs**. Un renommage ne réécrira jamais d'autre table : ceux qui se servent d'une nature
  en retiendront l'identifiant et liront le libellé courant.
- **Des natures par défaut**. Une entreprise neuve n'en a aucune.

## Agrégat

`NatureDeTravail` — deux composants, `NatureDeTravailId` et `LibelleDeNature`, donc pas de step builder.

## Invariants à ne pas casser

- **Le libellé est unique par entreprise à la casse, aux accents et aux espaces près.** L'unicité se juge sur la
  `CleDeNature` (minuscules, sans accents, espaces réduits à un seul), jamais sur le libellé. La garde vit dans
  `NaturesDeTravailService` ; la contrainte `ux_nature_de_travail_cle` est le filet.
- **Un renommage ne touche que la nature** : la transition `NatureDeTravail.renomme` conserve l'identifiant. Une
  nature peut reprendre sa propre clé, pour changer la casse ou les accents de son libellé.
- **Le libellé est rogné à la construction** : de 1 à 50 caractères une fois les espaces qui l'entourent retirés.
- **L'ordre de lecture est total** : clé, puis identifiant.
- **Une nature qui sert ne se supprime pas** (`NatureUtiliseeException`, 409). La règle vit dans le domaine, derrière
  le port `NaturesEnUsage`, et la liste dit pour chaque nature si elle sert, pour que l'écran ne propose pas une
  suppression vouée au refus. Les usages d'une page se lisent en une requête (`NaturesEnUsage.utiliseesParmi`).
- **Une nature sert dès qu'un poste la porte ou qu'un pointage l'a recopiée.** L'adapter `UsagesDesNatures` lit
  `poste_de_travail.nature_id` et `evenement_d_atelier.nature_id` par des entités en lecture seule, sans importer
  `postedetravail` ni `atelier`. Deux refus distincts : `nature-utilisee` tant qu'un poste la porte (la clé étrangère
  `fk_poste_de_travail_nature` est le filet), `nature-pointee` dès qu'un pointage l'a recopiée — définitif, et sans
  autre filet que la règle : comme pour l'opérateur et le poste, le journal ne porte aucune clé étrangère.

## Ports sortants

`NatureDeTravailRepository`, `NaturesEnUsage`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI, sur le patron de `postedetravail`.
`infrastructure/secondary/` persiste dans le schéma de l'entreprise courante, table `nature_de_travail`.
