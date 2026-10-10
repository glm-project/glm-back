Feature: Export du cout de revient d'un element de fabrication

  # Les exports mettent en forme le rapport que l'ecran lit : aucun montant n'y est recalcule. Le classeur Excel est
  # une source de donnees pour le client : nombres, vraies dates, tableaux nommes.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And le rapport connait le poste "fraiseuse" de nature "fraisage" a "45.00" de l'heure
    And le rapport connait l'operateur "dupont" a "20.00" de l'heure, habilite sur
      | fraiseuse |

  Scenario: Le classeur reprend la synthese de l'ecran, a l'heure de l'entreprise
    Given l'entreprise fabrique "OF 4001"
    And "OF 4001" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 4001" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 4001" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 4001" sans poste a "2026-05-11T13:00:00Z"
    And "dupont" pointe "FIN" sur "OF 4001" sans poste a "2026-05-11T14:00:00Z"
    When j'exporte en Excel le cout de revient de "OF 4001" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le classeur recu est nomme d'apres "OF 4001"
    # 18 h UTC, 20 h a Paris en mai.
    And le classeur est genere le "2026-05-11T20:00", heure de l'entreprise
    And le classeur dit l'element "En cours"
    And le tableau "Natures" du classeur porte
      | Nature     | Travail (h) | Non-conformité (h) | Temps total (h) | Machine (€) | Main d’œuvre (€) | Total (€) |
      | fraisage   | 2.00        | 0.00               | 2.00            | 90.00       | 40.00            | 130.00    |
      | Sans poste | 1.00        | 0.00               | 1.00            | 0.00        | 20.00            | 20.00     |
    And le tableau "Pointages" du classeur porte
      | Nature     | Poste      | Opérateur | Catégorie | Début            | Fin              | Durée (h) | Machine (€) | Main d’œuvre (€) | Total (€) |
      | fraisage   | fraiseuse  | dupont    | Travail   | 2026-05-11T11:00 | 2026-05-11T13:00 | 2.00      | 90.00       | 40.00            | 130.00    |
      | Sans poste | Sans poste | dupont    | Travail   | 2026-05-11T15:00 | 2026-05-11T16:00 | 1.00      | 0.00        | 20.00            | 20.00     |

  Scenario: Le classeur d'un element clos dit quand il a ete termine
    Given l'entreprise fabrique "OF 4003"
    And "OF 4003" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 4003" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 4003" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    And "OF 4003" est cloture a "2026-05-11T15:05:00Z"
    When j'exporte en Excel le cout de revient de "OF 4003" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le classeur dit l'element "Terminé le 11 mai 2026 à 17:05"

  Scenario: Un operateur n'exporte pas les couts
    Given l'entreprise fabrique "OF 4002"
    And I am logged in as "user" with role "USER"
    When j'exporte en Excel le cout de revient de "OF 4002" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 403
