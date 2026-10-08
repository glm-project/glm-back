Feature: Rejeu durable des gestes du pupitre

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare l'operateur "dupont"
    And il est "2026-05-10T07:00:00Z"

  Scenario: Un pointage rejoue apres cloture conserve le journal du suivi
    Given l'entreprise a cree l'element de fabrication "OF rejeu"
      | categorie | OF    |
      | reference | rejeu |
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

  Scenario: Une fin ciblee rejouee apres une interruption reseau ne cree pas de doublon
    # Le pupitre a pointe la fin hors ligne, a 12 h ; la reponse a son premier envoi s'est perdue. Il renvoie le meme
    # corps, meme identifiant, meme heure et meme cible : le serveur le reconnait.
    Given l'entreprise a cree l'element de fabrication "OF reseau"
      | categorie | OF     |
      | reference | reseau |
    And j'ai engage l'element "OF reseau" en atelier
    And il est "2026-05-10T08:00:00Z"
    And I am logged in as "user" with role "USER"
    And j'ai pointe sur "OF reseau"
      | id        | 00000000-0000-0000-0000-000000000301 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
    Given il est "2026-05-10T13:00:00Z"
    When je pointe sur "OF reseau"
      | id             | 00000000-0000-0000-0000-000000000302 |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000301 |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 201
    Given il est "2026-05-10T14:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi a l'identifiant "00000000-0000-0000-0000-000000000302"
    And l'evenement 1 du suivi vise l'activite de l'evenement 0
    And l'evenement 1 du suivi a survenu a "2026-05-10T12:00:00Z" et a ete saisi a "2026-05-10T13:00:00Z" par "user"

  Scenario: Un identifiant reutilise avec une autre intention et une autre cible est refuse
    Given l'entreprise a cree l'element de fabrication "OF cible"
      | categorie | OF    |
      | reference | cible |
    And j'ai engage l'element "OF cible" en atelier
    And il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000311 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
    Given il est "2026-05-10T09:00:00Z"
    And j'ai pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000313 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000311 |
      | operateur | dupont                               |
    And j'ai pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000312 |
      | type      | NON_CONFORMITE                       |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000312 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 4d7c2a19-8e03-4b56-9f21-c0a1b2d3e4f5 |
      | operateur | dupont                               |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:identifiant-evenement-reutilise"
    When je pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000312 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000000311 |
      | operateur | dupont                               |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:identifiant-evenement-reutilise"
    When je consulte "OF cible"
    Then le journal du suivi contient 3 evenements
    And l'evenement 2 du suivi a l'identifiant "00000000-0000-0000-0000-000000000312"
    And l'evenement 2 du suivi a l'intention "OUVERTURE"

  Scenario: Une fin ciblee acceptee reste soumise aux droits lors du rejeu
    Given l'entreprise a cree l'element de fabrication "OF droits FIN"
      | categorie | OF         |
      | reference | droits FIN |
    And j'ai engage l'element "OF droits FIN" en atelier
    And il est "2026-05-10T08:00:00Z"
    And I am logged in as "user" with role "USER"
    And j'ai pointe sur "OF droits FIN"
      | id        | 00000000-0000-0000-0000-000000000321 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
    Given il est "2026-05-10T12:00:00Z"
    When je pointe sur "OF droits FIN"
      | operateur | dupont                               |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000321 |
    Then la reponse a le statut http 201
    Given I am logged in as "user" with role "ADMIN"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 403
    Given I am logged in as "user" with role "USER"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi vise l'activite de l'evenement 0

  Scenario: Un pointage d'atelier accepte reste soumis aux droits lors du rejeu
    Given l'entreprise a cree l'element de fabrication "OF droits"
      | categorie | OF     |
      | reference | droits |
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

  Scenario: L'ancien contrat de pointage d'atelier sans UUID est refuse
    Given l'entreprise a cree l'element de fabrication "OF sans UUID"
      | categorie | OF        |
      | reference | sans UUID |
    And j'ai engage l'element "OF sans UUID" en atelier
    And il est "2026-05-10T08:00:00Z"
    When je pointe sur "OF sans UUID" sans identifiant de geste
      | operateur | dupont |
      | type      | DEBUT  |
    Then la reponse a le statut http 400
    When je consulte "OF sans UUID"
    Then le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 0 evenements

  Scenario: Une collision de date laisse la fin ciblee acceptee intacte
    Given l'entreprise a cree l'element de fabrication "OF collision FIN"
      | categorie | OF            |
      | reference | collision FIN |
    And j'ai engage l'element "OF collision FIN" en atelier
    And il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF collision FIN"
      | id        | 00000000-0000-0000-0000-000000000331 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
    Given il est "2026-05-10T13:00:00Z"
    When je pointe sur "OF collision FIN"
      | id             | 00000000-0000-0000-0000-000000000332 |
      | operateur      | dupont                               |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000331 |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 201
    When je pointe sur "OF collision FIN"
      | id             | 00000000-0000-0000-0000-000000000332 |
      | operateur      | dupont                               |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000331 |
      | dateDeSurvenue | 2026-05-10T12:30:00Z                 |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:identifiant-evenement-reutilise"
    When je pointe sur "OF collision FIN"
      | id             | 00000000-0000-0000-0000-000000000332 |
      | operateur      | dupont                               |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | cible          | 00000000-0000-0000-0000-000000000331 |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi vise l'activite de l'evenement 0
    And l'evenement 1 du suivi a survenu a "2026-05-10T12:00:00Z" et a ete saisi a "2026-05-10T13:00:00Z" par "gestionnaire"
