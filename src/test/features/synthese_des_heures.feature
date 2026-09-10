Feature: Synthese des heures hebdomadaire d'un operateur

  # Meme couture que la feuille de temps : l'atelier ne connait ni fuseau horaire ni jour calendaire, donc c'est ici
  # que les instants du journal de presence sont ramenes au calendrier de l'entreprise, semaine par semaine et jour
  # par jour. La difference porte sur ce que ce contexte ajoute a la feuille de temps : le journal brut des
  # pointages plutot que des fenetres repliees, et une duree travaillee calculee par jour et pour la semaine.
  #
  # Le releve sait aussi absorber un pointage qui casse l'automate de presence (il reste visible, marque invalide,
  # sans jamais empecher la lecture) — mais ce cas n'a pas de scenario ici : atelier valide tout le journal a
  # chaque ecriture, y compris une annulation, et refuse deja celle qui laisserait un pointage orphelin. Cette
  # resilience reste donc verifiee uniquement au niveau domaine (SyntheseDesHeuresServiceTest,
  # JourneeDeTravailTest), en defense en profondeur plutot que sur un cas atteignable aujourd'hui par l'API.
  #
  # Les heures des scenarios sont en UTC, l'entreprise lit ses jours a Paris : en mai, 8h locales font 06:00Z.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And la synthese des heures suit l'operateur "dupont"

  Scenario: Une semaine sans pointage rend sept jours vides
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la synthese des heures porte les jours
      | 2026-05-11 |
      | 2026-05-12 |
      | 2026-05-13 |
      | 2026-05-14 |
      | 2026-05-15 |
      | 2026-05-16 |
      | 2026-05-17 |
    And chaque jour de la synthese ne porte aucun pointage et une duree de "PT0S"
    And la duree totale de la semaine est "PT0S"
    And la synthese ne porte aucune anomalie

  Scenario: Une journee avec pause de midi porte son journal brut et sa duree
    Given "dupont" pointe son arrivee a "2026-05-11T06:00:00Z"
    And "dupont" enregistre le pointage "PAUSE" a "2026-05-11T10:00:00Z"
    And "dupont" enregistre le pointage "REPRISE" a "2026-05-11T11:00:00Z"
    And "dupont" enregistre le pointage "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And les pointages du "2026-05-11" sont
      | type    | dateDeSurvenue       | valide |
      | ARRIVEE | 2026-05-11T06:00:00Z | true   |
      | PAUSE   | 2026-05-11T10:00:00Z | true   |
      | REPRISE | 2026-05-11T11:00:00Z | true   |
      | DEPART  | 2026-05-11T15:00:00Z | true   |
    # La pause de midi n'ote que son propre creux : 4h + 4h, jamais l'amplitude de 9h.
    And le jour "2026-05-11" a une duree de "PT8H"
    And le jour "2026-05-11" ne porte aucune anomalie

  Scenario: Une equipe de nuit repartit sa duree sur les deux jours qu'elle traverse
    Given "dupont" pointe son arrivee a "2026-05-13T20:00:00Z"
    And "dupont" enregistre le pointage "DEPART" a "2026-05-14T00:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    # Minuit a Paris, c'est 22:00Z : c'est la que la duree bascule d'un jour a l'autre, comme les pointages restent
    # sur leur jour propre.
    And le jour "2026-05-13" a une duree de "PT2H"
    And le jour "2026-05-14" a une duree de "PT2H"

  Scenario: Une journee sans depart ne compte aucune duree, sans faire disparaitre le pointage
    Given "dupont" pointe son arrivee a "2026-05-15T06:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And les pointages du "2026-05-15" sont
      | type    | dateDeSurvenue       | valide |
      | ARRIVEE | 2026-05-15T06:00:00Z | true   |
    And le jour "2026-05-15" a une duree de "PT0S"

  Scenario: Une synthese ne se lit pas pour un operateur inconnu
    When je consulte la synthese des heures de l'operateur "11111111-2222-3333-4444-555555555555" pour la semaine 20 de 2026
    Then la reponse a le statut http 404

  Scenario: Une semaine hors bornes est refusee
    When je consulte la synthese des heures de "dupont" pour la semaine 54 de 2026
    Then la reponse a le statut http 400
