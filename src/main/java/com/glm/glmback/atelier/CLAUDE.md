# Bounded context `atelier`

Responsabilité, frontières et invariants de ce contexte. Les règles de code communes sont dans
[glm-back/CLAUDE.md](../../../../../../../CLAUDE.md), le détail métier et sa justification par le verbatim client dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md) — ne pas les dupliquer ici.

## Ce dont ce contexte s'occupe

Le **pointage et sa correction**, et rien d'autre. Trois actes :

1. **Engager** un élément de fabrication en atelier — geste métier explicite du back-office, distinct de la création de
   l'élément — puis le **clôturer** ou rouvrir la clôture.
2. **Enregistrer les pointages** : la présence de l'opérateur (arrivée, pause, reprise, départ) d'un côté, son travail
   sur un élément engagé (début, non conformité, fin) de l'autre.
3. **Corriger** ces saisies : `regularise` (saisie oubliée), `annule` (saisie en trop), `corrige` (saisie fausse).

Il en déduit, à la lecture seulement, les intervalles de temps passé — jamais stockés.

## Ce dont il ne s'occupe pas

Ne rien ajouter ici qui relève de :

- **le coût de revient monétaire** — taux horaire de l'opérateur, coût horaire du poste, temps réparti valorisé. Aucun
  montant n'entre dans ce contexte ;
- **le référentiel des ressources** — opérateur → fonction, postes autorisés, taux ; poste → libellé, nature, coût
  horaire. Ces données sont **lues par port**, jamais possédées ici. `Operateur` et `PosteDeTravail` sont des
  références opaques ;
- **la paie** — le contexte expose `amplitude()` et `fenetres()`, il ne choisit pas laquelle compte ;
- **le cycle de vie de l'élément de fabrication** lui-même, qui appartient à `elementdefabrication` ;
- **le fuseau horaire et le jour calendaire** — une `JourneeDeTravail` est bornée par une arrivée et un départ, pas par
  une date. Aucun `ZoneId`, aucun `LocalDate` dans ce contexte.

## Agrégats

| Agrégat            | Identité                | Journal             |
| ------------------ | ----------------------- | ------------------- |
| `JourneeDeTravail` | un opérateur, une venue | `JournalDePresence` |
| `SuiviDAtelier`    | un élément engagé       | `JournalDAtelier`   |

Ils cohabitent dans un seul contexte parce qu'ils partagent un même langage — opérateur, auteur, horodatage,
annulation — que le shared kernel ne peut pas accueillir puisqu'il est en anglais.

`TempsDAtelierService` est le seul point qui croise les deux : le temps effectif d'un élément est l'intersection de ses
intervalles bruts avec les fenêtres de présence de son opérateur.

## Invariants à ne pas casser

- **Le journal est la source de vérité.** Aucun état, aucun compteur, aucun intervalle n'est stocké : tout se déduit du
  repli. C'est la correction qui l'impose — une saisie rattrapée doit compter à l'heure où elle a eu lieu.
- **Horodatage bitemporel** sur chaque événement : date de survenue (métier) et date d'enregistrement (technique). Une
  régularisation se reconnaît à l'écart entre les deux, jamais à l'identité de l'auteur.
- **Un événement annulé reste au journal**, porteur de son `Annulation`. Le repli l'écarte ; personne ne le supprime.
- **La pause et le départ sont des faits de l'opérateur, écrits une seule fois.** Ne jamais les recopier dans le journal
  des éléments : c'est ce qui donne au client son bouton unique, et ce qui permet à une seule régularisation de départ
  de refermer tous les éléments de la journée.
- **`PosteDeTravail` et `NatureDOperation` sont toujours facultatifs.** L'application vise un maximum d'entreprises
  clientes ; celles qui n'ont ni parc machine ni métiers distincts laissent les deux vides et retrouvent un
  comportement cohérent, pas un cas dégradé.
- **La nature ne bloque rien.** Elle n'est qu'un axe d'agrégation pour la synthèse. Un pointage refusé en atelier
  coûterait plus cher qu'une ligne de synthèse mal rangée.
- **Aucun import de `elementdefabrication`**, annoté `@BusinessContext`. L'atelier déclare sa propre identité
  `ElementEngage`, dont le nom et le type sont **copiés à l'engagement**.
- **Domaine immuable** : toute transition rend un nouvel agrégat, suivie d'un `update` explicite sur le repository.

## Ports sortants

`SuiviDAtelierRepository`, `JourneeDeTravailRepository`, `ElementsEngageables`, `FonctionsDesOperateurs`, `Clock`.

Tout besoin d'une donnée de paramétrage passe par un nouveau port, jamais par une constante du domaine.

## État d'avancement

Seul `domain/` existe. `application/`, `infrastructure/primary/` et `infrastructure/secondary/` restent à écrire — donc
pas encore de feature Cucumber, faute d'endpoint à appeler.

Le scénario métier de référence, à lire avant toute modification du modèle, est
`src/test/java/com/glm/glmback/atelier/domain/VieDeLAtelierTest.java` : une journée complète jouée de bout en bout, avec
le verbatim client en javadoc de chaque assertion.

Les points encore ouverts sont listés en fin de section `atelier` dans `documentation/contexte-metier.md`.
