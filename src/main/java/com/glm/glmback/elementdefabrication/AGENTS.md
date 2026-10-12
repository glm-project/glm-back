# Bounded context `elementdefabrication`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Déclarer et nommer ce que l'entreprise fabrique**, et rien d'autre :

1. **Créer** un élément de fabrication dans une catégorie de produit de l'entreprise (`MOULE`, `OF`…), en lui
   attribuant son nom par numérotation automatique.
2. **Réviser** sa fiche : la référence que l'entreprise lui donne dans son propre système, et sa description.
3. **Lire** un élément, ou la liste paginée des éléments **dans leur cycle** (en préparation, en atelier, clôturés),
   avec ses compteurs par cycle.

« Produit » est volontairement générique : l'application s'adresse à plusieurs entreprises clientes, dont les métiers
nomment différemment ce qu'elles fabriquent (des moules, chez le client de référence). Ne jamais renommer ces concepts
d'après le vocabulaire d'un seul client.

## Ce dont il ne s'occupe pas

- **L'exécution** — engagement en atelier, pointages, temps passé, clôture. Tout cela appartient à `atelier`, qui ne
  connaît de ce contexte qu'une copie du nom et de la catégorie, prise à l'engagement. Ce contexte **lit** le suivi
  d'atelier pour dire où en est un élément, sans jamais l'écrire ni importer `atelier`.
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

## Lecture par cycle

L'élément ne porte aucun statut : son cycle se lit sur son suivi d'atelier (`ElementDansSonCycle`, `PassageOuvert`
ou `PassageClos`), unique par élément. Le port `ElementsDansLeurCycle` lit la liste et les compteurs ;
`JpaElementsDansLeurCycle` joint `suivi_d_atelier` par des entités `@Immutable` propres à ce contexte
(`SuiviDuProduitEntity`, `ActiviteDuProduitEntity`), puis juge l'état des seuls suivis ouverts de la page avec la
règle de l'atelier : en cours si la projection `activite_d_atelier` porte une activité sans fin dont l'échéance n'est pas
atteinte, interrompu si le journal (`evenement_d_atelier`) porte un fait, en attente sinon. La projection ne dit pas si un suivi
est pointé : le gestionnaire peut retirer toutes ses activités, qui disparaissent alors de la projection, et le suivi reste
interrompu, comme l'atelier le dit. `JpaElementsDansLeurCycleIT` confronte cet état à `SuiviDAtelier.etat`, retrait compris :
le modifier avec toute évolution de l'état côté atelier.

Les règles de sélection vivent dans le domaine (`FiltreDesElements`, `Recherche`, `CycleDemande`), que l'adapter
traduit en SQL. Une période de clôture ne se demande qu'avec le cycle `CLOTURE` (400 `cloture-hors-des-clotures`) ;
une période à une seule borne ne filtre rien, comme la liste de l'atelier.

## Structure

Les quatre couches existent : `domain/`, `application/`, `infrastructure/primary/` (REST) et `infrastructure/secondary/`
(JPA). Ce contexte sert donc de patron pour câbler `atelier`, qui n'a encore que son domaine.
