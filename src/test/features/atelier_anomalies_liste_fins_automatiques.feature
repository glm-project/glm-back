Feature: Liste des fins automatiques parmi les anomalies de pointage

  # Une fin automatique n'est jamais stockee : la liste la juge a l'instant de lecture, l'horloge figee du scenario,
  # sur les seules projections. Une activite y figure des que son echeance, son debut plus 13 heures, est atteinte,
  # borne comprise, tant qu'aucune fin reelle ne l'a terminee et qu'aucun conflit ne la laisse a resoudre.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-liste-fins" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare l'operateur "dupont-liste-fins" habilite sur "fraiseuse-liste-fins"

  Scenario: Une fin tardive est listee avec son debut et son echeance
    Given il est "2044-02-01T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Liste fins 8101"
      | categorie | OF     |
      | reference | LF8101 |
    And j'ai engage l'element "Liste fins 8101" en atelier
    And il est "2044-02-01T08:00:00Z"
    And j'ai pointe sur "Liste fins 8101"
      | id        | 00000000-0000-0000-0000-000000081011 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-01T22:00:00Z"
    When je liste les fins automatiques de "Liste fins 8101"
    Then la liste des fins automatiques compte 1 ligne
    And la ligne de la liste des fins automatiques porte
      | pointage | 00000000-0000-0000-0000-000000081011 |
      | activite | 00000000-0000-0000-0000-000000081011 |
      | debut    | 2044-02-01T08:00:00Z                 |
      | echeance | 2044-02-01T21:00:00Z                 |

  Scenario: L'echeance est comprise dans la liste a la nanoseconde pres
    Given il est "2044-02-02T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Liste fins 8102"
      | categorie | OF     |
      | reference | LF8102 |
    And j'ai engage l'element "Liste fins 8102" en atelier
    And il est "2044-02-02T08:00:00Z"
    And j'ai pointe sur "Liste fins 8102"
      | id        | 00000000-0000-0000-0000-000000081021 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    Given il est "2044-02-02T20:59:59.999999999Z"
    When je liste les fins automatiques de "Liste fins 8102"
    Then la liste des fins automatiques compte 0 ligne
    Given il est "2044-02-02T21:00:00Z"
    When je liste les fins automatiques de "Liste fins 8102"
    Then la liste des fins automatiques compte 1 ligne
    And la ligne de la liste des fins automatiques porte
      | echeance | 2044-02-02T21:00:00Z |

  Scenario: Une activite a resoudre n'est pas une fin automatique
    Given il est "2044-02-03T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Liste fins 8103"
      | categorie | OF     |
      | reference | LF8103 |
    And j'ai engage l'element "Liste fins 8103" en atelier
    And il est "2044-02-03T08:00:00Z"
    And j'ai pointe sur "Liste fins 8103"
      | id        | 00000000-0000-0000-0000-000000081031 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-03T09:00:00Z"
    And j'ai pointe sur "Liste fins 8103"
      | id        | 00000000-0000-0000-0000-000000081032 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000081031 |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-03T10:00:00Z"
    And j'ai pointe sur "Liste fins 8103"
      | id        | 00000000-0000-0000-0000-000000081033 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000081031 |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-03T22:00:00Z"
    When je consulte "Liste fins 8103"
    Then le suivi porte 1 sequence en conflit
    When je liste les fins automatiques de "Liste fins 8103"
    Then la liste des fins automatiques compte 0 ligne

  Scenario: Une fin regularisee par le gestionnaire sort de la liste
    Given il est "2044-02-04T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Liste fins 8104"
      | categorie | OF     |
      | reference | LF8104 |
    And j'ai engage l'element "Liste fins 8104" en atelier
    And il est "2044-02-04T08:00:00Z"
    And j'ai pointe sur "Liste fins 8104"
      | id        | 00000000-0000-0000-0000-000000081041 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-04T22:00:00Z"
    When je liste les fins automatiques de "Liste fins 8104"
    Then la liste des fins automatiques compte 1 ligne
    When je regularise sur "Liste fins 8104" en visant l'activite de l'evenement 0
      | type           | FIN                  |
      | intention      | FIN                  |
      | operateur      | dupont-liste-fins    |
      | poste          | fraiseuse-liste-fins |
      | dateDeSurvenue | 2044-02-04T17:00:00Z |
    Then la reponse a le statut http 201
    When je liste les fins automatiques de "Liste fins 8104"
    Then la liste des fins automatiques compte 0 ligne

  Scenario: Une fin reelle pointee apres l'echeance laisse l'activite listee
    Given il est "2044-02-05T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Liste fins 8105"
      | categorie | OF     |
      | reference | LF8105 |
    And j'ai engage l'element "Liste fins 8105" en atelier
    And il est "2044-02-05T08:00:00Z"
    And j'ai pointe sur "Liste fins 8105"
      | id        | 00000000-0000-0000-0000-000000081051 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-05T23:00:00Z"
    And j'ai pointe sur "Liste fins 8105"
      | id        | 00000000-0000-0000-0000-000000081052 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000081051 |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    When je liste les fins automatiques de "Liste fins 8105"
    Then la liste des fins automatiques compte 1 ligne
    And la ligne de la liste des fins automatiques porte
      | pointage | 00000000-0000-0000-0000-000000081051 |
      | echeance | 2044-02-05T21:00:00Z                 |

  Scenario: Une activite reouverte avant son echeance ne laisse que la nouvelle fin automatique
    Given il est "2044-02-06T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Liste fins 8106"
      | categorie | OF     |
      | reference | LF8106 |
    And j'ai engage l'element "Liste fins 8106" en atelier
    And il est "2044-02-06T08:00:00Z"
    And j'ai pointe sur "Liste fins 8106"
      | id        | 00000000-0000-0000-0000-000000081061 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-06T12:00:00Z"
    And j'ai pointe sur "Liste fins 8106"
      | id        | 00000000-0000-0000-0000-000000081062 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-liste-fins                    |
      | poste     | fraiseuse-liste-fins                 |
    And il est "2044-02-07T02:00:00Z"
    When je liste les fins automatiques de "Liste fins 8106"
    Then la liste des fins automatiques compte 1 ligne
    And la ligne de la liste des fins automatiques porte
      | pointage | 00000000-0000-0000-0000-000000081062 |
      | debut    | 2044-02-06T12:00:00Z                 |
      | echeance | 2044-02-07T01:00:00Z                 |
