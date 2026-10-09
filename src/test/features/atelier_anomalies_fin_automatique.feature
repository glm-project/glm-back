@dossier-fin-automatique
Feature: Dossier d'une fin automatique

  # Le dossier d'une fin automatique s'ouvre depuis l'ouvrant de l'activite echue : 200 tant qu'aucune fin reelle ne l'a
  # terminee, 404 sinon. L'echeance se juge a l'instant d'evaluation, a la nanoseconde, et n'est jamais stockee.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-anomalie" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "rectifieuse-anomalie" de nature "rectification" et de cout horaire "60"
    And l'entreprise a declare l'operateur "dupont-anomalie" habilite sur "fraiseuse-anomalie" et "rectifieuse-anomalie" avec un taux horaire de "22"

  Scenario: Une activite oubliee ouvre un dossier de fin automatique
    Given il est "2044-03-01T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4501"
      | categorie | OF      |
      | reference | ANO4501 |
    And j'ai engage l'element "Anomalie 4501" en atelier
    And il est "2044-03-01T08:00:00Z"
    And j'ai pointe sur "Anomalie 4501"
      | id        | 00000000-0000-0000-0000-000000045011 |
      | type      | DEBUT                                |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-01T22:00:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4501" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2044-03-01T08:00:00Z | 2044-03-01T21:00:00Z | PT13H |
    And le dossier d'anomalie donne les pointages des evenements "0"
    # L'en-tete du dossier nomme l'element de fabrication concerne.
    And le dossier d'anomalie donne l'element du suivi

  Scenario Outline: L'echeance se juge a l'instant de lecture, borne incluse, a la nanoseconde
    Given il est "2044-03-02T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4502 <rang>"
      | categorie | OF             |
      | reference | ANO4502-<rang> |
    And j'ai engage l'element "Anomalie 4502 <rang>" en atelier
    And il est "2044-03-02T08:00:00Z"
    And j'ai pointe sur "Anomalie 4502 <rang>"
      | type      | DEBUT              |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "<lecture>"
    When je consulte le dossier d'anomalie de "Anomalie 4502 <rang>" depuis l'evenement 0
    Then la reponse a le statut http <statut>

    Examples:
      | rang | lecture                        | statut |
      | 1    | 2044-03-02T20:59:00Z           | 404    |
      | 2    | 2044-03-02T20:59:59.999999999Z | 404    |
      | 3    | 2044-03-02T21:00:00Z           | 200    |

  Scenario: Le dossier ne rend que les pointages de la cle de l'activite echue
    Given il est "2044-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4503"
      | categorie | OF      |
      | reference | ANO4503 |
    And j'ai engage l'element "Anomalie 4503" en atelier
    And il est "2044-03-03T08:00:00Z"
    And j'ai pointe sur "Anomalie 4503"
      | type      | DEBUT              |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-03T09:00:00Z"
    And j'ai pointe sur "Anomalie 4503"
      | type      | DEBUT                |
      | operateur | dupont-anomalie      |
      | poste     | rectifieuse-anomalie |
    And il est "2044-03-03T10:00:00Z"
    And j'ai pointe sur "Anomalie 4503"
      | type      | FIN                |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-03T11:00:00Z"
    And j'ai pointe sur "Anomalie 4503"
      | type      | DEBUT              |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-04T01:00:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4503" depuis l'evenement 3
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne les pointages des evenements "0,2,3"
