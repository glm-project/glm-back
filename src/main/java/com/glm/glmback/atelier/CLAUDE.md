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

Les quatre couches existent. L'API REST est décrite par OpenAPI (`/swagger-ui.html`) et par
[documentation/atelier-api.md](../../../../../../documentation/atelier-api.md), le guide d'intégration du développeur
front — le tenir à jour avec le contrat.

`infrastructure/secondary/` persiste en PostgreSQL, dans le schéma de l'entreprise courante :

- `JpaSuiviDAtelierRepository` et `JpaJourneeDeTravailRepository` écrivent chacun leur agrégat sur deux tables — la
  ligne de l'agrégat et son journal —, l'agrégat étant reconstruit en entier par le domaine mais **rapproché par
  identifiant** côté persistance : un pointage coûte l'insertion d'une ligne, jamais la réécriture du journal ;
- `ElementsDeFabricationEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à
  l'atelier : aucun import de `elementdefabrication`, l'invariant tient ;
- `FonctionsDesOperateursInconnues` rend toujours vide, faute de référentiel des ressources. La nature étant
  facultative par invariant, c'est un port en attente de sa source, pas un cas dégradé.

### Les colonnes de projection ne contredisent pas « le journal est la source de vérité »

`suivi_d_atelier.etat`, `journee_de_travail.etat`, `.debut` et `.fin` sont **dérivées du journal, écrites depuis le
domaine à chaque enregistrement, et jamais relues** : `toDomain()` rejoue toujours le journal et ignore ces colonnes.
Ce sont des index, pas un état stocké — sans eux, filtrer l'écran d'atelier sur `etats` ou retrouver la journée
contenant un instant obligerait à ramener toute l'entreprise en mémoire à chaque lecture de temps effectif. Elles ne
dépendent que du journal, jamais de l'instant courant, donc restent justes entre deux écritures.

Leur contrepartie : `SuiviDAtelierCriteria.matches` et `JourneeDeTravailCriteria.matches` ne sont plus appelées par la
production, qui traduit les mêmes règles en SQL. C'est `PariteDesRepositoriesDAtelierIT` qui rétablit par l'exécution
la garantie que donnait le code partagé — le modifier en même temps que l'une des deux expressions de la règle.

### Concurrence

Deux saisies parties du même état valideraient chacune sa transition contre un journal qui ignore l'autre, et le
journal obtenu — deux débuts consécutifs sur la même activité — deviendrait illisible à chaque relecture. `update`
charge donc l'agrégat sous verrou pessimiste, puis refuse par `SaisieConcurrenteException` (409) toute saisie dont le
journal ignore un événement déjà stocké. Un `@Version` n'aurait rien protégé : la collection d'événements est le côté
inverse de l'association, donc l'insertion d'un événement ne salit pas la ligne parente et n'incrémente aucune version.

L'`Auteur` d'une saisie vient toujours du jeton (`AuteurConnecte`), jamais du corps de la requête ; l'`Operateur`, lui,
reste dans le corps.

Deux scénarios métier de référence, à lire avant toute modification du modèle :

- `src/test/java/com/glm/glmback/atelier/domain/VieDeLAtelierTest.java` — une journée complète en appels directs, avec
  le verbatim client en javadoc de chaque assertion ;
- `src/test/features/atelier_suivi.feature` — la même journée rejouée en HTTP, avec `atelier_presence.feature` pour la
  présence seule.

Les scénarios pilotent l'horloge (`CucumberClock`, step `Given il est "..."`). Deux pièges s'y rappellent seuls :
**faire avancer l'horloge entre deux événements** — à horodatage identique, l'ordre du journal se départage sur
l'identifiant, donc au hasard — et **ne jamais corriger vers une date postérieure à l'instant courant**, que
`Horodatage` refuse.

Les points encore ouverts sont listés en fin de section `atelier` dans `documentation/contexte-metier.md`.
