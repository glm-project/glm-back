Feature: Sequences en conflit

  # Des pointages qui se contredisent sont conserves, jamais refuses ni reinterpretes par le serveur : leur sequence est
  # en conflit, quel que soit leur ordre de reception. Tant qu'elle dure, ses activites sont a resoudre : ni en cours ni terminees, sans fin ni duree, et
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
      | categorie | OF   |
      | reference | 6001 |
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
      | categorie | OF   |
      | reference | 6002 |
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
      | categorie | OF   |
      | reference | 6003 |
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
      | categorie | OF   |
      | reference | 6004 |
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
      | categorie | OF   |
      | reference | 6005 |
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
      | categorie | OF   |
      | reference | 6006 |
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

  Scenario: Une fin survenue avant la cloture, recue apres elle, est enregistree a son heure
    Given il est "2026-07-17T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6008"
      | categorie | OF   |
      | reference | 6008 |
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
