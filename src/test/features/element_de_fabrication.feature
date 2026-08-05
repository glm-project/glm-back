Feature: Gestion des elements de fabrication

  Background:
    Given I am logged in as "admin" with role "ADMIN"

  Scenario: Creation d'un ordre de fabrication avec numerotation automatique du nom
    When je cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication contient
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    And la reponse d'element de fabrication a un nom commencant par "OF-"

  Scenario: Creation d'un produit avec numerotation automatique du nom
    When je cree un element de fabrication
      | type        | PRODUIT         |
      | titre       | Carter moteur   |
      | description | Carter en fonte |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication contient
      | type        | PRODUIT         |
      | titre       | Carter moteur   |
      | description | Carter en fonte |
    And la reponse d'element de fabrication a un nom commencant par "PRD-"

  Scenario: Lecture d'un ordre de fabrication existant
    Given j'ai cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    When je consulte cet element de fabrication
    Then la reponse a le statut http 200

  Scenario: Lecture d'un produit existant
    Given j'ai cree un element de fabrication
      | type        | PRODUIT         |
      | titre       | Carter moteur   |
      | description | Carter en fonte |
    When je consulte cet element de fabrication
    Then la reponse a le statut http 200

  Scenario: Lecture d'un element de fabrication inexistant renvoie 404
    When je consulte l'element de fabrication "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Modification d'un ordre de fabrication existant
    Given j'ai cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    When je modifie cet element de fabrication
      | titre       | Assemblage carter revise |
      | description | Carter en fonte usine    |
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | titre | Assemblage carter revise |

  Scenario: Modification d'un produit existant
    Given j'ai cree un element de fabrication
      | type        | PRODUIT         |
      | titre       | Carter moteur   |
      | description | Carter en fonte |
    When je modifie cet element de fabrication
      | titre       | Carter moteur revise  |
      | description | Carter en fonte usine |
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | titre | Carter moteur revise |

  Scenario: Modification d'un element de fabrication inexistant renvoie 404
    When je modifie l'element de fabrication "11111111-1111-1111-1111-111111111111"
      | titre       | Carter moteur revise  |
      | description | Carter en fonte usine |
    Then la reponse a le statut http 404

  Scenario: Suppression d'un ordre de fabrication existant
    Given j'ai cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    When je supprime cet element de fabrication
    Then la reponse a le statut http 204
    When je consulte cet element de fabrication
    Then la reponse a le statut http 404

  Scenario: Suppression d'un element de fabrication inexistant renvoie 404
    When je supprime l'element de fabrication "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Liste paginee sur une periode contenant les deux types d'elements
    Given j'ai cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    And j'ai cree un element de fabrication
      | type        | PRODUIT         |
      | titre       | Carter moteur   |
      | description | Carter en fonte |
    When je liste les elements de fabrication entre "2000-01-01T00:00:00Z" et "2100-01-01T00:00:00Z"
    Then la reponse a le statut http 200
    And la reponse contient au moins 2 elements

  Scenario: Creation refusee a un utilisateur sans role admin
    Given I am logged in as "user" with role "USER"
    When je cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    Then la reponse a le statut http 403

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       | Assemblage carter    |
      | description | Carter en fonte      |
    Given I am logged in as "user" with role "USER"
    When je consulte cet element de fabrication
    Then la reponse a le statut http 200

  Scenario: Creation refusee si le titre est vide
    When je cree un element de fabrication
      | type        | ORDRE_DE_FABRICATION |
      | titre       |                      |
      | description | Carter en fonte      |
    Then la reponse a le statut http 400
