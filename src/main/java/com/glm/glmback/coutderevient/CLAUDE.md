# Bounded context `coutderevient`

Les règles communes sont dans [AGENTS.md](../../../../../../../AGENTS.md), la justification métier dans
[contexte-metier.md](../../../../../../../documentation/contexte-metier.md).

## Responsabilité et frontières

Lire le coût d'un élément de fabrication, tous ses passages en atelier confondus, une ligne par nature.
Le contexte possède son modèle de lecture et ses entités JPA `@Immutable`, sans table ni écriture.
La capture, les corrections et l'interprétation des faits appartiennent à `atelier` : lire ses projections,
sans importer son domaine ni rejouer son journal. Les références de l'élément restent relues au référentiel.
Aucun calendrier, calcul de paie ni tarif courant dans ce contexte.

## Comptabilisation

L'horloge est relevée une seule fois par rapport, même vide. Cet instant est rendu dans `evaluation`.
Les faits connus restent lus ; seule l'expiration dépend de cet instant. Une activité avec une fin réelle
conserve cette borne. Sans fin, elle est en cours avant son échéance projetée et terminée automatiquement
dès celle-ci, avec sa période dans `finsAutomatiques` pour signaler l'anomalie active.

Une activité en cours est entièrement exclue du temps, des coûts et du diviseur. Le rapport rend son nombre
via `activitesEnCours`. Ne jamais fabriquer une fin à l'instant de lecture pour la valoriser.
Les transitions, fins reçues tardivement, régularisations, corrections, annulations et clôtures sont déjà
interprétées par atelier ; le coût lit leurs nouvelles bornes à chaque appel.

Les tarifs viennent du **fait ouvrant actif** de l'activité, figés à sa saisie. Son identité reste celle de
l'ouverture d'origine après correction ; lire `ouverture_id` pour les tarifs, jamais le tarif courant du poste
ou de l'opérateur, ni un ancien ouvrant annulé.

## Valorisation

- La machine d'une activité terminée coûte son intervalle entier.
- Le partage humain compte les **postes distincts** des activités terminées de cet opérateur, tous éléments
  confondus. Plusieurs activités sur un même poste comptent un poste ; sans poste est une valeur de poste.
- Découper aux changements d'occupation. Bâtir la charge sur l'union du travail valorisé et de l'occupation lue
  pour que chaque tranche valorisée ait un diviseur. Sélectionner l'occupation par recouvrement, sans borne
  basse fixe sur le début : une régularisation peut dépasser treize heures.
- Additionner les tranches à l'échelle de travail, arrondir au centime une seule fois par ligne, puis sommer
  les lignes déjà arrondies dans le rapport.

## Contrat et vérification

`GET /api/couts-de-revient/{elementId}` est réservé au `GESTIONNAIRE` : il expose les tarifs humains.
Conserver l'isolation des entreprises, les natures et la ligne sans nature en dernier.
Le service public et les scénarios REST Cucumber vérifient le calcul ; les scénarios écrivent par l'API
atelier puis lisent le coût, sans présence préalable. Respecter les noms logiques des colonnes d'atelier,
les noms uniques des entités Hibernate et les noms de schémas OpenAPI propres au coût.
