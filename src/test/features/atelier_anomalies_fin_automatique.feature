@dossier-fin-automatique
Feature: Dossier d'anomalie d'une fin automatique

  # Le dossier d'une fin automatique s'ouvre depuis l'ouvrant actif de l'activite echue. L'echeance se juge a
  # l'instant d'evaluation, a la nanoseconde, et n'est jamais stockee.
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
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-01T22:00:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4501" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie signale une fin automatique
    And le dossier d'anomalie n'est pas en conflit
    And le dossier d'anomalie donne les activites
      | evenement | etat  | debut                | fin                  | duree |
      | 0         | ECHUE | 2044-03-01T08:00:00Z | 2044-03-01T21:00:00Z | PT13H |
    And le perimetre du dossier d'anomalie contient l'activite de l'evenement 0

  Scenario Outline: L'echeance se juge a l'instant de lecture, borne incluse, a la nanoseconde
    Given il est "2044-03-02T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4502 <rang>"
      | categorie | OF             |
      | reference | ANO4502-<rang> |
    And j'ai engage l'element "Anomalie 4502 <rang>" en atelier
    And il est "2044-03-02T08:00:00Z"
    And j'ai pointe sur "Anomalie 4502 <rang>"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "<lecture>"
    When je consulte le dossier d'anomalie de "Anomalie 4502 <rang>" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "<etat>"
    And le dossier d'anomalie declare finAutomatique "<finAutomatique>"
    And le dossier d'anomalie donne <activites> activites

    Examples:
      | rang | lecture                        | etat            | finAutomatique | activites |
      | 1    | 2044-03-02T20:59:00Z           | SANS_ANOMALIE   | false          | 0         |
      | 2    | 2044-03-02T20:59:59.999999999Z | SANS_ANOMALIE   | false          | 0         |
      | 3    | 2044-03-02T21:00:00Z           | FIN_AUTOMATIQUE | true           | 1         |
