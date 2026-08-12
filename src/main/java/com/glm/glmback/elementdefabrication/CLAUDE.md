# Bounded context `elementdefabrication`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/CLAUDE.md](../../../../../../../CLAUDE.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

**Déclarer et nommer ce que l'entreprise fabrique**, et rien d'autre :

1. **Créer** un élément de fabrication — ordre de fabrication ou produit — en lui attribuant son nom par numérotation
   automatique.
2. **Réviser** sa fiche : la référence que l'entreprise lui donne dans son propre système, et sa description.
3. **Lire** un élément ou la liste paginée des éléments.

« Produit » est volontairement générique : l'application s'adresse à plusieurs entreprises clientes, dont les métiers
nomment différemment ce qu'elles fabriquent (des moules, chez le client de référence). Ne jamais renommer ces concepts
d'après le vocabulaire d'un seul client.

## Ce dont il ne s'occupe pas

- **L'exécution** — engagement en atelier, pointages, temps passé, clôture. Tout cela appartient à `atelier`, qui ne
  connaît de ce contexte qu'une copie du nom et du type, prise à l'engagement.
- **La suppression** — le client ne parle que de clôture. Le jour où des temps seront saisis, supprimer un élément qui
  en porte détruirait des heures de paie.
- **Le lien produit → ordre de fabrication**, que le client décrit mais ne demande pas.
- **L'isolation par entreprise** — assurée par l'infrastructure multi-tenant. Aucun agrégat ne porte d'identifiant
  d'entreprise.

## Agrégat

`ElementDeFabrication`, portant sa `Fiche` et son `TypeDElementDeFabrication`.

Le type est une **valeur**, pas une hiérarchie scellée : ordre de fabrication et produit ne diffèrent aujourd'hui que
par ce type, leur préfixe de nommage et leur série de numérotation. Deux sous-types identiques au nom près feraient
payer chaque évolution deux fois. Scinder plus tard, sur une différence réelle — le lien produit → OF, par exemple —
coûtera moins cher.

## Invariants à ne pas casser

- **Le nom est produit par le domaine, jamais fourni par l'API.** Il se compose d'un préfixe, d'une année et d'un
  compteur ; sa fabrication (`Nom.of`) appartient à `ElementsDeFabricationService`, qui détient les ports. Le step
  builder de l'agrégat prend un `Nom` déjà formé, jamais ses ingrédients — sans quoi la modification, qui conserve le
  nom existant, devrait le décomposer pour le reconstruire à l'identique.
- **La `Reference` est unique par entreprise quand elle est renseignée.** La garde vit dans
  `ElementsDeFabricationService`, qui lit le détenteur par `ElementDeFabricationRepository.idPourReference` et lève
  `ReferenceDejaUtiliseeException` (409). La contrainte du schéma est le filet de dernier recours, pas la règle.
- **Les deux champs de la `Fiche` sont facultatifs** : un élément se réduit légitimement à son seul numéro.
- **Domaine immuable** : la révision passe par `Fiche.revise`, qui conserve `dateDeCreation` et refuse une
  `dateDeModification` antérieure. Aucun setter, aucune méthode par champ.
- **L'état mutable — compteur, préfixes, horloge — n'est jamais un champ du domaine** : il vient de ports
  (`CompteurDElementsDeFabrication`, `PrefixesDElementsDeFabrication`, `Clock`), et la règle qui les utilise vit dans
  `ElementsDeFabricationService`.

## Structure

Les quatre couches existent : `domain/`, `application/`, `infrastructure/primary/` (REST) et `infrastructure/secondary/`
(JPA). Ce contexte sert donc de patron pour câbler `atelier`, qui n'a encore que son domaine.

`InMemoryPrefixesDElementsDeFabrication` fige les préfixes pour toutes les entreprises — contradiction connue avec la
cible multi-clients, listée en point ouvert dans `documentation/contexte-metier.md`.
