# Bounded context `syntheseheures`

Les règles communes sont dans [glm-back/AGENTS.md](../../../../../../../AGENTS.md), les raisons métier dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md).

## Responsabilité et vocabulaire

Relever le temps opérationnel d'un opérateur sur une semaine ISO explicite : sept jours du lundi au dimanche,
le journal brut des gestes actifs et les éléments touchés. Cette projection est purement lectrice et ne possède
aucune table. Elle ne calcule aucun montant.

`SyntheseDesHeures` porte l'opérateur résolu, la semaine, les `JourDeSynthese` et les `ElementDeLaSynthese`.
Le jour porte ses pointages et sa durée opérationnelle. L'élément porte son identité engagée, sa fiche relue,
sa durée totale, sa part de non-conformité et ses couples poste/nature.

Atelier possède l'interprétation des gestes et les corrections. `ActiviteInterpretee` reçoit cette interprétation ;
`ActiviteLue` conserve son identité, son état et ses bornes entières après évaluation. `DecoupageCalendaire` produit
les portions dont la synthèse additionne les durées. Le journal brut est lu séparément, sans déduire ses pointages
à partir des activités sélectionnées.

## Invariants

- `ActivitesDeLOperateur` lit `activite_d_atelier` par une entité propre `@Immutable`, jointe au suivi pour
  l'élément engagé. La sélection porte sur le recouvrement de la semaine par le début et la fin réelle ou l'échéance,
  sans borne basse fixe du début : une régularisation peut dépasser 13 h, voire une semaine.
- `SynthesesDesHeuresService` reçoit l'instant facultatif `evaluation`. Sans lui, l'heure du serveur est relevée
  une seule fois ; l'instant utilisé gouverne l'expiration et le découpage des activités en cours, et la réponse
  le rend. Passer le même instant à la feuille et à la synthèse assure la même décision d'expiration.
  Les faits connus restent interprétés, même postérieurs à cet instant : aucune lecture historique ni
  transaction commune entre les appels n'est garantie. Un instant fourni vide ou mal formé répond 400, sans rapport.
- Une activité avec fin réelle est `TERMINEE`, même au-delà de l'échéance. Sans fin, elle est `EN_COURS` avant
  l'échéance puis `TERMINEE_AUTOMATIQUEMENT` à celle-ci, borne incluse. Une activité `A_RESOUDRE` reste sans fin ;
  l'échéance ne lui donne aucune interprétation chiffrée. La restitution des conflits et de la complétude appartient
  à la tranche suivante du relevé.
- Seules les portions terminées contribuent aux durées. Une activité en cours reste portée par son élément sur
  chacun des jours atteints à l'instant de lecture, même sans pointage dans la semaine, sans durée ajoutée.
- Les portions sont coupées aux minuits locaux et aux limites de la semaine. Leurs bornes ne déplacent jamais
  celles de l'activité entière. Le fuseau passe par `FuseauHoraireDeLEntreprise`, actuellement `Europe/Paris`.
- Les durées se cumulent par élément : deux éléments simultanés pendant une heure comptent une heure chacun,
  deux heures dans le jour et la semaine. La NC est incluse une seule fois dans le total et présentée aussi à part.
  Somme des jours = somme des éléments = durée opérationnelle de la semaine.
- `JournalDeLOperateur` rend tous les pointages actifs datés de la semaine, même sans activité interprétable.
  Le tri est total : heure métier, rang `FIN < TRANSITION < OUVERTURE`, identité du pointage. L'heure
  d'enregistrement et l'identité de l'élément ne départagent jamais les gestes simultanés.
- Tout élément portant une portion d'activité ou un pointage dans la semaine est rendu, par première apparition
  puis nom. Un réengagement après clôture conserve un seul élément. Nom et type viennent du suivi ; référence,
  description et libellé du poste sont relus au référentiel. La fiche peut être absente.
- Les couples poste/nature suivent leur première apparition, activité et pointage confondus. Tout poste nommé
  au journal retrouve son libellé dans l'élément. L'absence de poste ou de nature est un cas nominal.
- Les sept jours sont toujours rendus, vides compris. Les règles de validation de la semaine et de l'opérateur,
  les rôles `USER`/`GESTIONNAIRE` et l'isolation des entreprises s'appliquent à la lecture.
- Aucun import des contextes `atelier`, `feuilledetemps`, `elementdefabrication`, `operateur` ou `postedetravail` :
  chacun possède son code. Les adapters de synthèse déclarent leurs propres entités immuables et leurs noms de beans.

## Couture et ports

`ActivitesDeLOperateur`, `JournalDeLOperateur`, `OperateursConnus`, `FuseauHoraireDeLEntreprise`,
`ElementsDeFabrication`, `PostesDeTravail` et `Clock` sont les ports sortants.
Les activités sont lues en une requête ; le journal en deux requêtes groupées, jamais une par suivi ; les fiches
et les postes sont chacun lus par lots.

`src/test/features/synthese_des_heures.feature` écrit réellement par l'API d'atelier avec
`EcrituresDuJournalDAtelier`, puis lit la synthèse. Les tableaux d'activité sont parallèles à ceux de
`feuille_de_temps.feature` : mêmes heures et mêmes décisions pour relances, transitions, fins tardives,
régularisations, corrections, annulations et clôtures. `DecoupageCalendaireTest` conserve les preuves de semaines ISO,
minuit et changements d'heure. Les textes de steps Cucumber doivent rester uniques dans tout le dépôt.
