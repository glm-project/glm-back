# Bounded context `postedetravail`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/CLAUDE.md](../../../../../../../CLAUDE.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **référentiel de ce sur quoi les opérateurs pointent**, et rien d'autre : déclarer, réviser, lister et supprimer les
postes de travail de l'entreprise.

Un poste porte un **libellé** et une **nature de travail**. Le terme « poste » est volontairement générique, comme dans
l'atelier : une machine chez le client de référence, un établi, un four, une salle ailleurs.

## Ce dont il ne s'occupe pas

- **Qui est habilité dessus** — cela appartient à `operateur`. Ce contexte n'en connaît que la conséquence : un poste
  encore habilité ne se supprime pas.
- **Le coût horaire** du poste, et tout montant : lot « coût de revient ». La forme de l'agrégat est prête à le
  recevoir, il n'y est pas.
- **Le pointage** lui-même, qui appartient à `atelier`. Celui-ci continue de recevoir un `PosteDeTravail` en texte
  libre dans le corps de la requête : le branchement des deux est un lot à part.

## Agrégat

`PosteDeTravail` — trois composants (`PosteDeTravailId`, `Libelle`, `NatureDeTravail`), donc pas de step builder : la
règle ne s'applique qu'au-delà de trois.

## Invariants à ne pas casser

- **Le libellé est unique par entreprise.** C'est ce qui fait du référentiel un référentiel, et ce qui évite que
  « Tour 1 » désigne deux choses. La garde vit dans `PostesDeTravailService`, sur le patron de
  `ElementsDeFabricationService.verifierReferenceLibre` : un seul chemin sert création et modification, et le poste qui
  conserve son propre libellé ne se heurte pas à lui-même. La contrainte du schéma est le filet de dernier recours, pas
  la règle.
- **La nature est obligatoire**, contrairement à l'atelier où elle reste facultative. Un poste n'est déclaré que pour
  dire quel travail s'y fait — c'est de lui, et non de la personne, que vient le métier exercé à un instant donné.
- **Un poste encore habilité ne se supprime pas** : cela laisserait des opérateurs pointer sur du vide. La règle vit
  dans le domaine, derrière le port `PostesEnUsage` ; la clé étrangère de `operateur_poste` n'est que le filet.
- **Aucun import de `operateur`**, annoté `@BusinessContext`. Le contexte voisin n'est atteint que par la donnée, via
  une entité en lecture seule sur `operateur_poste` (patron `ElementEngageableEntity`).
- **Domaine immuable** : la révision passe par `PosteDeTravail.revise`, qui rend un nouvel agrégat de même identité.
  Aucun setter, aucune méthode par champ.

## Ports sortants

`PosteDeTravailRepository`, `PostesEnUsage`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI (`/swagger-ui.html`), sur le patron d'annotation de
`atelier`. `infrastructure/secondary/` persiste dans le schéma de l'entreprise courante.

`OperateurHabiliteEntity` déclare son identifiant JPA sur `operateur_id` alors que la table porte une clé composite :
c'est délibéré et sans risque, parce que la seule lecture faite est une **question de présence**
(`findFirstByPosteId`), jamais un accès par identité. Un `@EmbeddedId` n'apporterait qu'un `equals`/`hashCode` de plus
à couvrir.
