# Bounded context `parametrage`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **paramétrage d'une entreprise** : pour l'instant, l'amplitude maximale d'une journée de travail (décision D1 de
[bornes-de-fin-de-journee.md](../../../../../../../documentation/strategie/bornes-de-fin-de-journee.md)). Le
gestionnaire la fixe, l'opérateur la lit, et la dernière modification est tracée.

## Ce dont il ne s'occupe pas

- **Juger une journée de travail.** Décider qu'une journée est abandonnée appartient à `atelier` (lot 3), qui lira
  la table `parametrage` par son propre port, sans importer ce contexte.
- **Un historique des valeurs.** Seule la dernière modification est conservée.

## Agrégat

`Parametrage` — `AmplitudeMaximale` et `Optional<Modification>`. Une seule instance par entreprise : pas
d'identifiant dans le domaine, la ligne unique est une affaire de schéma. La transition métier est
`fixeLAmplitudeMaximale`.

## Invariants à ne pas casser

- **La valeur par défaut vit en base, jamais dans le code.** Le changelog `2026/09/002-parametrage.xml` sème la ligne
  à 13 h dans chaque schéma d'entreprise. Aucune constante Java ne doit la répéter.
- **L'amplitude maximale se compte à la minute, strictement entre 0 et 24 h.** `AmplitudeMaximale` le garantit ;
  `RestAmplitudeMaximale` le répète en Bean Validation, faute de quoi une valeur hors bornes sortirait en 500.
  Les contraintes `check` du schéma sont le filet.
- **Une modification ne précède jamais celle qu'elle remplace.** Refixer la même valeur reste une modification.
- **L'auteur vient du jeton** (`AuteurConnecte`), jamais du corps de la requête.
- **L'absence de la ligne est une corruption**, pas un cas métier : `ParametrageIntrouvableException` n'a pas de
  traduction HTTP.

## Ports sortants

`ParametrageRepository` (`get`, `update` — jamais de `create` ni de `delete` : la ligne est semée), et l'horloge
partagée `Clock`.

## Tests

Le paramétrage est une ligne partagée par tous les tests d'une entreprise. `JpaParametrageRepositoryIT` annule ses
transactions ; les scénarios `@parametrage` remettent la valeur semée, sans trace, dans un hook `@After`.
