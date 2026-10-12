Feature: Referentiel des operateurs

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Les metiers d'un operateur se deduisent de ses postes
    Given j'ai un poste de travail "Poste de soudure A" de nature "soudage"
    And j'ai un poste de travail "Tour A" de nature "tournage"
    When je declare un operateur
      | nom         | Dupont                     |
      | prenom      | Jean                       |
      | identifiant | 049                        |
      | postes      | Tour A, Poste de soudure A |
      | tauxHoraire | 20                         |
    Then la reponse a le statut http 201
    And la reponse d'operateur contient
      | nom         | Dupont |
      | prenom      | Jean   |
      | identifiant | 049    |
    And la reponse d'operateur a les metiers "soudage, tournage"
    And la reponse d'operateur a les postes "Poste de soudure A, Tour A"

  Scenario: Declaration d'un operateur avec son taux horaire
    When je declare un operateur
      | nom         | Perez |
      | prenom      | Rosa  |
      | tauxHoraire | 22.5  |
    Then la reponse a le statut http 201
    And la reponse d'operateur contient
      | tauxHoraire | 22.5 |

  Scenario: Un taux inferieur au centime ne peut pas rendre le referentiel illisible
    When je declare un operateur
      | nom         | Precision |
      | prenom      | Centime   |
      | tauxHoraire | 0.001     |
    Then la reponse a le statut http 400
    When je liste les operateurs
    Then la reponse a le statut http 200

  Scenario: Revision du taux horaire d'un operateur
    Given j'ai declare un operateur
      | nom         | Faucher |
      | prenom      | Elise   |
      | tauxHoraire | 20      |
    When je revise cet operateur
      | nom         | Faucher |
      | prenom      | Elise   |
      | tauxHoraire | 27.5    |
    Then la reponse a le statut http 200
    And la reponse d'operateur contient
      | tauxHoraire | 27.5 |

  # Le taux est fige sur chaque pointage : un operateur sans taux rendrait le cout de revient non exportable.
  Scenario: Declaration refusee sans taux horaire
    When je declare un operateur
      | nom    | Sanstaux |
      | prenom | Lea      |
    Then la reponse a le statut http 400

  Scenario: Revision refusee sans taux horaire, le taux precedent est conserve
    Given j'ai declare un operateur
      | nom         | Retrait |
      | prenom      | Taux    |
      | tauxHoraire | 22.00   |
    When je revise cet operateur
      | nom    | Retrait |
      | prenom | Taux    |
    Then la reponse a le statut http 400
    When je consulte cet operateur
    Then la reponse d'operateur a le taux horaire "22.00"

  Scenario: Declaration d'un operateur sans identifiant ni habilitation
    When je declare un operateur
      | nom         | Martin |
      | prenom      | Sophie |
      | tauxHoraire | 20     |
    Then la reponse a le statut http 201
    And la reponse d'operateur n'a pas d'identifiant
    And la reponse d'operateur a les metiers ""

  Scenario: Plusieurs operateurs peuvent rester sans identifiant
    Given j'ai declare un operateur
      | nom         | Durand |
      | prenom      | Paul   |
      | tauxHoraire | 20     |
    When je declare un operateur
      | nom         | Petit |
      | prenom      | Marc  |
      | tauxHoraire | 20    |
    Then la reponse a le statut http 201

  Scenario: Declaration refusee si l'identite est deja utilisee
    Given j'ai declare un operateur
      | nom         | Leroy |
      | prenom      | Anne  |
      | tauxHoraire | 20    |
    When je declare un operateur
      | nom         | Leroy |
      | prenom      | Anne  |
      | tauxHoraire | 20    |
    Then la reponse a le statut http 409

  Scenario: Declaration refusee si l'identifiant est deja utilise
    Given j'ai declare un operateur
      | nom         | Moreau |
      | prenom      | Luc    |
      | identifiant | 100    |
      | tauxHoraire | 20     |
    When je declare un operateur
      | nom         | Simon |
      | prenom      | Eve   |
      | identifiant | 100   |
      | tauxHoraire | 20    |
    Then la reponse a le statut http 409

  Scenario: Declaration refusee si un poste est inconnu
    When je declare un operateur
      | nom         | Roux                                 |
      | prenom      | Marc                                 |
      | postes      | 99999999-9999-9999-9999-999999999999 |
      | tauxHoraire | 20                                   |
    Then la reponse a le statut http 404

  Scenario: Declaration refusee sans nom
    When je declare un operateur
      | prenom      | Jean |
      | tauxHoraire | 20   |
    Then la reponse a le statut http 400

  Scenario: L'identifiant conserve ses zeros en tete
    When je declare un operateur
      | nom         | Garnier |
      | prenom      | Ines    |
      | identifiant | 007     |
      | tauxHoraire | 20      |
    Then la reponse a le statut http 201
    And la reponse d'operateur contient
      | identifiant | 007 |

  Scenario Outline: Declaration refusee si l'identifiant n'est pas fait de 1 a 6 chiffres
    When je declare un operateur
      | nom         | Lambert       |
      | prenom      | Paul          |
      | identifiant | <identifiant> |
      | tauxHoraire | 20            |
    Then la reponse a le statut http 400

    Examples:
      | identifiant |
      | 12A         |
      | AB-12       |
      | 1234567     |

  Scenario: Revision refusee si l'identifiant depasse 6 chiffres
    Given j'ai declare un operateur
      | nom         | Marchand |
      | prenom      | Hugo     |
      | tauxHoraire | 20       |
    When je revise cet operateur
      | nom         | Marchand |
      | prenom      | Hugo     |
      | identifiant | 1234567  |
      | tauxHoraire | 20       |
    Then la reponse a le statut http 400

  Scenario: Lecture d'un operateur existant
    Given j'ai un poste de travail "Tour B" de nature "tournage"
    And j'ai declare un operateur
      | nom         | Blanc  |
      | prenom      | Claire |
      | postes      | Tour B |
      | tauxHoraire | 20     |
    When je consulte cet operateur
    Then la reponse a le statut http 200
    And la reponse d'operateur a les metiers "tournage"

  Scenario: Lecture d'un operateur inexistant renvoie 404
    When je consulte l'operateur "11111111-1111-1111-1111-111111111111"
    Then la reponse a le statut http 404

  Scenario: Suppression refusee d'un poste sur lequel un operateur est habilite
    Given j'ai un poste de travail "Tour C" de nature "tournage"
    And j'ai declare un operateur
      | nom         | Girard |
      | prenom      | Hugo   |
      | postes      | Tour C |
      | tauxHoraire | 20     |
    When je supprime le poste de travail nomme "Tour C"
    Then la reponse a le statut http 409

  Scenario: Revision des habilitations d'un operateur
    Given j'ai un poste de travail "Tour D" de nature "tournage"
    And j'ai un poste de travail "Poste de soudure D" de nature "soudage"
    And j'ai declare un operateur
      | nom         | Fabre  |
      | prenom      | Lea    |
      | postes      | Tour D |
      | tauxHoraire | 20     |
    When je revise cet operateur
      | nom         | Fabre              |
      | prenom      | Lea                |
      | postes      | Poste de soudure D |
      | tauxHoraire | 20                 |
    Then la reponse a le statut http 200
    And la reponse d'operateur a les metiers "soudage"

  Scenario: Revision retirant l'identifiant
    Given j'ai declare un operateur
      | nom         | Noel |
      | prenom      | Remi |
      | identifiant | 200  |
      | tauxHoraire | 20   |
    When je revise cet operateur
      | nom         | Noel |
      | prenom      | Remi |
      | tauxHoraire | 20   |
    Then la reponse a le statut http 200
    And la reponse d'operateur n'a pas d'identifiant

  Scenario: Revision conservant sa propre identite et son propre identifiant
    Given j'ai declare un operateur
      | nom         | Bonnet |
      | prenom      | Yann   |
      | identifiant | 201    |
      | tauxHoraire | 20     |
    When je revise cet operateur
      | nom         | Bonnet |
      | prenom      | Yann   |
      | identifiant | 201    |
      | tauxHoraire | 20     |
    Then la reponse a le statut http 200
    And la reponse d'operateur contient
      | identifiant | 201 |

  Scenario: Revision refusee si l'identite appartient a un autre operateur
    Given j'ai declare un operateur
      | nom         | Henry |
      | prenom      | Nina  |
      | tauxHoraire | 20    |
    And j'ai declare un operateur
      | nom         | Robin |
      | prenom      | Tom   |
      | tauxHoraire | 20    |
    When je revise cet operateur
      | nom         | Henry |
      | prenom      | Nina  |
      | tauxHoraire | 20    |
    Then la reponse a le statut http 409

  Scenario: Revision refusee si l'identifiant appartient a un autre operateur
    Given j'ai declare un operateur
      | nom         | Colin |
      | prenom      | Ines  |
      | identifiant | 300   |
      | tauxHoraire | 20    |
    And j'ai declare un operateur
      | nom         | Meyer |
      | prenom      | Sacha |
      | identifiant | 301   |
      | tauxHoraire | 20    |
    When je revise cet operateur
      | nom         | Meyer |
      | prenom      | Sacha |
      | identifiant | 300   |
      | tauxHoraire | 20    |
    Then la reponse a le statut http 409

  Scenario: Revision d'un operateur inexistant renvoie 404
    When je revise l'operateur "11111111-1111-1111-1111-111111111111"
      | nom         | Absent |
      | prenom      | Pierre |
      | tauxHoraire | 20     |
    Then la reponse a le statut http 404

  Scenario: Suppression d'un operateur
    Given j'ai declare un operateur
      | nom         | Dubois |
      | prenom      | Zoe    |
      | tauxHoraire | 20     |
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
      | nom         | Perrin |
      | prenom      | Alex   |
      | postes      | Tour E |
      | tauxHoraire | 20     |
    When je revise cet operateur
      | nom         | Perrin |
      | prenom      | Alex   |
      | tauxHoraire | 20     |
    Then la reponse a le statut http 200
    When je supprime le poste de travail nomme "Tour E"
    Then la reponse a le statut http 204

  Scenario: Liste des operateurs
    Given j'ai declare un operateur
      | nom         | Adam |
      | prenom      | Iris |
      | tauxHoraire | 20   |
    And j'ai declare un operateur
      | nom         | Zola |
      | prenom      | Theo |
      | tauxHoraire | 20   |
    When je liste les operateurs
    Then la reponse a le statut http 200
    And la reponse contient au moins 2 operateurs

  Scenario: Liste filtree sur les habilites d'un poste
    Given j'ai un poste de travail "Erodeuse F" de nature "erosion"
    And j'ai declare un operateur
      | nom         | Faure      |
      | prenom      | Jules      |
      | postes      | Erodeuse F |
      | tauxHoraire | 20         |
    And j'ai declare un operateur
      | nom         | Garnier |
      | prenom      | Manon   |
      | tauxHoraire | 20      |
    When je liste les operateurs habilites sur "Erodeuse F"
    Then la reponse a le statut http 200
    And la reponse ne contient que l'operateur "Faure"

  Scenario: Declaration refusee a un utilisateur simple
    Given I am logged in as "user" with role "USER"
    When je declare un operateur
      | nom         | Interdit |
      | prenom      | Pierre   |
      | tauxHoraire | 20       |
    Then la reponse a le statut http 403

  Scenario: Declaration refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN"
    When je declare un operateur
      | nom         | Interdit |
      | prenom      | Paul     |
      | tauxHoraire | 20       |
    Then la reponse a le statut http 403

  Scenario: Lecture autorisee a un utilisateur simple
    Given j'ai declare un operateur
      | nom         | Lambert |
      | prenom      | Chloe   |
      | tauxHoraire | 20      |
    Given I am logged in as "user" with role "USER"
    When je consulte cet operateur
    Then la reponse a le statut http 200

  Scenario: Un operateur n'est pas visible depuis une autre entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    And j'ai declare un operateur
      | nom         | Cloisonne |
      | prenom      | Emma      |
      | tauxHoraire | 20        |
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte cet operateur
    Then la reponse a le statut http 404

  Scenario: Deux entreprises peuvent employer la meme identite
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je declare un operateur
      | nom         | Partage |
      | prenom      | Louis   |
      | identifiant | 400     |
      | tauxHoraire | 20      |
    Then la reponse a le statut http 201
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je declare un operateur
      | nom         | Partage |
      | prenom      | Louis   |
      | identifiant | 400     |
      | tauxHoraire | 20      |
    Then la reponse a le statut http 201

  Scenario Outline: Le taux horaire doit etre representable sans arrondi ni depassement
    When je declare un operateur
      | nom         | Precision |
      | prenom      | Refuse    |
      | tauxHoraire | <taux>    |
    Then la reponse a le statut http 400

    Examples:
      | taux      |
      | 0.001     |
      | 22.555    |
      | 100000000 |
      | 1E+8      |

  Scenario Outline: La revision refuse un taux non representable et conserve le precedent
    Given j'ai declare un operateur
      | nom         | Precision       |
      | prenom      | Revision <taux> |
      | tauxHoraire | 22.00           |
    When je revise cet operateur
      | nom         | Precision       |
      | prenom      | Revision <taux> |
      | tauxHoraire | <taux>          |
    Then la reponse a le statut http 400
    When je consulte cet operateur
    Then la reponse a le statut http 200
    And la reponse d'operateur a le taux horaire "22.00"

    Examples:
      | taux      |
      | 0.001     |
      | 100000000 |

  Scenario Outline: Un taux representable conserve sa valeur a la relecture
    Given j'ai declare un operateur
      | nom         | Precision      |
      | prenom      | Accepte <taux> |
      | tauxHoraire | <taux>         |
    When je consulte cet operateur
    Then la reponse a le statut http 200
    And la reponse d'operateur a le taux horaire "<attendu>"
    When je liste les operateurs
    Then la reponse a le statut http 200

    Examples:
      | taux        | attendu     |
      | 0.01        | 0.01        |
      | 99999999.99 | 99999999.99 |
      | 22.000      | 22.00       |
      | 1E+3        | 1000        |

  Scenario: Les tarifs des operateurs sont reserves au gestionnaire
    Given j'ai declare un operateur
      | nom         | Confidentiel |
      | prenom      | Operateur    |
      | tauxHoraire | 22.00        |
    Given I am logged in as "user" with role "USER"
    When je consulte cet operateur
    Then la reponse a le statut http 200
    And la reponse d'operateur contient
      | nom    | Confidentiel |
      | prenom | Operateur    |
    And la reponse ne contient aucun tarif horaire
    When je liste les operateurs
    Then la reponse a le statut http 200
    And la reponse ne contient aucun tarif horaire
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    When je consulte cet operateur
    Then la reponse a le statut http 200
    And la reponse d'operateur a le taux horaire "22.00"
