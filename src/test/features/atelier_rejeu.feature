Feature: Rejeu durable des gestes du pupitre

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare l'operateur "dupont"
    And il est "2026-05-10T07:00:00Z"

  Scenario: Un depart rejoue apres une nouvelle venue conserve la premiere journee
    Given je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "matin"
    And il est "2026-05-10T12:00:00Z"
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000101 |
      | operateur | dupont                               |
      | type      | DEPART                               |
    Then la reponse a le statut http 201
    Given il est "2026-05-10T13:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "apres-midi"
    And il est "2026-05-10T14:00:00Z"
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000101 |
      | operateur | dupont                               |
      | type      | DEPART                               |
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"
    And la journee a l'etat "ABSENT"
    And le journal de la journee contient 2 evenements
    And l'evenement 1 de la journee a survenu a "2026-05-10T12:00:00Z" et a ete saisi a "2026-05-10T12:00:00Z" par "gestionnaire"
    When je consulte ma journee
    Then la reponse designe la journee "apres-midi"
    And la journee a l'etat "PRESENT"
    And le journal de la journee contient 1 evenements

  Scenario: Une arrivee rejouee apres le depart ne rouvre pas la journee
    Given je suis arrive
      | id        | 00000000-0000-0000-0000-000000000102 |
      | operateur | dupont                               |
    And je retiens la journee sous le nom "matin"
    And il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    Given il est "2026-05-10T14:00:00Z"
    When j'arrive
      | id        | 00000000-0000-0000-0000-000000000102 |
      | operateur | dupont                               |
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"
    And la journee a l'etat "ABSENT"
    And le journal de la journee contient 2 evenements
    And l'evenement 0 de la journee a survenu a "2026-05-10T07:00:00Z" et a ete saisi a "2026-05-10T07:00:00Z" par "gestionnaire"
    When je liste les journees de "dupont"
    Then la liste des journees contient 1 journees

  Scenario: Un pointage rejoue apres cloture conserve le journal du suivi
    Given l'entreprise a cree l'element de fabrication "OF rejeu"
      | type      | ORDRE_DE_FABRICATION |
      | reference | rejeu                |
    And j'ai engage l'element "OF rejeu" en atelier
    And il est "2026-05-10T09:00:00Z"
    And I am logged in as "user" with role "USER"
    When je pointe sur "OF rejeu"
      | operateur      | dupont               |
      | type           | DEBUT                |
      | dateDeSurvenue | 2026-05-10T08:00:00Z |
    Then la reponse a le statut http 201
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And il est "2026-05-10T12:00:00Z"
    When je cloture "OF rejeu" a l'instant present
    Then la reponse a le statut http 200
    Given I am logged in as "user" with role "USER"
    And il est "2026-05-10T14:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le suivi a l'etat "CLOTURE"
    And le journal du suivi contient 1 evenements
    And l'evenement 0 du suivi a survenu a "2026-05-10T08:00:00Z" et a ete saisi a "2026-05-10T09:00:00Z" par "user"

  Scenario: Une arrivee acceptee reste soumise aux droits lors du rejeu
    Given I am logged in as "user" with role "USER"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 201
    Given I am logged in as "user" with role "ADMIN"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 403
    Given I am logged in as "user" with role "USER"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal de la journee contient 1 evenements

  Scenario: Un pointage de presence accepte reste soumis aux droits lors du rejeu
    Given je suis arrive
      | operateur | dupont |
    And il est "2026-05-10T12:00:00Z"
    And I am logged in as "user" with role "USER"
    When je pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    Then la reponse a le statut http 201
    Given I am logged in as "user" with role "ADMIN"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 403
    Given I am logged in as "user" with role "USER"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal de la journee contient 2 evenements

  Scenario: Un pointage d'atelier accepte reste soumis aux droits lors du rejeu
    Given l'entreprise a cree l'element de fabrication "OF droits"
      | type      | ORDRE_DE_FABRICATION |
      | reference | droits               |
    And j'ai engage l'element "OF droits" en atelier
    And il est "2026-05-10T08:00:00Z"
    And I am logged in as "user" with role "USER"
    When je pointe sur "OF droits"
      | operateur | dupont |
      | type      | DEBUT  |
    Then la reponse a le statut http 201
    Given I am logged in as "user" with role "ADMIN"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 403
    Given I am logged in as "user" with role "USER"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal du suivi contient 1 evenements

  Scenario: L'ancien contrat d'arrivee sans UUID est refuse
    When j'arrive sans identifiant de geste
      | operateur | dupont |
    Then la reponse a le statut http 400
    When je liste les journees de "dupont"
    Then la liste des journees contient 0 journees

  Scenario: L'ancien contrat de presence sans UUID est refuse
    Given je suis arrive
      | operateur | dupont |
    And il est "2026-05-10T12:00:00Z"
    When je pointe ma presence sans identifiant de geste
      | operateur | dupont |
      | type      | DEPART |
    Then la reponse a le statut http 400
    When je consulte ma journee
    Then la journee a l'etat "PRESENT"
    And le journal de la journee contient 1 evenements

  Scenario: L'ancien contrat de pointage d'atelier sans UUID est refuse
    Given l'entreprise a cree l'element de fabrication "OF sans UUID"
      | type      | ORDRE_DE_FABRICATION |
      | reference | sans UUID            |
    And j'ai engage l'element "OF sans UUID" en atelier
    And il est "2026-05-10T08:00:00Z"
    When je pointe sur "OF sans UUID" sans identifiant de geste
      | operateur | dupont |
      | type      | DEBUT  |
    Then la reponse a le statut http 400
    When je consulte "OF sans UUID"
    Then le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 0 evenements

  Scenario: Une collision de contenu laisse le geste accepte intact
    When j'arrive
      | id             | 00000000-0000-0000-0000-000000000103 |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T07:00:00Z                 |
    Then la reponse a le statut http 201
    Given il est "2026-05-10T08:00:00Z"
    When j'arrive
      | id             | 00000000-0000-0000-0000-000000000103 |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T08:00:00Z                 |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:identifiant-evenement-reutilise"
    When j'arrive
      | id             | 00000000-0000-0000-0000-000000000103 |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T07:00:00Z                 |
    Then la reponse a le statut http 200
    And le journal de la journee contient 1 evenements
    And l'evenement 0 de la journee a survenu a "2026-05-10T07:00:00Z" et a ete saisi a "2026-05-10T07:00:00Z" par "gestionnaire"
