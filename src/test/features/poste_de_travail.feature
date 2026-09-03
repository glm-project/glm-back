Feature: Referentiel des postes de travail

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Declaration d'un poste de travail avec sa nature
    When je declare un poste de travail
      | libelle | Tour 1   |
      | nature  | tournage |
    Then la reponse a le statut http 201
    And la reponse de poste de travail contient
      | libelle | Tour 1   |
      | nature  | tournage |

  Scenario: Declaration d'un poste de travail avec son cout horaire
    When je declare un poste de travail
      | libelle     | Tour 18  |
      | nature      | tournage |
      | coutHoraire | 45.5     |
    Then la reponse a le statut http 201
    And la reponse de poste de travail contient
      | coutHoraire | 45.5 |

  Scenario: Revision du cout horaire d'un poste de travail
    Given j'ai declare un poste de travail
      | libelle     | Tour 19  |
      | nature      | tournage |
      | coutHoraire | 40       |
    When je revise ce poste de travail
      | libelle     | Tour 19  |
      | nature      | tournage |
      | coutHoraire | 55.5     |
    Then la reponse a le statut http 200
    And la reponse de poste de travail contient
      | coutHoraire | 55.5 |

  Scenario: Declaration refusee si le libelle est deja utilise
    Given j'ai declare un poste de travail
      | libelle | Tour 2   |
      | nature  | tournage |
    When je declare un poste de travail
      | libelle | Tour 2  |
      | nature  | soudage |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:poste-de-travail:libelle-deja-utilise"

  Scenario: Declaration refusee sans libelle
    When je declare un poste de travail
      | nature | tournage |
    Then la reponse a le statut http 400

  Scenario: Declaration refusee sans nature
    When je declare un poste de travail
      | libelle | Tour 3 |
    Then la reponse a le statut http 400

  Scenario: Lecture d'un poste de travail existant
    Given j'ai declare un poste de travail
      | libelle | Tour 4   |
      | nature  | tournage |
    When je consulte ce poste de travail
    Then la reponse a le statut http 200
    And la reponse de poste de travail contient
      | libelle | Tour 4   |
      | nature  | tournage |

  Scenario: Lecture d'un poste de travail inexistant renvoie 404
    When je consulte le poste de travail "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:poste-de-travail:poste-de-travail-introuvable"

  Scenario: Revision d'un poste de travail
    Given j'ai declare un poste de travail
      | libelle | Tour 5   |
      | nature  | tournage |
    When je revise ce poste de travail
      | libelle | Tour 5 renove |
      | nature  | fraisage      |
    Then la reponse a le statut http 200
    And la reponse de poste de travail contient
      | libelle | Tour 5 renove |
      | nature  | fraisage      |

  Scenario: Revision conservant le libelle existant
    Given j'ai declare un poste de travail
      | libelle | Tour 6   |
      | nature  | tournage |
    When je revise ce poste de travail
      | libelle | Tour 6   |
      | nature  | fraisage |
    Then la reponse a le statut http 200
    And la reponse de poste de travail contient
      | libelle | Tour 6   |
      | nature  | fraisage |

  Scenario: Revision refusee si le libelle appartient a un autre poste
    Given j'ai declare un poste de travail
      | libelle | Tour 7   |
      | nature  | tournage |
    And j'ai declare un poste de travail
      | libelle | Tour 8   |
      | nature  | tournage |
    When je revise ce poste de travail
      | libelle | Tour 7   |
      | nature  | tournage |
    Then la reponse a le statut http 409

  Scenario: Revision d'un poste de travail inexistant renvoie 404
    When je revise le poste de travail "11111111-1111-1111-1111-111111111111"
      | libelle | Tour 9   |
      | nature  | tournage |
    Then la reponse a le statut http 404

  Scenario: Suppression d'un poste de travail libre
    Given j'ai declare un poste de travail
      | libelle | Tour 10  |
      | nature  | tournage |
    When je supprime ce poste de travail
    Then la reponse a le statut http 204
    When je consulte ce poste de travail
    Then la reponse a le statut http 404

  Scenario: Suppression d'un poste de travail inexistant renvoie 404
    When je supprime le poste de travail "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Liste des postes de travail
    Given j'ai declare un poste de travail
      | libelle | Tour 11  |
      | nature  | tournage |
    And j'ai declare un poste de travail
      | libelle | Poste de soudure 11 |
      | nature  | soudage             |
    When je liste les postes de travail
    Then la reponse a le statut http 200
    And la reponse contient au moins 2 postes de travail

  Scenario: Liste filtree par nature
    Given j'ai declare un poste de travail
      | libelle | Erodeuse 1 |
      | nature  | erosion    |
    And j'ai declare un poste de travail
      | libelle | Tour 12  |
      | nature  | tournage |
    When je liste les postes de travail de nature "erosion"
    Then la reponse a le statut http 200
    And la reponse ne contient que des postes de travail de nature "erosion"

  Scenario: Declaration refusee a un utilisateur simple
    Given I am logged in as "user" with role "USER"
    When je declare un poste de travail
      | libelle | Tour 13  |
      | nature  | tournage |
    Then la reponse a le statut http 403

  Scenario: Declaration refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN"
    When je declare un poste de travail
      | libelle | Tour 14  |
      | nature  | tournage |
    Then la reponse a le statut http 403

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai declare un poste de travail
      | libelle | Tour 15  |
      | nature  | tournage |
    Given I am logged in as "user" with role "USER"
    When je consulte ce poste de travail
    Then la reponse a le statut http 200

  Scenario: Un poste de travail n'est pas visible depuis une autre entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    And j'ai declare un poste de travail
      | libelle | Tour 16  |
      | nature  | tournage |
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte ce poste de travail
    Then la reponse a le statut http 404

  Scenario: Deux entreprises peuvent utiliser le meme libelle
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je declare un poste de travail
      | libelle | Tour 17  |
      | nature  | tournage |
    Then la reponse a le statut http 201
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je declare un poste de travail
      | libelle | Tour 17  |
      | nature  | tournage |
    Then la reponse a le statut http 201
