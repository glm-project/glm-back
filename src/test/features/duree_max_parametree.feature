Feature: La duree max d'une activite fixee par le gestionnaire gouverne l'atelier et le pupitre

  # La duree max d'une activite est un reglage de l'entreprise. Ces scenarios vivent dans l'entreprise
  # "duree_parametree", dont aucune autre feature ne lit les reglages : chaque scenario y fixe sa propre duree avant tout
  # geste, parce que les reglages d'une entreprise survivent a un scenario.
  #
  # L'activite tient son echeance de la duree en vigueur a son debut : le gestionnaire qui change le reglage ne change
  # que les activites ouvertes ensuite.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "duree_parametree"
    And l'entreprise a declare le poste de travail "fraiseuse-8h" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare l'operateur "dupont" habilite sur "fraiseuse-8h"

  Scenario: Le referentiel du pupitre porte la duree que le gestionnaire a fixee
    Given j'ai fixe la duree max d'une activite a "PT8H"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200
    And le referentiel du pupitre porte la duree maximale d'activite "PT8H"

  Scenario: Le referentiel suit un changement de la duree sans attendre
    Given j'ai fixe la duree max d'une activite a "PT10H"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then le referentiel du pupitre porte la duree maximale d'activite "PT10H"
    Given j'ai fixe la duree max d'une activite a "PT8H30M"
    When je lis le referentiel du pupitre a "2026-05-11T07:05:00Z"
    Then le referentiel du pupitre porte la duree maximale d'activite "PT8H30M"

  Scenario: Une activite commencee sous huit heures se termine automatiquement a 16:00
    Given j'ai fixe la duree max d'une activite a "PT8H"
    And il est "2026-07-01T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6001"
      | categorie | OF   |
      | reference | 6001 |
    And j'ai engage l'element "OF 6001" en atelier
    And il est "2026-07-01T08:00:00Z"
    And j'ai pointe sur "OF 6001"
      | id        | 00000000-0000-0000-0000-000000006001 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-8h                         |
    Given il est "2026-07-01T15:59:00Z"
    When je consulte "OF 6001"
    Then le suivi a l'etat "EN_COURS"
    And les activites en cours sont
      | categorie | depuis               | ouverture                            | echeance             |
      | TRAVAIL   | 2026-07-01T08:00:00Z | 00000000-0000-0000-0000-000000006001 | 2026-07-01T16:00:00Z |
    Given il est "2026-07-01T16:00:00Z"
    When je consulte "OF 6001"
    Then le suivi a l'etat "INTERROMPU"
    And le suivi a 0 activites en cours
    When je consulte le dossier d'anomalie de "OF 6001" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-07-01T08:00:00Z | 2026-07-01T16:00:00Z | PT8H  |

  Scenario: Une fin pointee apres l'echeance de huit heures est ignoree, quand treize heures l'auraient acceptee
    Given j'ai fixe la duree max d'une activite a "PT8H"
    And il est "2026-07-02T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6002"
      | categorie | OF   |
      | reference | 6002 |
    And j'ai engage l'element "OF 6002" en atelier
    And il est "2026-07-02T08:00:00Z"
    And j'ai pointe sur "OF 6002"
      | id        | 00000000-0000-0000-0000-000000006011 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-8h                         |
    Given il est "2026-07-02T17:00:00Z"
    When je pointe sur "OF 6002"
      | id        | 00000000-0000-0000-0000-000000006012 |
      | type      | FIN                                  |
      | operateur | dupont                               |
      | poste     | fraiseuse-8h                         |
    # La fin de 17 h est apres l'echeance de 16 h : elle part en audit (APRES_ECHEANCE) et l'activite garde sa fin
    # automatique. Sous treize heures, la meme fin aurait termine l'activite.
    Then le pointage est ignore
    When je consulte "OF 6002"
    Then le journal du suivi contient 1 evenements
    And le suivi a l'etat "INTERROMPU"
    When je consulte le dossier d'anomalie de "OF 6002" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2026-07-02T08:00:00Z | 2026-07-02T16:00:00Z | PT8H  |

  Scenario: Une activite commencee avant le passage de treize heures a huit heures garde ses treize heures
    Given j'ai fixe la duree max d'une activite a "PT13H"
    And il est "2026-07-03T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 6003"
      | categorie | OF   |
      | reference | 6003 |
    And j'ai engage l'element "OF 6003" en atelier
    And il est "2026-07-03T08:00:00Z"
    And j'ai pointe sur "OF 6003"
      | id        | 00000000-0000-0000-0000-000000006021 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-8h                         |
    And j'ai fixe la duree max d'une activite a "PT8H"
    # A 17 h, l'activite ouverte sous treize heures n'est pas echue : la fin est acceptee et la termine.
    Given il est "2026-07-03T17:00:00Z"
    When je pointe sur "OF 6003"
      | id        | 00000000-0000-0000-0000-000000006022 |
      | type      | FIN                                  |
      | operateur | dupont                               |
      | poste     | fraiseuse-8h                         |
    Then le pointage est accepte
    # L'activite ouverte a 17 h, apres le changement, prend huit heures : elle se termine automatiquement a 01:00.
    When je pointe sur "OF 6003"
      | id        | 00000000-0000-0000-0000-000000006023 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-8h                         |
    Then le pointage est accepte
    Given il est "2026-07-04T01:00:00Z"
    When je consulte "OF 6003"
    Then le suivi a l'etat "INTERROMPU"
    When je consulte le dossier d'anomalie de "OF 6003" depuis l'evenement 2
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 2         | 2026-07-03T17:00:00Z | 2026-07-04T01:00:00Z | PT8H  |
