Feature: Gestion des elements de fabrication

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Creation d'un ordre de fabrication avec numerotation automatique du nom
    When je cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1001            |
      | description | Carter en fonte |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication contient
      | categorie   | OF              |
      | reference   | 1001            |
      | description | Carter en fonte |
    And la reponse d'element de fabrication a un nom commencant par "OF-"

  Scenario: Creation d'un produit avec numerotation automatique du nom
    When je cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1002            |
      | description | Carter en fonte |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication contient
      | categorie   | MOULE           |
      | reference   | 1002            |
      | description | Carter en fonte |
    And la reponse d'element de fabrication a un nom commencant par "MOULE-"

  Scenario: Creation d'un produit reduit a son seul numero
    When je cree un element de fabrication
      | categorie | MOULE |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication a un nom commencant par "MOULE-"
    And la reponse d'element de fabrication n'a ni reference ni description

  Scenario: Plusieurs elements de fabrication peuvent rester sans reference
    Given j'ai cree un element de fabrication
      | categorie | MOULE |
    When je cree un element de fabrication
      | categorie | OF |
    Then la reponse a le statut http 201

  Scenario: Creation refusee si la reference est deja utilisee
    Given j'ai cree un element de fabrication
      | categorie | MOULE |
      | reference | 1003  |
    When je cree un element de fabrication
      | categorie | OF   |
      | reference | 1003 |
    Then la reponse a le statut http 409

  Scenario: Creation refusee dans une categorie que l'entreprise n'a pas declaree
    When je cree un element de fabrication sans declarer sa categorie
      | categorie | ARTICLE |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:element-de-fabrication:categorie-inconnue"

  Scenario: Creation refusee pour une categorie hors motif
    When je cree un element de fabrication sans declarer sa categorie
      | categorie | ORDRE_DE_FABRICATION |
    Then la reponse a le statut http 400

  Scenario: Chaque categorie a sa propre numerotation, prefixee de son code
    Given j'ai declare la categorie de produit "PIECE"
    When je cree un element de fabrication
      | categorie | PIECE |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication a un nom commencant par "PIECE-"

  Scenario: Creation refusee a un client qui n'envoie que l'ancien type d'element
    When je cree un element de fabrication sans declarer sa categorie
      | type | ORDRE_DE_FABRICATION |
    Then la reponse a le statut http 400

  Scenario: Creation refusee si la categorie est absente
    When je cree un element de fabrication
      | reference | 1004 |
    Then la reponse a le statut http 400

  Scenario: Lecture d'un ordre de fabrication existant
    Given j'ai cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1005            |
      | description | Carter en fonte |
    When je consulte cet element de fabrication
    Then la reponse a le statut http 200

  Scenario: Lecture d'un produit existant
    Given j'ai cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1006            |
      | description | Carter en fonte |
    When je consulte cet element de fabrication
    Then la reponse a le statut http 200

  Scenario: Lecture d'un element de fabrication inexistant renvoie 404
    When je consulte l'element de fabrication "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Modification d'un ordre de fabrication existant
    Given j'ai cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1007            |
      | description | Carter en fonte |
    When je modifie cet element de fabrication
      | reference   | 1008                  |
      | description | Carter en fonte usine |
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | reference | 1008 |

  Scenario: Modification d'un produit existant
    Given j'ai cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1009            |
      | description | Carter en fonte |
    When je modifie cet element de fabrication
      | reference   | 1010                  |
      | description | Carter en fonte usine |
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | reference | 1010 |

  Scenario: Modification conservant la reference existante
    Given j'ai cree un element de fabrication
      | categorie | MOULE |
      | reference | 1011  |
    When je modifie cet element de fabrication
      | reference   | 1011                  |
      | description | Carter en fonte usine |
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication contient
      | reference   | 1011                  |
      | description | Carter en fonte usine |

  Scenario: Modification refusee si la reference appartient a un autre element
    Given j'ai cree un element de fabrication
      | categorie | MOULE |
      | reference | 1012  |
    And j'ai cree un element de fabrication
      | categorie | OF   |
      | reference | 1013 |
    When je modifie cet element de fabrication
      | reference | 1012 |
    Then la reponse a le statut http 409

  Scenario: Modification qui retire la reference et la description
    Given j'ai cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1014            |
      | description | Carter en fonte |
    When je modifie cet element de fabrication sans reference ni description
    Then la reponse a le statut http 200
    And la reponse d'element de fabrication n'a ni reference ni description

  Scenario: Modification d'un element de fabrication inexistant renvoie 404
    When je modifie l'element de fabrication "11111111-1111-1111-1111-111111111111"
      | reference   | 1015                  |
      | description | Carter en fonte usine |
    Then la reponse a le statut http 404

  Scenario: Suppression d'un ordre de fabrication existant
    Given j'ai cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1016            |
      | description | Carter en fonte |
    When je supprime cet element de fabrication
    Then la reponse a le statut http 204
    When je consulte cet element de fabrication
    Then la reponse a le statut http 404

  Scenario: Suppression d'un element de fabrication inexistant renvoie 404
    When je supprime l'element de fabrication "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Liste paginee sur une periode contenant des elements de deux categories
    Given j'ai cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1017            |
      | description | Carter en fonte |
    And j'ai cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1018            |
      | description | Carter en fonte |
    When je liste les elements de fabrication entre "2000-01-01T00:00:00Z" et "2100-01-01T00:00:00Z"
    Then la reponse a le statut http 200
    And la reponse contient au moins 2 elements

  Scenario: Creation refusee a un utilisateur sans role gestionnaire
    Given I am logged in as "user" with role "USER"
    When je cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1019            |
      | description | Carter en fonte |
    Then la reponse a le statut http 403

  Scenario: Creation refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN"
    When je cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1025            |
      | description | Carter en fonte |
    Then la reponse a le statut http 403

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai cree un element de fabrication
      | categorie   | OF              |
      | reference   | 1020            |
      | description | Carter en fonte |
    Given I am logged in as "user" with role "USER"
    When je consulte cet element de fabrication
    Then la reponse a le statut http 200

  Scenario: Un element de fabrication n'est pas visible depuis une autre entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    And j'ai cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1021            |
      | description | Carter en fonte |
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte cet element de fabrication
    Then la reponse a le statut http 404

  Scenario: Chaque entreprise a sa propre numerotation
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1022            |
      | description | Carter en fonte |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication a un nom commencant par "MOULE-"
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je cree un element de fabrication
      | categorie   | MOULE           |
      | reference   | 1023            |
      | description | Carter en fonte |
    Then la reponse a le statut http 201
    And la reponse d'element de fabrication a un nom commencant par "MOULE-"

  Scenario: Deux entreprises peuvent utiliser la meme reference
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je cree un element de fabrication
      | categorie | MOULE |
      | reference | 1024  |
    Then la reponse a le statut http 201
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je cree un element de fabrication
      | categorie | MOULE |
      | reference | 1024  |
    Then la reponse a le statut http 201

  Scenario: Acces refuse a un utilisateur sans entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" without tenant
    When je liste les elements de fabrication entre "2000-01-01T00:00:00Z" et "2100-01-01T00:00:00Z"
    Then la reponse a le statut http 403
