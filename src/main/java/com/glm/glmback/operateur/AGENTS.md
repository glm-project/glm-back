# Bounded context `operateur`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **référentiel des personnes qui pointent** : nom, prénom, matricule facultatif, et les **postes sur lesquels elles
sont habilitées**.

Il en déduit, à la lecture seulement, les **métiers** de l'opérateur : `ProfilDOperateur.natures()` rend les natures de
ses postes. Un opérateur habilité sur le poste de soudure et sur un tour **est** soudeur et tourneur, sans que personne
ne l'ait saisi deux fois.

## Ce dont il ne s'occupe pas

- **Les gestionnaires**, ni aucun utilisateur qui ne pointe pas. Leur fiche n'aurait aucun usage tant que
  l'authentification n'est pas tranchée : l'`Auteur` d'une saisie vient du jeton, pas d'un référentiel.
- **La nature de travail comme propriété d'une personne.** Elle appartient au poste. Déclarer un métier sur
  l'opérateur le stockerait deux fois, avec la possibilité qu'ils se contredisent.
- **Le calcul du coût de revient**. L'opérateur porte son `TauxHoraire` (facultatif, strictement positif), mais ce
  contexte ne fait rien d'autre que le stocker et le restituer : aucun calcul, aucune répartition. `atelier` ne le lit
  pas encore — c'est le lot « coût de revient » qui posera ce port.
- **Le pointage**, qui appartient à `atelier` — lequel ne connaît de ce contexte que l'identifiant, lu par port, et
  n'en copie rien.
- **L'identification à la borne** et l'authentification. Le matricule est un attribut d'identité, pas un moyen de
  connexion.

## Agrégat

`Operateur` — six composants, donc **step builder en chaîne de lambdas** : les six types d'étapes sont tous
distincts, aucune inversion ne compile, le constructeur privé prend la liste positionnelle sans risque. `builder()` est
public parce que la relecture depuis la persistance se fait dans `infrastructure/secondary`.

`ProfilDOperateur` n'est pas un agrégat : c'est la lecture composée que les écrans consomment, opérateur plus postes
résolus.

## Invariants à ne pas casser

- **L'identité (nom, prénom) est unique par entreprise** → `IdentiteDejaUtiliseeException` (409).
- **Le matricule est unique quand il est renseigné**, et plusieurs opérateurs peuvent rester sans — patron
  `elementdefabrication.Reference`. PostgreSQL considérant les `NULL` comme distincts, l'index unique laisse coexister
  autant d'opérateurs sans matricule que nécessaire.
- **Le taux horaire est facultatif et strictement positif** quand il est renseigné, sur le patron du matricule : un
  taux à zéro n'a pas de sens métier — s'il est inconnu, le champ reste absent plutôt qu'à zéro.
- **Tout poste référencé existe.** Invariant inter-contextes : il ne peut pas vivre dans le constructeur, qui ne voit
  que des identifiants. Il est porté par `OperateursService`, qui détient les deux ports.
- **Rien n'est copié du poste**, à la différence de l'atelier qui copie nom et type à l'engagement. Aucun historique ne
  pend ici à un poste : un poste renommé doit s'afficher renommé partout. L'opérateur ne stocke donc que
  l'identifiant ; libellé et nature sont relus à chaque lecture.
- **Aucun import de `postedetravail`**, annoté `@BusinessContext`. Le contexte déclare ses propres `PosteHabilitableId`,
  `LibelleDePoste` et `NatureDeTravail`, et n'atteint la table voisine que par une entité en lecture seule (patron
  `ElementEngageableEntity`).
- **Une page se résout en une requête.** `PostesHabilitables` n'expose que `parIds` : un accès unitaire coûterait
  autant de requêtes que d'habilitations sur la page.
- **Un opérateur qui a pointé ne se supprime pas**, qu'il ait pointé sur un élément ou seulement sa présence : le
  journal d'atelier ne retient que son identifiant, et sa disparition laisserait des heures sans personne à payer. La
  règle vit dans `OperateursService`, derrière le port `OperateursQuiOntPointe`.
- **Domaine immuable** : la révision passe par `Operateur.revise`, qui rend un nouvel agrégat de même identité.

## Ports sortants

`OperateurRepository`, `PostesHabilitables`, `OperateursQuiOntPointe`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI (`/swagger-ui.html`), sur le patron d'annotation de
`atelier`. En entrée l'opérateur porte des identifiants de postes ; **en sortie il porte les postes résolus et ses
natures dérivées**, pour qu'un écran se serve d'un seul appel. Les collections sont rendues triées — postes par
libellé, natures alphabétiquement — pour que deux appels identiques donnent la même réponse.

Le scénario de référence est `src/test/features/operateur.feature`, dont le premier cas porte le lot : deux postes de
natures différentes, un opérateur habilité sur les deux, et des métiers qui ressortent sans avoir été saisis.
