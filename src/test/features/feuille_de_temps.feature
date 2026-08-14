Feature: Feuille de temps hebdomadaire d'un operateur

  # L'atelier ne connait ni fuseau horaire ni jour calendaire : une journee de travail y va d'une arrivee a un
  # depart, et rien n'y dit a quel jour appartient une heure. La feuille de temps est le premier contexte a ramener
  # ces instants au calendrier de l'entreprise, semaine par semaine et jour par jour.
  #
  # Les heures des scenarios sont en UTC, l'entreprise lit ses jours a Paris : en mai, 8h locales font 06:00Z.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And la feuille de temps suit l'operateur "dupont"

  Scenario: Une semaine sans pointage rend sept jours vides
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la feuille de temps porte les jours
      | 2026-05-11 |
      | 2026-05-12 |
      | 2026-05-13 |
      | 2026-05-14 |
      | 2026-05-15 |
      | 2026-05-16 |
      | 2026-05-17 |
    And la feuille de temps ne porte aucune presence

  Scenario: Une journee avec pause de midi se lit dans le jour qui la porte
    Given "dupont" est arrive a "2026-05-11T06:00:00Z"
    And "dupont" a pointe "PAUSE" a "2026-05-11T10:00:00Z"
    And "dupont" a pointe "REPRISE" a "2026-05-11T11:00:00Z"
    And "dupont" a pointe "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    # La pause n'ote que son propre creux : la journee se lit en deux fenetres, le meme jour.
    And la presence du "2026-05-11" est
      | debut                | fin                  |
      | 2026-05-11T06:00:00Z | 2026-05-11T10:00:00Z |
      | 2026-05-11T11:00:00Z | 2026-05-11T15:00:00Z |
    And la presence du "2026-05-12" est vide

  Scenario: Une equipe de nuit compte sur les deux jours qu'elle traverse
    Given "dupont" est arrive a "2026-05-13T20:00:00Z"
    And "dupont" a pointe "DEPART" a "2026-05-14T00:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    # Minuit a Paris, c'est 22:00Z : c'est la que la venue bascule d'un jour a l'autre.
    And la presence du "2026-05-13" est
      | debut                | fin                  |
      | 2026-05-13T20:00:00Z | 2026-05-13T22:00:00Z |
    And la presence du "2026-05-14" est
      | debut                | fin                  |
      | 2026-05-13T22:00:00Z | 2026-05-14T00:00:00Z |

  Scenario: Une journee sans depart reste ouverte, sur son seul jour
    Given "dupont" est arrive a "2026-05-15T06:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la presence du "2026-05-15" commence a "2026-05-15T06:00:00Z" et n'est pas terminee
    And la presence du "2026-05-16" est vide

  Scenario: Une feuille de temps ne se lit pas pour un operateur inconnu
    When je consulte la feuille de temps de l'operateur "11111111-2222-3333-4444-555555555555" pour la semaine 20 de 2026
    Then la reponse a le statut http 404

  Scenario: Une semaine hors bornes est refusee
    When je consulte la feuille de temps de "dupont" pour la semaine 54 de 2026
    Then la reponse a le statut http 400
