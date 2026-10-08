# Bounded context `categoriedeproduit`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **référentiel des familles dans lesquelles l'entreprise range ce qu'elle fabrique** : des moules et des OF chez le
client de référence, autre chose ailleurs. Déclarer, réordonner, supprimer et lister les catégories dans l'ordre d'affichage
choisi par l'entreprise.

## Ce dont il ne s'occupe pas

- **Nommer les éléments de fabrication** — cela appartient à `elementdefabrication`, qui ne connaîtra de ce contexte
  que le code, lu par la donnée.
- **Un libellé distinct du code**. Le code est ce qui s'affiche ; il n'existe pas de seconde désignation à tenir
  cohérente avec la première.
- **Des catégories par défaut**. Une entreprise neuve n'en a aucune : rien dans le schéma ni dans le code ne présume
  du vocabulaire d'un client.

## Agrégat

`CategorieDeProduit` — deux composants, `CodeDeCategorie` et `Rang`, donc ni step builder ni identifiant technique :
le code **est** l'identité.

## Invariants à ne pas casser

- **Le code ne se renomme jamais.** Il sert de préfixe au nom des éléments (`MOULE-2026-000001`) : le changer
  désaccorderait les noms déjà attribués de leur catégorie. Aucune transition ne le touche, et l'API n'expose aucune
  modification. Son motif, `^[A-Z]{1,10}$`, est celui du préfixe de `elementdefabrication.Nom`.
- **Le code est unique par entreprise.** La garde vit dans `CategoriesDeProduitService` ; la clé primaire est le filet.
- **Une catégorie nouvelle se range en dernier** : `Rang` suit le dernier rang connu, lu par le port
  (`CategorieDeProduitRepository.dernierRang`), pour que l'ordre déjà choisi ne bouge pas.
- **Une catégorie qui range des produits ne se supprime pas** : leur nom porte son code. La règle vit dans le domaine,
  derrière le port `CategoriesUtilisees` ; son adapter lit `element_de_fabrication` par une entité en lecture seule
  (patron `ElementEngageableEntity`), sans importer `elementdefabrication`. La clé étrangère
  `fk_element_de_fabrication_categorie` est le filet.
- **Un réordonnancement donne l'ordre entier** : chaque catégorie une fois et une seule, sinon
  `OrdreIncompletException` (409). Les rangs sont alors réattribués de 1 à n par la transition
  `CategorieDeProduit.deplace`, qui conserve le code.
- **L'ordre de lecture est total** : rang, puis code. Le rang n'est pas unique en base — deux déclarations
  concurrentes peuvent obtenir le même — et le code départage.

## Ports sortants

`CategorieDeProduitRepository`, `CategoriesUtilisees`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI, sur le patron de `postedetravail`.
`infrastructure/secondary/` persiste dans le schéma de l'entreprise courante, table `categorie_de_produit`.
