# Bounded context `coutderevient`

Les règles communes sont dans [AGENTS.md](../../../../../../../AGENTS.md), la justification métier dans
[contexte-metier.md](../../../../../../../documentation/contexte-metier.md).

## Responsabilité et frontières

Lire le coût d'un élément de fabrication, tous ses passages en atelier confondus, une ligne par nature.
Le contexte possède son modèle de lecture et ses entités JPA `@Immutable`, sans table ni écriture.
La capture, la régularisation et l'interprétation des faits appartiennent à `atelier` : lire ses projections,
sans importer son domaine ni rejouer son journal. Les références de l'élément restent relues au référentiel.
Le calendrier appartient aux lecteurs hebdomadaires ; la valorisation utilise les tarifs figés sur les faits.

## Comptabilisation

L'horloge est relevée une seule fois par rapport, même vide. Cet instant est rendu dans `evaluation`.
Les faits connus restent lus, même postérieurs à cet instant. La transaction de lecture ne garantit pas un
instantané commun face aux écritures concurrentes. Une activité avec une fin réelle
conserve cette borne. Sans fin, elle est en cours avant son échéance projetée et terminée automatiquement
dès celle-ci, borne incluse, avec sa période dans `finsAutomatiques` pour signaler l'anomalie active.

Une activité en cours est entièrement exclue du temps, des coûts et du diviseur. Le rapport rend son nombre
via `activitesEnCours`. Ne jamais fabriquer une fin à l'instant de lecture pour la valoriser.
Les fins pointées, les régularisations et les clôtures sont déjà interprétées par atelier ; le coût lit leurs
bornes à chaque appel.

Les tarifs viennent du **fait ouvrant** de l'activité, figés à sa saisie ; lire `ouverture_id` pour les tarifs, jamais
le tarif courant du poste ou de l'opérateur.

## Valorisation

- La machine d'une activité terminée coûte son intervalle entier.
- Le partage humain compte les **postes distincts** des activités terminées de cet opérateur, tous éléments
  confondus. Plusieurs activités sur un même poste comptent un poste ; sans poste est une valeur de poste.
- Découper aux changements d'occupation. Bâtir la charge sur l'union du travail valorisé et de l'occupation lue
  pour que chaque tranche valorisée ait un diviseur. Acquérir aussi les activités adjacentes et prolonger
  la couverture jusqu’aux bornes des activités terminées découvertes : couper une fenêtre change ses centimes.
  Aucune borne basse fixe sur le début : une régularisation peut dépasser la durée maximale d'activité.
- Arrondir le coût humain une fois par **fenêtre de partage** (ensemble de postes constant, diviseur connu),
  puis le répartir en centimes entiers : plus forts restes, puis activité commencée la première
  ([ADR 0004](../../../../../../../documentation/adr/0004-split-the-operator-cost-to-the-cent.md)). Arrondir la
  machine une fois par activité. La ligne et le rapport n'additionnent que des montants déjà arrondis.

## Valeurs

Chaque durée et montant porte sa `valeur`, toujours connue. Un taux absent donne zéro, indépendant du diviseur.
Aucun tarif ne doit être inventé.

## Contrat et vérification

`GET /api/couts-de-revient/{elementId}` est réservé au `GESTIONNAIRE` : il expose les tarifs humains.
Les exports (`…/export.xlsx`) mettent en forme ce même `CoutDeRevient` dans un `CompteRenduDuCout`, sous les mêmes
droits : aucun montant recalculé ni ré-arrondi, instants écrits dans le fuseau de l'entreprise. Apache POI reste
confiné à `infrastructure/primary` ([ADR 0013](../../../../../../../documentation/adr/0013-build-the-cost-price-workbook-with-apache-poi.md)).
Chaque ligne rend ses pointages : parts par fenêtre de partage, activités parallèles, anomalies. Les noms (opérateurs, postes, éléments) sont relus aux tables voisines par leurs noms
logiques de colonnes, jamais copiés ; un nom absent laisse l'identifiant seul.
Conserver l'isolation des entreprises, les natures et la ligne sans nature en dernier.
Le service public et les scénarios REST Cucumber vérifient le calcul ; les scénarios écrivent par l'API
atelier puis lisent le coût des activités. Respecter les noms logiques des colonnes d'atelier,
les noms uniques des entités Hibernate et les noms de schémas OpenAPI propres au coût.
