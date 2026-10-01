Feature: Intention et activite visee des pointages d'atelier

  # Le type d'un pointage ne dit pas ce qu'il fait d'une activite : son intention le dit. Une ouverture cree une
  # activite, y compris en non conformite ou a la reprise apres une pause. Une transition remplace l'activite qu'elle
  # vise par une activite de l'autre categorie. Une fin termine l'activite qu'elle vise, et elle seule. L'activite
  # visee se designe par l'identifiant de son pointage ouvrant d'origine, que le journal expose dans `activite`.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-1" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "fraiseuse-2" de nature "tournage"
    And l'entreprise a declare l'operateur "dupont" habilite sur "fraiseuse-1" et "fraiseuse-2" avec un taux horaire de "22.5"

  Scenario: Travail, non conformite puis travail : chaque transition vise l'activite qu'elle remplace
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2101"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2101                 |
    And j'ai engage l'element "OF 2101" en atelier
    And j'ai pointe sur "OF 2101"
      | id        | 00000000-0000-0000-0000-000000000201 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    And j'ai pointe sur "OF 2101"
      | id        | 00000000-0000-0000-0000-000000000202 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000201 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T12:00:00Z"
    When je pointe sur "OF 2101"
      | id        | 00000000-0000-0000-0000-000000000203 |
      | type      | DEBUT                                |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000202 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And l'activite en cours est de categorie "TRAVAIL" depuis "2026-05-10T12:00:00Z"
    Given il est "2026-05-10T14:00:00Z"
    When je pointe sur "OF 2101"
      | id        | 00000000-0000-0000-0000-000000000204 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000203 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le suivi a l'etat "INTERROMPU"
    And l'evenement 0 du suivi a l'intention "OUVERTURE"
    And l'evenement 0 du suivi ouvre sa propre activite sans en viser aucune
    And l'evenement 1 du suivi a l'intention "TRANSITION"
    And l'evenement 1 du suivi vise l'activite de l'evenement 0
    And l'evenement 2 du suivi vise l'activite de l'evenement 1
    And l'evenement 3 du suivi a l'intention "FIN"
    And l'evenement 3 du suivi vise l'activite de l'evenement 2
    And l'evenement 3 du suivi n'ouvre aucune activite
    # Trois activites distinctes et contigues : aucun trou, aucun recouvrement.
    When je consulte le temps effectif de "OF 2101"
    Then le temps effectif contient
      | categorie      | debut                | fin                  |
      | TRAVAIL        | 2026-05-10T08:00:00Z | 2026-05-10T10:00:00Z |
      | NON_CONFORMITE | 2026-05-10T10:00:00Z | 2026-05-10T12:00:00Z |
      | TRAVAIL        | 2026-05-10T12:00:00Z | 2026-05-10T14:00:00Z |

  Scenario: Reprise en non conformite apres une pause, puis transition ciblee vers le travail
    # La pause arrete la non conformite par une fin ; la reprise en rouvre une par une ouverture, sans cible. Le retour
    # au travail est une transition qui vise la non conformite reprise, pas celle d'avant la pause.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2102"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2102                 |
    And j'ai engage l'element "OF 2102" en atelier
    And j'ai pointe sur "OF 2102"
      | id        | 00000000-0000-0000-0000-000000000211 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T09:00:00Z"
    And j'ai pointe sur "OF 2102"
      | id        | 00000000-0000-0000-0000-000000000212 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000211 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    And j'ai pointe sur "OF 2102"
      | id        | 00000000-0000-0000-0000-000000000213 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000212 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T11:00:00Z"
    When je pointe sur "OF 2102"
      | id        | 00000000-0000-0000-0000-000000000214 |
      | type      | NON_CONFORMITE                       |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And l'activite en cours est de categorie "NON_CONFORMITE" depuis "2026-05-10T11:00:00Z"
    And l'evenement 3 du suivi a l'intention "OUVERTURE"
    And l'evenement 3 du suivi ouvre sa propre activite sans en viser aucune
    Given il est "2026-05-10T12:00:00Z"
    When je pointe sur "OF 2102"
      | id        | 00000000-0000-0000-0000-000000000215 |
      | type      | DEBUT                                |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000214 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And l'activite en cours est de categorie "TRAVAIL" depuis "2026-05-10T12:00:00Z"
    And l'evenement 4 du suivi a l'intention "TRANSITION"
    And l'evenement 4 du suivi vise l'activite de l'evenement 3

  Scenario: Une transition dont la cible est terminee ne devient jamais une ouverture
    # Le geste contradictoire est conserve, jamais refuse : la sequence est en conflit, et la non conformite qu'il ouvre
    # est a resoudre, pas en cours a la place de sa cible.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2103"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2103                 |
    And j'ai engage l'element "OF 2103" en atelier
    And j'ai pointe sur "OF 2103"
      | id        | 00000000-0000-0000-0000-000000000221 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    And j'ai pointe sur "OF 2103"
      | id        | 00000000-0000-0000-0000-000000000222 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000221 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T11:00:00Z"
    When je pointe sur "OF 2103"
      | id        | 00000000-0000-0000-0000-000000000223 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000221 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le journal du suivi contient 3 evenements
    And le suivi a 0 activites en cours
    And le suivi a l'etat "INTERROMPU"
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000221, 00000000-0000-0000-0000-000000000223                                       |
      | pointages | 00000000-0000-0000-0000-000000000221, 00000000-0000-0000-0000-000000000222, 00000000-0000-0000-0000-000000000223 |

  Scenario: Une fin qui vise une activite remplacee ne termine jamais sa remplacante
    # A a 8 h, relance B a 10 h, puis une fin qui vise encore A. Le geste contradictoire est conserve : la sequence est
    # en conflit, et B, a resoudre avec A, n'est plus en cours sans jamais avoir ete terminee par la fin de A.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2104"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2104                 |
    And j'ai engage l'element "OF 2104" en atelier
    And j'ai pointe sur "OF 2104"
      | id        | 00000000-0000-0000-0000-000000000231 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    And j'ai pointe sur "OF 2104"
      | id        | 00000000-0000-0000-0000-000000000232 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T11:00:00Z"
    When je pointe sur "OF 2104"
      | id        | 00000000-0000-0000-0000-000000000233 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000231 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    When je consulte "OF 2104"
    Then le suivi a 0 activites en cours
    And le suivi a l'etat "INTERROMPU"
    And le journal du suivi contient 3 evenements
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000231, 00000000-0000-0000-0000-000000000232                                       |
      | pointages | 00000000-0000-0000-0000-000000000231, 00000000-0000-0000-0000-000000000232, 00000000-0000-0000-0000-000000000233 |

  Scenario: Une fin qui vise une activite introuvable dans ce suivi est refusee
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2105"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2105                 |
    And j'ai engage l'element "OF 2105" en atelier
    When je pointe sur "OF 2105"
      | id        | 00000000-0000-0000-0000-000000000241 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 4d7c2a19-8e03-4b56-9f21-c0a1b2d3e4f5 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-visee-introuvable"
    When je consulte "OF 2105"
    Then le journal du suivi contient 0 evenements

  Scenario: Une fin qui vise l'activite d'un autre poste est refusee
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2106"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2106                 |
    And j'ai engage l'element "OF 2106" en atelier
    And j'ai pointe sur "OF 2106"
      | id        | 00000000-0000-0000-0000-000000000242 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF 2106"
      | id        | 00000000-0000-0000-0000-000000000243 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000242 |
      | operateur | dupont                               |
      | poste     | fraiseuse-2                          |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-visee-incoherente"
    When je consulte "OF 2106"
    Then le suivi a 1 activites en cours

  Scenario: Un geste sans intention, ou dont la cible ne s'accorde pas a son intention, est refuse
    # Aucune intention par defaut : un debut sans intention ne devient pas une ouverture.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2107"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2107                 |
    And j'ai engage l'element "OF 2107" en atelier
    When je pointe sur "OF 2107" sans intention
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 400
    When je pointe sur "OF 2107"
      | type      | NON_CONFORMITE |
      | intention | TRANSITION     |
      | operateur | dupont         |
      | poste     | fraiseuse-1    |
    Then la reponse a le statut http 400
    When je pointe sur "OF 2107"
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | cible     | 4d7c2a19-8e03-4b56-9f21-c0a1b2d3e4f5 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 400
    When je pointe sur "OF 2107"
      | type      | FIN         |
      | intention | OUVERTURE   |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 400
    When je regularise sur "OF 2107"
      | type           | FIN                  |
      | intention      | FIN                  |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T08:00:00Z |
    Then la reponse a le statut http 400
    When je consulte "OF 2107"
    Then le journal du suivi contient 0 evenements

  Scenario: Le debut corrige garde son activite, et la fin qui la visait la termine toujours
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2108"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2108                 |
    And j'ai engage l'element "OF 2108" en atelier
    And j'ai pointe sur "OF 2108"
      | id        | 00000000-0000-0000-0000-000000000251 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe sur "OF 2108"
      | id        | 00000000-0000-0000-0000-000000000252 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000251 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T13:00:00Z"
    When je corrige l'evenement 0 de "OF 2108"
      | motif          | Demarre a 8h30       |
      | type           | DEBUT                |
      | intention      | FIN                  |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T08:30:00Z |
    Then la reponse a le statut http 400
    When je corrige l'evenement 0 de "OF 2108"
      | motif          | Demarre a 8h30       |
      | type           | DEBUT                |
      | intention      | OUVERTURE            |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T08:30:00Z |
    Then la reponse a le statut http 200
    And le journal du suivi contient 3 evenements
    And l'evenement 1 du suivi ouvre l'activite de l'evenement 0 sous son propre identifiant
    And l'evenement 2 du suivi vise l'activite de l'evenement 1
    When je consulte le temps effectif de "OF 2108"
    Then le temps effectif contient
      | debut                | fin                  |
      | 2026-05-10T08:30:00Z | 2026-05-10T12:00:00Z |

  Scenario: Une fin oubliee se regularise sur l'activite qu'elle termine
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2109"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2109                 |
    And j'ai engage l'element "OF 2109" en atelier
    And j'ai pointe sur "OF 2109"
      | id        | 00000000-0000-0000-0000-000000000281 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-11T09:00:00Z"
    When je regularise sur "OF 2109"
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000281 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-05-10T17:00:00Z                 |
    Then la reponse a le statut http 201
    And le suivi a l'etat "INTERROMPU"
    And l'evenement 1 du suivi vise l'activite de l'evenement 0
    And l'evenement 1 du suivi est une regularisation de "dupont" saisie par "gestionnaire"
