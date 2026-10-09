# Bounded context `parametrage`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Les **réglages que l'entreprise fixe elle-même**, un seul jeu pour toute l'entreprise : aujourd'hui la durée max
d'une activité. Les lire, et les modifier pour le gestionnaire.

## Ce dont il ne s'occupe pas

- **Appliquer un réglage.** La fin automatique d'une activité appartient à `atelier`, qui lira la durée par un port ;
  ce contexte ne connaît ni les activités ni leur échéance.
- **La configuration technique d'une entreprise** (schéma, base, pool) : c'est le registre `public.tenant`, hors des
  schémas d'entreprise.

## Agrégat

`Parametrage` — un composant par réglage, aujourd'hui `DureeMaxDActivite`. Il n'a pas d'identifiant : l'entreprise
courante n'en a qu'un.

## Invariants à ne pas casser

- **Une seule ligne par schéma**, créée par le changelog avec le schéma (`ck_parametrage_unique`). Le port n'a donc
  ni `create` ni `delete` : un réglage est toujours une mise à jour.
- **Un réglage jamais fixé vaut sa valeur par défaut**, que seul le domaine connaît (`DureeMaxDActivite.parDefaut()`,
  treize heures) : la colonne vide ne la recopie pas.
- **La durée max d'une activité est comprise entre une heure et vingt-quatre heures**, bornes comprises.

## Ports sortants

`ParametrageRepository`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI. `infrastructure/secondary/` persiste dans le schéma
de l'entreprise courante, table `parametrage`.
