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
- **Le calcul du coût de revient**. Le poste porte son `CoutHoraire` (facultatif, strictement positif), mais ce
  contexte ne fait rien d'autre que le stocker et le restituer : aucun calcul, aucune répartition. `atelier` ne le lit
  pas encore — c'est le lot « coût de revient » qui posera ce port.
- **Le pointage** lui-même, qui appartient à `atelier`. Celui-ci ne connaît de ce contexte que l'identifiant, lu par
  port, et n'en copie que la nature au moment de la saisie.

## Agrégat

`PosteDeTravail` — quatre composants (`PosteDeTravailId`, `Libelle`, `NatureDeTravail`, `Optional<CoutHoraire>`), donc
**step builder en chaîne de lambdas** depuis l'ajout du coût horaire : les quatre types d'étapes sont tous distincts,
aucune inversion ne compile. `builder()` est public parce que la relecture depuis la persistance se fait dans
`infrastructure/secondary`.

## Invariants à ne pas casser

- **Le libellé est unique par entreprise.** C'est ce qui fait du référentiel un référentiel, et ce qui évite que
  « Tour 1 » désigne deux choses. La garde vit dans `PostesDeTravailService`, sur le patron de
  `ElementsDeFabricationService.verifierReferenceLibre` : un seul chemin sert création et modification, et le poste qui
  conserve son propre libellé ne se heurte pas à lui-même. La contrainte du schéma est le filet de dernier recours, pas
  la règle.
- **La nature est obligatoire**, contrairement à l'atelier où elle reste facultative. Un poste n'est déclaré que pour
  dire quel travail s'y fait — c'est de lui, et non de la personne, que vient le métier exercé à un instant donné.
- **Le coût horaire est facultatif et strictement positif** quand il est renseigné, sur le patron de `Matricule` :
  toutes les entreprises ne valorisent pas encore leurs postes, et un coût à zéro n'a pas de sens métier — s'il est
  inconnu, le champ reste absent plutôt qu'à zéro.
- **Un poste encore habilité ne se supprime pas** : cela laisserait des opérateurs pointer sur du vide. La règle vit
  dans le domaine, derrière le port `PostesEnUsage` ; la clé étrangère de `operateur_poste` n'est que le filet.
- **Un poste sur lequel du temps a été pointé ne se supprime plus du tout**, et ce refus-là est définitif : le journal
  d'atelier ne retient que l'identifiant du poste, donc le supprimer laisserait des heures de travail sans machine.
  Port `PostesPointes`, sur `evenement_d_atelier`.
- **Aucun import de `operateur`**, annoté `@BusinessContext`. Le contexte voisin n'est atteint que par la donnée, via
  une entité en lecture seule sur `operateur_poste` (patron `ElementEngageableEntity`).
- **Domaine immuable** : la révision passe par `PosteDeTravail.revise`, qui rend un nouvel agrégat de même identité.
  Aucun setter, aucune méthode par champ.

## Ports sortants

`PosteDeTravailRepository`, `PostesEnUsage`, `PostesPointes`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI (`/swagger-ui.html`), sur le patron d'annotation de
`atelier`. `infrastructure/secondary/` persiste dans le schéma de l'entreprise courante.

`OperateurHabiliteEntity` déclare son identifiant JPA sur `operateur_id` alors que la table porte une clé composite :
c'est délibéré et sans risque, parce que la seule lecture faite est une **question de présence**
(`findFirstByPosteId`), jamais un accès par identité. Un `@EmbeddedId` n'apporterait qu'un `equals`/`hashCode` de plus
à couvrir.
