Feature: Sequences en conflit

  # Des pointages qui se contredisent sont conserves, jamais refuses ni reinterpretes par le serveur : leur sequence est
  # en conflit, quel que soit leur ordre de reception, et le gestionnaire la resout en corrigeant ou en annulant les
  # faits concernes. Tant qu'elle dure, ses activites sont a resoudre : ni en cours ni terminees, sans fin ni duree, et
  # l'echeance ne les termine pas. Un pointage conserve en conflit est acquitte (201, puis 200 au rejeu) : son
  # identifiant figure dans les pointages de la sequence, ce qui le distingue d'un refus (4xx).
  #
  # Chaque scenario engage son element un jour qui lui est propre.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-1" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare l'operateur "dupont" habilite sur "fraiseuse-1"

  Scenario: Transition A vers NC a 12 h puis fin de A a 17 h : la sequence est en conflit
    Given il est "2026-07-06T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6001"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6001                 |
    And j'ai engage l'element "OF 6001" en atelier
    And il est "2026-07-06T08:00:00Z"
    And j'ai pointe sur "OF 6001"
      | id        | 00000000-0000-0000-0000-000000000601 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-06T12:00:00Z"
    And j'ai pointe sur "OF 6001"
      | id        | 00000000-0000-0000-0000-000000000602 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000601 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-06T17:00:00Z"
    When je pointe sur "OF 6001"
      | id        | 00000000-0000-0000-0000-000000000603 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000601 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    # La fin de A ne termine pas la non conformite qui l'a remplacee, et la transition n'est pas ignoree.
    Then la reponse a le statut http 201
    And le journal du suivi contient 3 evenements
    And le suivi a 0 activites en cours
    And le suivi a l'etat "INTERROMPU"
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000601, 00000000-0000-0000-0000-000000000602                                       |
      | pointages | 00000000-0000-0000-0000-000000000601, 00000000-0000-0000-0000-000000000602, 00000000-0000-0000-0000-000000000603 |
    # Aucune duree n'est presentee comme definitive, et l'echeance ne tranche pas : lue le lendemain, rien n'a de fin.
    Given il est "2026-07-07T09:00:00Z"
    When je consulte le temps effectif de "OF 6001"
    Then le temps effectif contient
      | activite                             | categorie      | debut                | fin | finAutomatique | aResoudre |
      | 00000000-0000-0000-0000-000000000601 | TRAVAIL        | 2026-07-06T08:00:00Z |     | false          | true      |
      | 00000000-0000-0000-0000-000000000602 | NON_CONFORMITE | 2026-07-06T12:00:00Z |     | false          | true      |

  Scenario: La fin de A recue avant la transition rejouee le lendemain donne la meme sequence en conflit
    Given il est "2026-07-08T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6002"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6002                 |
    And j'ai engage l'element "OF 6002" en atelier
    And il est "2026-07-08T08:00:00Z"
    And j'ai pointe sur "OF 6002"
      | id        | 00000000-0000-0000-0000-000000000611 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-08T17:00:00Z"
    When je pointe sur "OF 6002"
      | id        | 00000000-0000-0000-0000-000000000613 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000611 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le suivi n'a aucune sequence en conflit
    # La transition pointee hors ligne a 12 h arrive le lendemain : la fin deja acceptee devient contradictoire.
    Given il est "2026-07-09T09:00:00Z"
    When je pointe sur "OF 6002"
      | id             | 00000000-0000-0000-0000-000000000612 |
      | type           | NON_CONFORMITE                       |
      | intention      | TRANSITION                           |
      | cible          | 00000000-0000-0000-0000-000000000611 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-07-08T12:00:00Z                 |
    Then la reponse a le statut http 201
    And le journal du suivi contient 3 evenements
    And le suivi a 0 activites en cours
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000611, 00000000-0000-0000-0000-000000000612                                       |
      | pointages | 00000000-0000-0000-0000-000000000611, 00000000-0000-0000-0000-000000000612, 00000000-0000-0000-0000-000000000613 |

  Scenario: Un geste visant A apres son remplacement par B ne termine jamais B, et son rejeu rend le meme conflit
    Given il est "2026-07-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6003"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6003                 |
    And j'ai engage l'element "OF 6003" en atelier
    And il est "2026-07-10T08:00:00Z"
    And j'ai pointe sur "OF 6003"
      | id        | 00000000-0000-0000-0000-000000000621 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-10T10:00:00Z"
    And j'ai pointe sur "OF 6003"
      | id        | 00000000-0000-0000-0000-000000000622 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-10T11:00:00Z"
    When je pointe sur "OF 6003"
      | id        | 00000000-0000-0000-0000-000000000623 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000621 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    # Acquitte, le pointage n'est pas un refus : son identifiant figure dans la sequence en conflit.
    Then la reponse a le statut http 201
    And le suivi a 0 activites en cours
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000621, 00000000-0000-0000-0000-000000000622                                       |
      | pointages | 00000000-0000-0000-0000-000000000621, 00000000-0000-0000-0000-000000000622, 00000000-0000-0000-0000-000000000623 |
    # Le pupitre qui rejoue ce geste recoit le meme conflit, sans second fait.
    Given il est "2026-07-10T11:05:00Z"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal du suivi contient 3 evenements
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000621, 00000000-0000-0000-0000-000000000622                                       |
      | pointages | 00000000-0000-0000-0000-000000000621, 00000000-0000-0000-0000-000000000622, 00000000-0000-0000-0000-000000000623 |
    When je consulte le temps effectif de "OF 6003"
    Then le temps effectif contient
      | activite                             | debut                | fin | aResoudre |
      | 00000000-0000-0000-0000-000000000621 | 2026-07-10T08:00:00Z |     | true      |
      | 00000000-0000-0000-0000-000000000622 | 2026-07-10T10:00:00Z |     | true      |

  Scenario: Une nouvelle ouverture apres une sequence en conflit est en cours, hors du conflit
    Given il est "2026-07-13T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6004"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6004                 |
    And j'ai engage l'element "OF 6004" en atelier
    And il est "2026-07-13T08:00:00Z"
    And j'ai pointe sur "OF 6004"
      | id        | 00000000-0000-0000-0000-000000000631 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-13T10:00:00Z"
    And j'ai pointe sur "OF 6004"
      | id        | 00000000-0000-0000-0000-000000000632 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-13T11:00:00Z"
    And j'ai pointe sur "OF 6004"
      | id        | 00000000-0000-0000-0000-000000000633 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000631 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-13T13:00:00Z"
    When je pointe sur "OF 6004"
      | id        | 00000000-0000-0000-0000-000000000634 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le suivi a l'etat "EN_COURS"
    And les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-07-13T13:00:00Z | 00000000-0000-0000-0000-000000000634 | 2026-07-14T02:00:00Z |
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000631, 00000000-0000-0000-0000-000000000632                                       |
      | pointages | 00000000-0000-0000-0000-000000000631, 00000000-0000-0000-0000-000000000632, 00000000-0000-0000-0000-000000000633 |

  Scenario: Une transition vers sa propre categorie est conservee en conflit
    Given il est "2026-07-14T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6005"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6005                 |
    And j'ai engage l'element "OF 6005" en atelier
    And il est "2026-07-14T08:00:00Z"
    And j'ai pointe sur "OF 6005"
      | id        | 00000000-0000-0000-0000-000000000641 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-14T09:00:00Z"
    When je pointe sur "OF 6005"
      | id        | 00000000-0000-0000-0000-000000000642 |
      | type      | DEBUT                                |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000641 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le suivi a 0 activites en cours
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000641, 00000000-0000-0000-0000-000000000642 |
      | pointages | 00000000-0000-0000-0000-000000000641, 00000000-0000-0000-0000-000000000642 |

  Scenario: Une transition visant une activite echue pendant qu'une autre est en cours est conservee en conflit
    Given il est "2026-07-15T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6006"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6006                 |
    And j'ai engage l'element "OF 6006" en atelier
    And il est "2026-07-15T08:00:00Z"
    And j'ai pointe sur "OF 6006"
      | id        | 00000000-0000-0000-0000-000000000651 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-15T22:00:00Z"
    And j'ai pointe sur "OF 6006"
      | id        | 00000000-0000-0000-0000-000000000652 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-15T23:00:00Z"
    When je pointe sur "OF 6006"
      | id        | 00000000-0000-0000-0000-000000000653 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000651 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le suivi a 0 activites en cours
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000651, 00000000-0000-0000-0000-000000000652, 00000000-0000-0000-0000-000000000653 |
      | pointages | 00000000-0000-0000-0000-000000000651, 00000000-0000-0000-0000-000000000652, 00000000-0000-0000-0000-000000000653 |

  Scenario: Une fin qui vise une ouverture annulee est conservee en conflit
    Given il est "2026-07-16T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6007"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6007                 |
    And j'ai engage l'element "OF 6007" en atelier
    And il est "2026-07-16T08:00:00Z"
    And j'ai pointe sur "OF 6007"
      | id        | 00000000-0000-0000-0000-000000000661 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-16T12:00:00Z"
    And j'ai pointe sur "OF 6007"
      | id        | 00000000-0000-0000-0000-000000000662 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000661 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    # Annuler l'ouverture que vise la fin est admis : la fin reste, sans activite a terminer.
    Given il est "2026-07-16T13:00:00Z"
    When j'annule l'evenement 0 de "OF 6007"
      | motif | Debut pointe par erreur |
    Then la reponse a le statut http 200
    And le suivi a l'etat "INTERROMPU"
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites |                                      |
      | pointages | 00000000-0000-0000-0000-000000000662 |

  Scenario: Une fin survenue avant la cloture, recue apres elle, est enregistree a son heure
    Given il est "2026-07-17T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6008"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 6008                 |
    And j'ai engage l'element "OF 6008" en atelier
    And il est "2026-07-17T08:00:00Z"
    And j'ai pointe sur "OF 6008"
      | id        | 00000000-0000-0000-0000-000000000671 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-17T13:00:00Z"
    And j'ai cloture "OF 6008"
      | dateDeSurvenue | 2026-07-17T12:00:00Z |
    When je pointe sur "OF 6008"
      | id             | 00000000-0000-0000-0000-000000000672 |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000671 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-07-17T11:00:00Z                 |
    Then la reponse a le statut http 201
    And le suivi a l'etat "CLOTURE"
    And le journal du suivi contient 2 evenements
    And le suivi n'a aucune sequence en conflit
    When je consulte le temps effectif de "OF 6008"
    Then le temps effectif contient
      | activite                             | debut                | fin                  | finAutomatique | aResoudre |
      | 00000000-0000-0000-0000-000000000671 | 2026-07-17T08:00:00Z | 2026-07-17T11:00:00Z | false          | false     |

  Scenario: Le gestionnaire resout le conflit en annulant la transition erronee
    Given il est "2026-07-20T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 7001"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 7001                 |
    And j'ai engage l'element "OF 7001" en atelier
    And il est "2026-07-20T08:00:00Z"
    And j'ai pointe sur "OF 7001"
      | id        | 00000000-0000-0000-0000-000000000701 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-20T12:00:00Z"
    And j'ai pointe sur "OF 7001"
      | id        | 00000000-0000-0000-0000-000000000702 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000701 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-20T17:00:00Z"
    And j'ai pointe sur "OF 7001"
      | id        | 00000000-0000-0000-0000-000000000703 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000701 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-21T09:00:00Z"
    When j'annule l'evenement 1 de "OF 7001"
      | motif | Pas de non conformite ce jour-la |
    # Le conflit disparait au recalcul ; la transition reste au journal, annulee.
    Then la reponse a le statut http 200
    And le suivi n'a aucune sequence en conflit
    And le journal du suivi contient 3 evenements
    And l'evenement 1 du suivi est annule avec le motif "Pas de non conformite ce jour-la"
    When je consulte le temps effectif de "OF 7001"
    Then le temps effectif contient
      | activite                             | categorie | debut                | fin                  | finAutomatique | aResoudre |
      | 00000000-0000-0000-0000-000000000701 | TRAVAIL   | 2026-07-20T08:00:00Z | 2026-07-20T17:00:00Z | false          | false     |

  Scenario: Le gestionnaire resout le conflit en corrigeant la fin de A en fin de la non conformite
    Given il est "2026-07-22T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 7002"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 7002                 |
    And j'ai engage l'element "OF 7002" en atelier
    And il est "2026-07-22T08:00:00Z"
    And j'ai pointe sur "OF 7002"
      | id        | 00000000-0000-0000-0000-000000000711 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-22T12:00:00Z"
    And j'ai pointe sur "OF 7002"
      | id        | 00000000-0000-0000-0000-000000000712 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000711 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-22T17:00:00Z"
    And j'ai pointe sur "OF 7002"
      | id        | 00000000-0000-0000-0000-000000000713 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000711 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-23T09:00:00Z"
    When je corrige l'evenement 2 de "OF 7002"
      | motif          | La fin terminait la non conformite   |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000712 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-07-22T17:00:00Z                 |
    # La fin corrigee reste au journal, annulee, avec son remplacant.
    Then la reponse a le statut http 200
    And le suivi n'a aucune sequence en conflit
    And le journal du suivi contient 4 evenements
    When je consulte le temps effectif de "OF 7002"
    Then le temps effectif contient
      | activite                             | categorie      | debut                | fin                  | finAutomatique | aResoudre |
      | 00000000-0000-0000-0000-000000000711 | TRAVAIL        | 2026-07-22T08:00:00Z | 2026-07-22T12:00:00Z | false          | false     |
      | 00000000-0000-0000-0000-000000000712 | NON_CONFORMITE | 2026-07-22T12:00:00Z | 2026-07-22T17:00:00Z | false          | false     |

  Scenario: Une resolution en plusieurs actes passe par des etats intermediaires en conflit
    # Le gestionnaire insere une non conformite de 12 h a 14 h dans un travail pointe de 08 h a 17 h : chacune des
    # deux transitions qu'il regularise laisse la fin de 17 h contredire le journal, jusqu'a ce qu'il la reporte sur le
    # travail repris.
    Given il est "2026-07-24T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 7003"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 7003                 |
    And j'ai engage l'element "OF 7003" en atelier
    And il est "2026-07-24T08:00:00Z"
    And j'ai pointe sur "OF 7003"
      | id        | 00000000-0000-0000-0000-000000000721 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-24T17:00:00Z"
    And j'ai pointe sur "OF 7003"
      | id        | 00000000-0000-0000-0000-000000000722 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000721 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-25T09:00:00Z"
    When je regularise sur "OF 7003"
      | type           | NON_CONFORMITE                       |
      | intention      | TRANSITION                           |
      | cible          | 00000000-0000-0000-0000-000000000721 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-07-24T12:00:00Z                 |
    Then la reponse a le statut http 201
    And le suivi porte 1 sequence en conflit
    When je regularise sur "OF 7003" en visant l'activite de l'evenement 1
      | type           | DEBUT                |
      | intention      | TRANSITION           |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-07-24T14:00:00Z |
    Then la reponse a le statut http 201
    And le suivi porte 1 sequence en conflit
    When je corrige l'evenement 3 de "OF 7003" en visant l'activite de l'evenement 2
      | motif          | Fin du travail repris |
      | type           | FIN                   |
      | intention      | FIN                   |
      | operateur      | dupont                |
      | poste          | fraiseuse-1           |
      | dateDeSurvenue | 2026-07-24T17:00:00Z  |
    Then la reponse a le statut http 200
    And le suivi n'a aucune sequence en conflit
    And le journal du suivi contient 5 evenements
    When je consulte le temps effectif de "OF 7003"
    Then le temps effectif contient
      | categorie      | debut                | fin                  | finAutomatique | aResoudre |
      | TRAVAIL        | 2026-07-24T08:00:00Z | 2026-07-24T12:00:00Z | false          | false     |
      | NON_CONFORMITE | 2026-07-24T12:00:00Z | 2026-07-24T14:00:00Z | false          | false     |
      | TRAVAIL        | 2026-07-24T14:00:00Z | 2026-07-24T17:00:00Z | false          | false     |

  Scenario: Un conflit se resout sur un element cloture, dont la cloture reste acquise
    Given il est "2026-07-27T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 7004"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 7004                 |
    And j'ai engage l'element "OF 7004" en atelier
    And il est "2026-07-27T08:00:00Z"
    And j'ai pointe sur "OF 7004"
      | id        | 00000000-0000-0000-0000-000000000731 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-27T10:00:00Z"
    And j'ai pointe sur "OF 7004"
      | id        | 00000000-0000-0000-0000-000000000732 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-27T11:00:00Z"
    And j'ai pointe sur "OF 7004"
      | id        | 00000000-0000-0000-0000-000000000733 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000731 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-27T13:00:00Z"
    When je cloture "OF 7004"
      | dateDeSurvenue | 2026-07-27T12:00:00Z |
    Then le suivi a l'etat "CLOTURE"
    And le suivi porte 1 sequence en conflit
    When j'annule l'evenement 2 de "OF 7004"
      | motif | Fin pointee sur la mauvaise activite |
    # Sans la fin contradictoire, la relance court jusqu'a la cloture, qui reste acquise.
    Then la reponse a le statut http 200
    And le suivi a l'etat "CLOTURE"
    And le suivi n'a aucune sequence en conflit
    When je consulte le temps effectif de "OF 7004"
    Then le temps effectif contient
      | activite                             | debut                | fin                  | finAutomatique | aResoudre |
      | 00000000-0000-0000-0000-000000000731 | 2026-07-27T08:00:00Z | 2026-07-27T10:00:00Z | false          | false     |
      | 00000000-0000-0000-0000-000000000732 | 2026-07-27T10:00:00Z | 2026-07-27T12:00:00Z | false          | false     |

  Scenario: Un debut corrige de 08 h a 12 h, lu a 22 h, garde les gestes qui visent son activite
    Given il est "2026-07-28T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 7005"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 7005                 |
    And j'ai engage l'element "OF 7005" en atelier
    And il est "2026-07-28T08:00:00Z"
    And j'ai pointe sur "OF 7005"
      | id        | 00000000-0000-0000-0000-000000000741 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-07-28T22:00:00Z"
    When je corrige l'evenement 0 de "OF 7005"
      | motif          | Demarre a midi       |
      | type           | DEBUT                |
      | intention      | OUVERTURE            |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-07-28T12:00:00Z |
    # L'echeance passe de 21 h a 01 h : l'activite redevient en cours, sous l'identite de son pointage d'origine.
    Then la reponse a le statut http 200
    And le suivi a l'etat "EN_COURS"
    And les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-07-28T12:00:00Z | 00000000-0000-0000-0000-000000000741 | 2026-07-29T01:00:00Z |
    # La fin que le pupitre pointe en visant ce pointage d'origine termine l'activite corrigee, sans conflit.
    Given il est "2026-07-28T23:00:00Z"
    When je pointe sur "OF 7005"
      | id        | 00000000-0000-0000-0000-000000000742 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000741 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le suivi n'a aucune sequence en conflit
    When je consulte le temps effectif de "OF 7005"
    Then le temps effectif contient
      | activite                             | debut                | fin                  | finAutomatique | aResoudre |
      | 00000000-0000-0000-0000-000000000741 | 2026-07-28T12:00:00Z | 2026-07-28T23:00:00Z | false          | false     |
