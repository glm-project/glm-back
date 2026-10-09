Feature: Natures de travail

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Declaration d'une nature de travail
    When je declare la nature de travail "Soudage CNA"
    Then la reponse a le statut http 201
    And la reponse de nature de travail contient
      | libelle | Soudage CNA |
    And la nature de travail declaree est libre

  Scenario: Le libelle est declare sans les espaces qui l'entourent
    When je declare la nature de travail "  Tournage CNB  "
    Then la reponse a le statut http 201
    And la reponse de nature de travail contient
      | libelle | Tournage CNB |

  Scenario Outline: Declaration refusee si une nature porte deja ce libelle a la casse ou aux accents pres
    Given j'ai declare la nature de travail "<existante>"
    When je declare la nature de travail "<doublon>"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:nature-de-travail:nature-deja-existante"

    Examples:
      | existante     | doublon       |
      | Fraisage CNC  | FRAISAGE CNC  |
      | Ébavurage CND | ebavurage cnd |

  Scenario: Declaration refusee sans libelle
    When je declare la nature de travail "   "
    Then la reponse a le statut http 400

  Scenario: Declaration refusee au-dela de cinquante caracteres
    When je declare la nature de travail "Une nature de travail dont le libelle est bien trop long"
    Then la reponse a le statut http 400

  Scenario: La liste suit l'ordre alphabetique sans egard aux accents ni a la casse
    Given j'ai declare la nature de travail "fraisage CNE"
    And j'ai declare la nature de travail "Électro-érosion CNE"
    And j'ai declare la nature de travail "Dessin CNE"
    When je liste les natures de travail
    Then la reponse a le statut http 200
    And les natures de travail listees comprennent dans l'ordre "Dessin CNE, Électro-érosion CNE, fraisage CNE"

  Scenario: Lecture autorisee a un utilisateur simple
    Given I am logged in as "user" with role "USER"
    When je liste les natures de travail
    Then la reponse a le statut http 200

  Scenario: Declaration refusee a un utilisateur simple
    Given I am logged in as "user" with role "USER"
    When je declare la nature de travail "Soudage CNF"
    Then la reponse a le statut http 403

  Scenario: Declaration refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN"
    When je declare la nature de travail "Soudage CNG"
    Then la reponse a le statut http 403

  Scenario: Cinquante caracteres entoures d'espaces sont acceptes
    When je declare la nature de travail "  Une nature au libelle de cinquante caracteres CNHI  "
    Then la reponse a le statut http 201

  Scenario: Renommage d'une nature de travail
    Given j'ai declare la nature de travail "Soudure CNJ"
    When je renomme la nature de travail "Soudure CNJ" en "Soudage CNJ"
    Then la reponse a le statut http 200
    And la reponse de nature de travail contient
      | libelle | Soudage CNJ |
    And la nature de travail "Soudure CNJ" est listee sous le libelle "Soudage CNJ"

  Scenario: Une nature peut changer la casse et les accents de son libelle
    Given j'ai declare la nature de travail "electro-erosion CNK"
    When je renomme la nature de travail "electro-erosion CNK" en "Électro-érosion CNK"
    Then la reponse a le statut http 200
    And la nature de travail "electro-erosion CNK" est listee sous le libelle "Électro-érosion CNK"

  Scenario: Renommage refuse si une autre nature porte deja ce libelle a la casse ou aux accents pres
    Given j'ai declare la nature de travail "Fraisage CNL"
    And j'ai declare la nature de travail "Tournage CNL"
    When je renomme la nature de travail "Tournage CNL" en "FRAISÂGE CNL"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:nature-de-travail:nature-deja-existante"
    And la nature de travail "Tournage CNL" est listee sous le libelle "Tournage CNL"

  Scenario: Renommage d'une nature inexistante renvoie 404
    When je renomme une nature de travail inconnue en "Soudage CNM"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:nature-de-travail:nature-introuvable"

  Scenario: Renommage refuse sans libelle
    Given j'ai declare la nature de travail "Dessin CNN"
    When je renomme la nature de travail "Dessin CNN" en " "
    Then la reponse a le statut http 400

  Scenario: Renommage refuse a un utilisateur simple
    Given j'ai declare la nature de travail "Dessin CNO"
    And I am logged in as "user" with role "USER"
    When je renomme la nature de travail "Dessin CNO" en "Dessin industriel CNO"
    Then la reponse a le statut http 403
