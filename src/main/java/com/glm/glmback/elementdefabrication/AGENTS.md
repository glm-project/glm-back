# Bounded context `elementdefabrication`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Déclarer et nommer ce que l'entreprise fabrique**, et rien d'autre :

1. **Créer** un élément de fabrication dans une catégorie de produit de l'entreprise (`MOULE`, `OF`…), en lui
   attribuant son nom par numérotation automatique.
2. **Réviser** sa fiche : la référence que l'entreprise lui donne dans son propre système, et sa description.
3. **Lire** un élément ou la liste paginée des éléments.

« Produit » est volontairement générique : l'application s'adresse à plusieurs entreprises clientes, dont les métiers
nomment différemment ce qu'elles fabriquent (des moules, chez le client de référence). Ne jamais renommer ces concepts
d'après le vocabulaire d'un seul client.

## Ce dont il ne s'occupe pas

- **L'exécution** — engagement en atelier, pointages, temps passé, clôture. Tout cela appartient à `atelier`, qui ne
  connaît de ce contexte qu'une copie du nom et de la catégorie, prise à l'engagement.
- **La suppression** — le client ne parle que de clôture. Le jour où des temps seront saisis, supprimer un élément qui
  en porte empêcherait de relire les faits et leurs coûts.
- **Déclarer les catégories** — cela appartient à `categoriedeproduit`. Ce contexte n'en lit que l'existence, par le
  port `CategoriesDeclarees`, sur une entité en lecture seule de `categorie_de_produit`.
- **Le lien produit → ordre de fabrication**, que le client décrit mais ne demande pas.
- **L'isolation par entreprise** — assurée par l'infrastructure multi-tenant. Aucun agrégat ne porte d'identifiant
  d'entreprise.

## Agrégat

`ElementDeFabrication`, portant sa `Fiche` et sa `Categorie`.

La catégorie est une **valeur** libre que l'entreprise déclare, pas une hiérarchie ni une liste fermée : les éléments de
deux catégories ne diffèrent que par cette valeur, leur préfixe de nommage et leur série de numérotation. Scinder
plus tard, sur une différence réelle — le lien produit → OF, par exemple — coûtera moins cher.

## Invariants à ne pas casser

- **Un élément ne se crée que dans une catégorie déclarée** par l'entreprise, sinon `CategorieInconnueException`
  (409). La garde vit dans `ElementsDeFabricationService`, derrière le port `CategoriesDeclarees` ; la clé étrangère
  `fk_element_de_fabrication_categorie` est le filet.
- **Le nom est produit par le domaine, jamais fourni par l'API.** Il se compose du code de la catégorie, d'une année
  et d'un compteur propre à la catégorie et à l'année ; sa fabrication (`Nom.of`) appartient à `ElementsDeFabricationService`, qui détient les ports. Le step
  builder de l'agrégat prend un `Nom` déjà formé, jamais ses ingrédients — sans quoi la modification, qui conserve le
  nom existant, devrait le décomposer pour le reconstruire à l'identique.
- **La `Reference` est unique par entreprise quand elle est renseignée.** La garde vit dans
  `ElementsDeFabricationService`, qui lit le détenteur par `ElementDeFabricationRepository.idPourReference` et lève
  `ReferenceDejaUtiliseeException` (409). La contrainte du schéma est le filet de dernier recours, pas la règle.
- **Les deux champs de la `Fiche` sont facultatifs** : un élément se réduit légitimement à son seul numéro.
- **Domaine immuable** : la révision passe par `Fiche.revise`, qui conserve `dateDeCreation` et refuse une
  `dateDeModification` antérieure. Aucun setter, aucune méthode par champ.
- **L'état mutable — compteur, catégories déclarées, horloge — n'est jamais un champ du domaine** : il vient de ports
  (`CompteurDElementsDeFabrication`, `CategoriesDeclarees`, `Clock`), et la règle qui les utilise vit dans
  `ElementsDeFabricationService`.

## Structure

Les quatre couches existent : `domain/`, `application/`, `infrastructure/primary/` (REST) et `infrastructure/secondary/`
(JPA). Ce contexte sert donc de patron pour câbler `atelier`, qui n'a encore que son domaine.

**Transition** : l'API accepte encore l'ancien champ `type` (`ORDRE_DE_FABRICATION` → `OF`, `PRODUIT` → `MOULE`) et le
rend, déprécié, à côté de `categorie`. La traduction vit dans `shared/elementtype` (`LegacyElementType`), hors du
domaine ; elle disparaît avec le champ dès que le front lit `categorie`.
