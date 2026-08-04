Feature: Gestion des elements de fabrication

  Background:
    Given I am logged in as "admin" with role "ADMIN"

  Scenario: Creation d'un ordre de fabrication avec nom fourni
    When je cree un ordre de fabrication avec le corps
      """
      { "nom": "OF-2026-000001", "titre": "Assemblage carter", "description": "Carter en fonte" }
      """
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication contient
      | type        | ORDRE_DE_FABRICATION |
      | nom         | OF-2026-000001       |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |

  Scenario: Creation d'un ordre de fabrication sans nom genere une numerotation automatique
    When je cree un ordre de fabrication avec le corps
      """
      { "titre": "Assemblage carter", "description": "Carter en fonte" }
      """
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication a un nom commencant par "OF-"

  Scenario: Creation d'un produit avec nom fourni
    When je cree un produit avec le corps
      """
      { "nom": "PRD-2026-000001", "titre": "Carter moteur", "description": "Carter en fonte" }
      """
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication contient
      | type | PRODUIT         |
      | nom  | PRD-2026-000001 |

  Scenario: Creation d'un produit sans nom genere une numerotation automatique
    When je cree un produit avec le corps
      """
      { "titre": "Carter moteur", "description": "Carter en fonte" }
      """
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication a un nom commencant par "PRD-"

  Scenario: Lecture d'un ordre de fabrication existant
    Given j'ai cree un ordre de fabrication
    When je consulte cet ordre de fabrication
    Then la reponse a le statut http 200

  Scenario: Lecture d'un ordre de fabrication inexistant renvoie 404
    When je consulte l'ordre de fabrication "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Lecture d'un produit existant
    Given j'ai cree un produit
    When je consulte ce produit
    Then la reponse a le statut http 200

  Scenario: Modification d'un ordre de fabrication existant
    Given j'ai cree un ordre de fabrication
    When je modifie cet ordre de fabrication avec le corps
      """
      { "titre": "Assemblage carter revise", "description": "Carter en fonte usine" }
      """
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | titre | Assemblage carter revise |

  Scenario: Modification d'un produit existant
    Given j'ai cree un produit
    When je modifie ce produit avec le corps
      """
      { "titre": "Carter moteur revise", "description": "Carter en fonte usine" }
      """
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | titre | Carter moteur revise |

  Scenario: Modification d'un produit inexistant renvoie 404
    When je modifie le produit "11111111-1111-1111-1111-111111111111" avec le corps
      """
      { "titre": "Carter moteur revise", "description": "Carter en fonte usine" }
      """
    Then la reponse a le statut http 404

  Scenario: Suppression d'un ordre de fabrication existant
    Given j'ai cree un ordre de fabrication
    When je supprime cet ordre de fabrication
    Then la reponse a le statut http 204
    When je consulte cet ordre de fabrication
    Then la reponse a le statut http 404

  Scenario: Suppression d'un produit existant
    Given j'ai cree un produit
    When je supprime ce produit
    Then la reponse a le statut http 204
    When je consulte ce produit
    Then la reponse a le statut http 404

  Scenario: Suppression d'un produit inexistant renvoie 404
    When je supprime le produit "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Liste paginee sur une periode contenant les deux types d'elements
    Given j'ai cree un ordre de fabrication
    And j'ai cree un produit
    When je liste les elements de fabrication entre "2000-01-01T00:00:00Z" et "2100-01-01T00:00:00Z"
    Then la reponse a le statut http 200
    And la reponse contient au moins 2 elements

  Scenario: Creation refusee a un utilisateur sans role admin
    Given I am logged in as "user" with role "USER"
    When je cree un ordre de fabrication avec le corps
      """
      { "titre": "Assemblage carter", "description": "Carter en fonte" }
      """
    Then la reponse a le statut http 403

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai cree un ordre de fabrication
    Given I am logged in as "user" with role "USER"
    When je consulte cet ordre de fabrication
    Then la reponse a le statut http 200

  Scenario: Creation refusee si le titre est vide
    When je cree un ordre de fabrication avec le corps
      """
      { "titre": "", "description": "Carter en fonte" }
      """
    Then la reponse a le statut http 400
