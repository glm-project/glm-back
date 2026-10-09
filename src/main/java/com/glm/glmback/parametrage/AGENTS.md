# Bounded context `parametrage`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/AGENTS.md](../../../../../../../AGENTS.md), le détail métier et sa justification dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Les **réglages que l'entreprise fixe elle-même**, un seul jeu pour toute l'entreprise : aujourd'hui la durée max
d'une activité et le logo de l'entreprise. Les lire, et les modifier pour le gestionnaire.

## Ce dont il ne s'occupe pas

- **Appliquer un réglage.** La fin automatique d'une activité appartient à `atelier`, qui lira la durée par un port ;
  ce contexte ne connaît ni les activités ni leur échéance.
- **La configuration technique d'une entreprise** (schéma, base, pool) : c'est le registre `public.tenant`, hors des
  schémas d'entreprise.

## Agrégat

`Parametrage` — un composant par réglage, aujourd'hui `DureeMaxDActivite`. Il n'a pas d'identifiant : l'entreprise
courante n'en a qu'un.

`Logo` vit à part, derrière son propre port : son contenu pèse jusqu'à 20 Ko, et la lecture des autres réglages ne doit
pas le charger. Il partage la ligne unique du paramétrage (colonnes `logo_*`), lue et écrite par sa propre entité.

## Invariants à ne pas casser

- **Une seule ligne par schéma**, créée par le changelog avec le schéma (`ck_parametrage_unique`). Le port n'a donc
  ni `create` ni `delete` : un réglage est toujours une mise à jour.
- **Un réglage jamais fixé vaut sa valeur par défaut**, que seul le domaine connaît (`DureeMaxDActivite.parDefaut()`,
  treize heures) : la colonne vide ne la recopie pas.
- **La durée max d'une activité est comprise entre une heure et vingt-quatre heures**, bornes comprises.
- **Un logo est une image PNG ou JPEG de 50 x 50 pixels exactement, de 20 Ko au plus** (`DepotDeLogo`). Le poids se
  juge avant tout décodage ; le format et les dimensions viennent du contenu, lu par le port `DecodeurDImage`, jamais
  du nom du fichier ni du type annoncé. Aucun recadrage ni aucune conversion : le logo s'affiche tel qu'il a été déposé.
- **La version d'un logo est l'empreinte de son contenu** (`VersionDuLogo`) : elle change avec lui. C'est une clé de
  cache, pas une preuve d'intégrité. L'image se sert à l'adresse de sa version, en cache privé d'un an et immuable ;
  une version qui n'est plus la courante répond 404 (`LectureDuLogo`), jamais par le logo courant, sans quoi une
  adresse gardée en cache montrerait un autre logo que celui qu'elle nomme.
- **Retirer le logo vide ses colonnes** et laisse la ligne en place ; sans logo, retirer est sans effet. Les en-têtes
  reviennent alors au logo de GLM, côté front.

## Ports sortants

`ParametrageRepository`, `LogoRepository`, `DecodeurDImage`.

## Structure

Les quatre couches existent. L'API REST est décrite par OpenAPI. `infrastructure/secondary/` persiste dans le schéma
de l'entreprise courante, table `parametrage`.
