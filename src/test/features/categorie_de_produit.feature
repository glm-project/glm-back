Feature: Categories de produit

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Declaration d'une categorie de produit
    When je declare la categorie de produit "CUCA"
    Then la reponse a le statut http 201
    And la reponse de categorie de produit contient
      | code | CUCA |

  Scenario: Une categorie nouvelle se range en dernier
    Given j'ai declare la categorie de produit "CUCB"
    And j'ai declare la categorie de produit "CUCC"
    When je liste les categories de produit
    Then la reponse a le statut http 200
    And les categories de produit se terminent par "CUCB, CUCC"

  Scenario: Declaration refusee si le code existe deja
    Given j'ai declare la categorie de produit "CUCD"
    When je declare la categorie de produit "CUCD"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:categorie-de-produit:categorie-deja-existante"

  Scenario: Declaration refusee pour un code hors motif
    When je declare la categorie de produit "Moule"
    Then la reponse a le statut http 400

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai declare la categorie de produit "CUCE"
    And I am logged in as "user" with role "USER"
    When je liste les categories de produit
    Then la reponse a le statut http 200

  Scenario: Declaration refusee a un utilisateur simple
    Given I am logged in as "user" with role "USER"
    When je declare la categorie de produit "CUCF"
    Then la reponse a le statut http 403

  Scenario: Declaration refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN"
    When je declare la categorie de produit "CUCG"
    Then la reponse a le statut http 403

  Scenario: Suppression d'une categorie sans produit
    Given j'ai declare la categorie de produit "CUCH"
    When je supprime la categorie de produit "CUCH"
    Then la reponse a le statut http 204
    And la categorie de produit "CUCH" n'est plus listee

  Scenario: Une categorie supprimee peut etre declaree a nouveau
    Given j'ai declare la categorie de produit "CUCI"
    And j'ai supprime la categorie de produit "CUCI"
    When je declare la categorie de produit "CUCI"
    Then la reponse a le statut http 201

  Scenario: Suppression d'une categorie inexistante renvoie 404
    When je supprime la categorie de produit "CUCZZ"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:categorie-de-produit:categorie-introuvable"

  Scenario: Suppression refusee pour un code hors motif
    When je supprime la categorie de produit "cuch"
    Then la reponse a le statut http 400

  # Tant que les elements portent leur type, la categorie PRODUIT est la seule qu'un element puisse occuper.
  Scenario: Suppression refusee si des produits y sont ranges
    Given j'ai declare la categorie de produit "PRODUIT"
    And j'ai cree un element de fabrication
      | type | PRODUIT |
    When je supprime la categorie de produit "PRODUIT"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:categorie-de-produit:categorie-utilisee"

  Scenario: Suppression refusee a un utilisateur simple
    Given j'ai declare la categorie de produit "CUCJ"
    And I am logged in as "user" with role "USER"
    When je supprime la categorie de produit "CUCJ"
    Then la reponse a le statut http 403
