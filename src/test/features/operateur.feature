Feature: Referentiel des operateurs

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Les metiers d'un operateur se deduisent de ses postes
    Given j'ai un poste de travail "Poste de soudure A" de nature "soudage"
    And j'ai un poste de travail "Tour A" de nature "tournage"
    When je declare un operateur
      | nom       | Dupont                     |
      | prenom    | Jean                       |
      | matricule | 049                        |
      | postes    | Tour A, Poste de soudure A |
    Then la reponse a le statut http 201
    And la reponse d'operateur contient
      | nom       | Dupont |
      | prenom    | Jean   |
      | matricule | 049    |
    And la reponse d'operateur a les metiers "soudage, tournage"
    And la reponse d'operateur a les postes "Poste de soudure A, Tour A"

  Scenario: Declaration d'un operateur sans matricule ni habilitation
    When je declare un operateur
      | nom    | Martin |
      | prenom | Sophie |
    Then la reponse a le statut http 201
    And la reponse d'operateur n'a pas de matricule
    And la reponse d'operateur a les metiers ""

  Scenario: Plusieurs operateurs peuvent rester sans matricule
    Given j'ai declare un operateur
      | nom    | Durand |
      | prenom | Paul   |
    When je declare un operateur
      | nom    | Petit |
      | prenom | Marc  |
    Then la reponse a le statut http 201

  Scenario: Declaration refusee si l'identite est deja utilisee
    Given j'ai declare un operateur
      | nom    | Leroy |
      | prenom | Anne  |
    When je declare un operateur
      | nom    | Leroy |
      | prenom | Anne  |
    Then la reponse a le statut http 409

  Scenario: Declaration refusee si le matricule est deja utilise
    Given j'ai declare un operateur
      | nom       | Moreau |
      | prenom    | Luc    |
      | matricule | 100    |
    When je declare un operateur
      | nom       | Simon |
      | prenom    | Eve   |
      | matricule | 100   |
    Then la reponse a le statut http 409

  Scenario: Declaration refusee si un poste est inconnu
    When je declare un operateur
      | nom    | Roux                                 |
      | prenom | Marc                                 |
      | postes | 99999999-9999-9999-9999-999999999999 |
    Then la reponse a le statut http 404

  Scenario: Declaration refusee sans nom
    When je declare un operateur
      | prenom | Jean |
    Then la reponse a le statut http 400

  Scenario: Lecture d'un operateur existant
    Given j'ai un poste de travail "Tour B" de nature "tournage"
    And j'ai declare un operateur
      | nom    | Blanc  |
      | prenom | Claire |
      | postes | Tour B |
    When je consulte cet operateur
    Then la reponse a le statut http 200
    And la reponse d'operateur a les metiers "tournage"

  Scenario: Lecture d'un operateur inexistant renvoie 404
    When je consulte l'operateur "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Suppression refusee d'un poste sur lequel un operateur est habilite
    Given j'ai un poste de travail "Tour C" de nature "tournage"
    And j'ai declare un operateur
      | nom    | Girard |
      | prenom | Hugo   |
      | postes | Tour C |
    When je supprime le poste de travail nomme "Tour C"
    Then la reponse a le statut http 409

  Scenario: Revision des habilitations d'un operateur
    Given j'ai un poste de travail "Tour D" de nature "tournage"
    And j'ai un poste de travail "Poste de soudure D" de nature "soudage"
    And j'ai declare un operateur
      | nom    | Fabre  |
      | prenom | Lea    |
      | postes | Tour D |
    When je revise cet operateur
      | nom    | Fabre              |
      | prenom | Lea                |
      | postes | Poste de soudure D |
    Then la reponse a le statut http 200
    And la reponse d'operateur a les metiers "soudage"

  Scenario: Revision retirant le matricule
    Given j'ai declare un operateur
      | nom       | Noel |
      | prenom    | Remi |
      | matricule | 200  |
    When je revise cet operateur
      | nom    | Noel |
      | prenom | Remi |
    Then la reponse a le statut http 200
    And la reponse d'operateur n'a pas de matricule

  Scenario: Revision conservant sa propre identite et son propre matricule
    Given j'ai declare un operateur
      | nom       | Bonnet |
      | prenom    | Yann   |
      | matricule | 201    |
    When je revise cet operateur
      | nom       | Bonnet |
      | prenom    | Yann   |
      | matricule | 201    |
    Then la reponse a le statut http 200
    And la reponse d'operateur contient
      | matricule | 201 |

  Scenario: Revision refusee si l'identite appartient a un autre operateur
    Given j'ai declare un operateur
      | nom    | Henry |
      | prenom | Nina  |
    And j'ai declare un operateur
      | nom    | Robin |
      | prenom | Tom   |
    When je revise cet operateur
      | nom    | Henry |
      | prenom | Nina  |
    Then la reponse a le statut http 409

  Scenario: Revision refusee si le matricule appartient a un autre operateur
    Given j'ai declare un operateur
      | nom       | Colin |
      | prenom    | Ines  |
      | matricule | 300   |
    And j'ai declare un operateur
      | nom       | Meyer |
      | prenom    | Sacha |
      | matricule | 301   |
    When je revise cet operateur
      | nom       | Meyer |
      | prenom    | Sacha |
      | matricule | 300   |
    Then la reponse a le statut http 409

  Scenario: Revision d'un operateur inexistant renvoie 404
    When je revise l'operateur "11111111-1111-1111-1111-111111111111"
      | nom    | Absent |
      | prenom | Pierre |
    Then la reponse a le statut http 404

  Scenario: Suppression d'un operateur
    Given j'ai declare un operateur
      | nom    | Dubois |
      | prenom | Zoe    |
    When je supprime cet operateur
    Then la reponse a le statut http 204
    When je consulte cet operateur
    Then la reponse a le statut http 404

  Scenario: Suppression d'un operateur inexistant renvoie 404
    When je supprime l'operateur "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Suppression d'un poste libere une fois retire des operateurs
    Given j'ai un poste de travail "Tour E" de nature "tournage"
    And j'ai declare un operateur
      | nom    | Perrin |
      | prenom | Alex   |
      | postes | Tour E |
    When je revise cet operateur
      | nom    | Perrin |
      | prenom | Alex   |
    Then la reponse a le statut http 200
    When je supprime le poste de travail nomme "Tour E"
    Then la reponse a le statut http 204

  Scenario: Liste des operateurs
    Given j'ai declare un operateur
      | nom    | Adam |
      | prenom | Iris |
    And j'ai declare un operateur
      | nom    | Zola |
      | prenom | Theo |
    When je liste les operateurs
    Then la reponse a le statut http 200
    And la reponse contient au moins 2 operateurs

  Scenario: Liste filtree sur les habilites d'un poste
    Given j'ai un poste de travail "Erodeuse F" de nature "erosion"
    And j'ai declare un operateur
      | nom    | Faure      |
      | prenom | Jules      |
      | postes | Erodeuse F |
    And j'ai declare un operateur
      | nom    | Garnier |
      | prenom | Manon   |
    When je liste les operateurs habilites sur "Erodeuse F"
    Then la reponse a le statut http 200
    And la reponse ne contient que l'operateur "Faure"

  Scenario: Declaration refusee a un utilisateur simple
    Given I am logged in as "user" with role "USER"
    When je declare un operateur
      | nom    | Interdit |
      | prenom | Pierre   |
    Then la reponse a le statut http 403

  Scenario: Declaration refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN"
    When je declare un operateur
      | nom    | Interdit |
      | prenom | Paul     |
    Then la reponse a le statut http 403

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai declare un operateur
      | nom    | Lambert |
      | prenom | Chloe   |
    Given I am logged in as "user" with role "USER"
    When je consulte cet operateur
    Then la reponse a le statut http 200

  Scenario: Un operateur n'est pas visible depuis une autre entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    And j'ai declare un operateur
      | nom    | Cloisonne |
      | prenom | Emma      |
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte cet operateur
    Then la reponse a le statut http 404

  Scenario: Deux entreprises peuvent employer la meme identite
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je declare un operateur
      | nom       | Partage |
      | prenom    | Louis   |
      | matricule | 400     |
    Then la reponse a le statut http 201
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je declare un operateur
      | nom       | Partage |
      | prenom    | Louis   |
      | matricule | 400     |
    Then la reponse a le statut http 201
