Feature: Echeance et fin automatique des activites

  # Une activite que rien n'a terminee se termine automatiquement a son echeance, son debut plus 13 heures ecoulees,
  # avec une anomalie. Rien n'est ecrit : la fin automatique se lit a l'instant d'evaluation, ici l'horloge figee du
  # scenario, et la meme regle gouverne l'interpretation des gestes recus ensuite.
  #
  # Chaque scenario engage son element un jour qui lui est propre : la liste des elements engages, filtree sur ce jour,
  # ne rend que lui.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-1" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare l'operateur "dupont" habilite sur "fraiseuse-1"

  Scenario: Une activite commencee a 08:00 est encore en cours a 20:59
    Given il est "2026-06-01T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5001"
      | categorie | OF   |
      | reference | 5001 |
    And j'ai engage l'element "OF 5001" en atelier
    And il est "2026-06-01T08:00:00Z"
    And j'ai pointe sur "OF 5001"
      | id        | 00000000-0000-0000-0000-000000000501 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-01T20:59:00Z"
    When je consulte "OF 5001"
    Then le suivi a l'etat "EN_COURS"
    And les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-06-01T08:00:00Z | 00000000-0000-0000-0000-000000000501 | 2026-06-01T21:00:00Z |
    # La grille rend la meme activite en cours, avec son pointage ouvrant et son echeance.
    And je retiens les informations du suivi hors journal
    When je liste les elements engages entre "2026-06-01T00:00:00Z" et "2026-06-01T23:59:59Z"
    Then la grille contient les memes informations sans journal

  Scenario: Une activite que rien n'a terminee se termine automatiquement a 21:00, et garde cette borne ensuite
    Given il est "2026-06-02T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5002"
      | categorie | OF   |
      | reference | 5002 |
    And j'ai engage l'element "OF 5002" en atelier
    And il est "2026-06-02T08:00:00Z"
    And j'ai pointe sur "OF 5002"
      | id        | 00000000-0000-0000-0000-000000000511 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-02T21:00:00Z"
    When je consulte "OF 5002"
    Then le suivi a l'etat "INTERROMPU"
    And le suivi a 0 activites en cours
    When je consulte le dossier d'anomalie de "OF 5002" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-02T08:00:00Z | 2026-06-02T21:00:00Z | PT13H |
    # Lue le lendemain, l'activite garde la meme borne : l'echeance, pas l'instant de la lecture.
    Given il est "2026-06-03T09:00:00Z"
    When je consulte le dossier d'anomalie de "OF 5002" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-02T08:00:00Z | 2026-06-02T21:00:00Z | PT13H |

  Scenario: Une activite que rien n'a terminee expire sans autre geste
    # Le travail commence a 08:00 se termine automatiquement a son echeance, 21:00.
    Given il est "2026-06-24T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5016"
      | categorie | OF   |
      | reference | 5016 |
    And j'ai engage l'element "OF 5016" en atelier
    And il est "2026-06-24T08:00:00Z"
    And j'ai pointe sur "OF 5016"
      | id        | 00000000-0000-0000-0000-0000000005f1 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-25T09:00:00Z"
    When je consulte le dossier d'anomalie de "OF 5016" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-24T08:00:00Z | 2026-06-24T21:00:00Z | PT13H |

  Scenario: Une relance avant l'echeance termine l'activite precedente a son heure
    Given il est "2026-06-04T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5003"
      | categorie | OF   |
      | reference | 5003 |
    And j'ai engage l'element "OF 5003" en atelier
    And il est "2026-06-04T08:00:00Z"
    And j'ai pointe sur "OF 5003"
      | id        | 00000000-0000-0000-0000-000000000521 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-04T10:00:00Z"
    And j'ai pointe sur "OF 5003"
      | id        | 00000000-0000-0000-0000-000000000522 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-06-04T10:00:00Z | 00000000-0000-0000-0000-000000000522 | 2026-06-04T23:00:00Z |

  Scenario: Une relance apres l'echeance laisse sa borne automatique a l'activite precedente
    Given il est "2026-06-05T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5004"
      | categorie | OF   |
      | reference | 5004 |
    And j'ai engage l'element "OF 5004" en atelier
    And il est "2026-06-05T08:00:00Z"
    And j'ai pointe sur "OF 5004"
      | id        | 00000000-0000-0000-0000-000000000531 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-05T23:00:00Z"
    When je pointe sur "OF 5004"
      | id        | 00000000-0000-0000-0000-000000000532 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-06-05T23:00:00Z | 00000000-0000-0000-0000-000000000532 | 2026-06-06T12:00:00Z |
    # Rien ne couvre 21:00 - 23:00 : la relance ne prolonge pas l'activite echue.
    When je consulte le dossier d'anomalie de "OF 5004" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-05T08:00:00Z | 2026-06-05T21:00:00Z | PT13H |

  Scenario: Travail a 08 h puis non conformite a 12 h
    Given il est "2026-06-06T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5005"
      | categorie | OF   |
      | reference | 5005 |
    And j'ai engage l'element "OF 5005" en atelier
    And il est "2026-06-06T08:00:00Z"
    And j'ai pointe sur "OF 5005"
      | id        | 00000000-0000-0000-0000-000000000541 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-06T12:00:00Z"
    And j'ai pointe sur "OF 5005"
      | id        | 00000000-0000-0000-0000-000000000543 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000541 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    When je pointe sur "OF 5005"
      | id        | 00000000-0000-0000-0000-000000000542 |
      | type      | NON_CONFORMITE                       |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    # Les 4 h de travail sont terminees a 12 h ; la non conformite a sa propre echeance, a 01 h le lendemain.
    Then le suivi a l'etat "EN_COURS"
    And les activites en cours sont
      | categorie      | depuis               | ouverture                            | echeance             |
      | NON_CONFORMITE | 2026-06-06T12:00:00Z | 00000000-0000-0000-0000-000000000542 | 2026-06-07T01:00:00Z |

  Scenario: Une fin pointee a 17 h et recue apres la fin automatique la remplace
    Given il est "2026-06-08T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5006"
      | categorie | OF   |
      | reference | 5006 |
    And j'ai engage l'element "OF 5006" en atelier
    And il est "2026-06-08T08:00:00Z"
    And j'ai pointe sur "OF 5006"
      | id        | 00000000-0000-0000-0000-000000000551 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-08T22:00:00Z"
    When je consulte le dossier d'anomalie de "OF 5006" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-08T08:00:00Z | 2026-06-08T21:00:00Z | PT13H |
    # Le pupitre, reste hors ligne, publie le lendemain la fin pointee a 17 h.
    Given il est "2026-06-09T09:00:00Z"
    When je pointe sur "OF 5006"
      | id             | 00000000-0000-0000-0000-000000000552 |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000551 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-06-08T17:00:00Z                 |
    Then la reponse a le statut http 201
    When je consulte le dossier d'anomalie de "OF 5006" depuis l'evenement 0
    Then la reponse a le statut http 404

  Scenario: Une fin pointee a 23 h apres la fin automatique de 21 h est conservee sans effet
    Given il est "2026-06-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5007"
      | categorie | OF   |
      | reference | 5007 |
    And j'ai engage l'element "OF 5007" en atelier
    And il est "2026-06-10T08:00:00Z"
    And j'ai pointe sur "OF 5007"
      | id        | 00000000-0000-0000-0000-000000000561 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-10T23:00:00Z"
    When je pointe sur "OF 5007"
      | id        | 00000000-0000-0000-0000-000000000562 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000561 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    # La fin est enregistree, pas absorbee ; elle ne prolonge pas l'activite, qui garde 13 h et son anomalie.
    Then la reponse a le statut http 201
    And le journal du suivi contient 2 evenements
    And le suivi a l'etat "INTERROMPU"
    When je consulte le dossier d'anomalie de "OF 5007" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-10T08:00:00Z | 2026-06-10T21:00:00Z | PT13H |

  Scenario: Une fin puis une non conformite pointees a 23 h ouvrent la non conformite a son heure
    Given il est "2026-06-11T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5008"
      | categorie | OF   |
      | reference | 5008 |
    And j'ai engage l'element "OF 5008" en atelier
    And il est "2026-06-11T08:00:00Z"
    And j'ai pointe sur "OF 5008"
      | id        | 00000000-0000-0000-0000-000000000571 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-11T23:00:00Z"
    And j'ai pointe sur "OF 5008"
      | id        | 00000000-0000-0000-0000-000000000573 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000571 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    When je pointe sur "OF 5008"
      | id        | 00000000-0000-0000-0000-000000000572 |
      | type      | NON_CONFORMITE                       |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And les activites en cours sont
      | categorie      | depuis               | ouverture                            | echeance             |
      | NON_CONFORMITE | 2026-06-11T23:00:00Z | 00000000-0000-0000-0000-000000000572 | 2026-06-12T12:00:00Z |
    # Le travail garde sa borne automatique ; rien ne couvre 21:00 - 23:00.
    When je consulte le dossier d'anomalie de "OF 5008" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-06-11T08:00:00Z | 2026-06-11T21:00:00Z | PT13H |

  Scenario: Le gestionnaire regularise une fin a 23 h apres la fin automatique de 21 h
    Given il est "2026-06-12T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5009"
      | categorie | OF   |
      | reference | 5009 |
    And j'ai engage l'element "OF 5009" en atelier
    And il est "2026-06-12T08:00:00Z"
    And j'ai pointe sur "OF 5009"
      | id        | 00000000-0000-0000-0000-000000000581 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-13T09:00:00Z"
    When je regularise sur "OF 5009"
      | activite       | 00000000-0000-0000-0000-000000000581 |
      | dateDeSurvenue | 2026-06-12T23:00:00Z                 |
    Then la reponse a le statut http 201
    And l'evenement 1 du suivi est une regularisation de "dupont" saisie par "gestionnaire"
    # Seule une regularisation etablit une fin au-dela de l'echeance : l'activite n'a plus de dossier de fin automatique.
    When je consulte le dossier d'anomalie de "OF 5009" depuis l'evenement 0
    Then la reponse a le statut http 404

  Scenario: Une fin pointee exactement a l'echeance l'emporte sur la fin automatique
    Given il est "2026-06-17T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5011"
      | categorie | OF   |
      | reference | 5011 |
    And j'ai engage l'element "OF 5011" en atelier
    And il est "2026-06-17T08:00:00Z"
    And j'ai pointe sur "OF 5011"
      | id        | 00000000-0000-0000-0000-0000000005a1 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-17T21:00:00Z"
    When je pointe sur "OF 5011"
      | id        | 00000000-0000-0000-0000-0000000005a2 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-0000000005a1 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    When je consulte le dossier d'anomalie de "OF 5011" depuis l'evenement 0
    Then la reponse a le statut http 404

  Scenario: Une fin puis une non conformite pointees a 12 h et recues le lendemain sont rejouees a leur heure
    Given il est "2026-06-19T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5013"
      | categorie | OF   |
      | reference | 5013 |
    And j'ai engage l'element "OF 5013" en atelier
    And il est "2026-06-19T08:00:00Z"
    And j'ai pointe sur "OF 5013"
      | id        | 00000000-0000-0000-0000-0000000005c1 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-06-20T09:00:00Z"
    And j'ai pointe sur "OF 5013"
      | id             | 00000000-0000-0000-0000-0000000005c3 |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-0000000005c1 |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-06-19T12:00:00Z                 |
    When je pointe sur "OF 5013"
      | id             | 00000000-0000-0000-0000-0000000005c2 |
      | type           | NON_CONFORMITE                       |
      | intention      | OUVERTURE                            |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-06-19T12:00:00Z                 |
    Then la reponse a le statut http 201
    And le suivi a l'etat "INTERROMPU"
    And le suivi a 0 activites en cours
    # Le travail compte 4 h, sans anomalie ; la non conformite, lue le lendemain, est terminee a son echeance de 01 h.
    When je consulte le dossier d'anomalie de "OF 5013" depuis l'evenement 0
    Then la reponse a le statut http 404
    When je consulte le dossier d'anomalie de "OF 5013" depuis l'evenement 2
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 2         | 2026-06-19T12:00:00Z | 2026-06-20T01:00:00Z | PT13H |

  Scenario: L'echeance compte 13 heures ecoulees, meme au passage a l'heure d'ete
    # 00:30 UTC le 29 mars 2026, c'est 01:30 a Paris ; 13 heures plus tard, 13:30 UTC, il est 15:30 a Paris.
    Given il est "2026-03-29T00:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 5015"
      | categorie | OF   |
      | reference | 5015 |
    And j'ai engage l'element "OF 5015" en atelier
    And il est "2026-03-29T00:30:00Z"
    And j'ai pointe sur "OF 5015"
      | id        | 00000000-0000-0000-0000-0000000005e1 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-03-29T13:29:59Z"
    When je consulte "OF 5015"
    Then les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-03-29T00:30:00Z | 00000000-0000-0000-0000-0000000005e1 | 2026-03-29T13:30:00Z |
    Given il est "2026-03-29T13:30:00Z"
    When je consulte "OF 5015"
    Then le suivi a 0 activites en cours
