# Bounded context `naturedetravail`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **référentiel des métiers exercés dans l'atelier** : soudage, tournage, fraisage, dessin. Déclarer les natures et
les lister par ordre alphabétique, en disant pour chacune si elle sert déjà.

## Ce dont il ne s'occupe pas

- **La nature d'un poste** — elle appartient à `postedetravail`, qui ne connaîtra de ce contexte que l'identifiant,
  lu par la donnée.
- **Recopier un libellé ailleurs**. Un renommage ne réécrira jamais d'autre table : ceux qui se servent d'une nature
  en retiendront l'identifiant et liront le libellé courant.
- **Des natures par défaut**. Une entreprise neuve n'en a aucune.

## Agrégat

`NatureDeTravail` — deux composants, `NatureDeTravailId` et `LibelleDeNature`, donc pas de step builder.

## Invariants à ne pas casser

- **Le libellé est unique par entreprise à la casse, aux accents et aux espaces près.** L'unicité se juge sur la
  `CleDeNature` (minuscules, sans accents, espaces réduits à un seul), jamais sur le libellé. La garde vit dans
  `NaturesDeTravailService` ; la contrainte `ux_nature_de_travail_cle` est le filet.
- **Le libellé est rogné à la construction** : de 1 à 50 caractères une fois les espaces qui l'entourent retirés.
- **L'ordre de lecture est total** : clé, puis identifiant.
- **Les usages d'une page se lisent en une requête** (`NaturesEnUsage.utiliseesParmi`). Rien ne référence encore une
  nature : l'adapter `NaturesSansUsage` répond qu'aucune ne sert, jusqu'à ce que les postes puis les pointages en
  portent l'identifiant.

## Ports sortants

`NatureDeTravailRepository`, `NaturesEnUsage`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI, sur le patron de `postedetravail`.
`infrastructure/secondary/` persiste dans le schéma de l'entreprise courante, table `nature_de_travail`.
