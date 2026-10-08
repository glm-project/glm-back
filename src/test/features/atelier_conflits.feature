Feature: Activites que le moteur juge a resoudre

  # Des pointages qui se contredisent sont conserves, jamais refuses. Les activites qu'ils laissent a resoudre ne sont
  # exposees par aucun lecteur : elles ne sont ni en cours ni terminees, et l'echeance ne les termine pas.
  #
  # Chaque scenario engage son element un jour qui lui est propre.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-1" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare l'operateur "dupont" habilite sur "fraiseuse-1"

  Scenario: Une nouvelle ouverture apres des activites que le moteur juge a resoudre est en cours, elles non
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
