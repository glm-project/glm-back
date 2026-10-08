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
      | operateur | dupont                               |
    Given il est "2026-05-10T13:00:00Z"
    When je pointe sur "OF reseau"
      | id             | 00000000-0000-0000-0000-000000000302 |
      | type           | FIN                                  |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 201
    Given il est "2026-05-10T14:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi a l'identifiant "00000000-0000-0000-0000-000000000302"
    And l'evenement 1 du suivi a survenu a "2026-05-10T12:00:00Z" et a ete saisi a "2026-05-10T13:00:00Z" par "user"

  Scenario: Un identifiant deja au journal est un rejeu, quel que soit le contenu renvoye
    Given l'entreprise a cree l'element de fabrication "OF cible"
      | categorie | OF    |
      | reference | cible |
    And j'ai engage l'element "OF cible" en atelier
    And il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000311 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
    Given il est "2026-05-10T09:00:00Z"
    And j'ai pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000313 |
      | type      | FIN                                  |
      | operateur | dupont                               |
    And j'ai pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000312 |
      | type      | NON_CONFORMITE                       |
      | operateur | dupont                               |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000312 |
      | type      | NON_CONFORMITE                       |
      | operateur | dupont                               |
    Then la reponse a le statut http 200
    When je pointe sur "OF cible"
      | id        | 00000000-0000-0000-0000-000000000312 |
      | type      | NON_CONFORMITE                       |
      | operateur | dupont                               |
    Then la reponse a le statut http 200
    When je consulte "OF cible"
    Then le journal du suivi contient 3 evenements
    And l'evenement 2 du suivi a l'identifiant "00000000-0000-0000-0000-000000000312"

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
    Given il est "2026-05-10T12:00:00Z"
    When je pointe sur "OF droits FIN"
      | operateur | dupont |
      | type      | FIN    |
    Then la reponse a le statut http 201
    Given I am logged in as "user" with role "ADMIN"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 403
    Given I am logged in as "user" with role "USER"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements

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

  Scenario: Un renvoi avec une autre heure laisse la fin ciblee acceptee intacte
    Given l'entreprise a cree l'element de fabrication "OF collision FIN"
      | categorie | OF            |
      | reference | collision FIN |
    And j'ai engage l'element "OF collision FIN" en atelier
    And il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF collision FIN"
      | id        | 00000000-0000-0000-0000-000000000331 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
    Given il est "2026-05-10T13:00:00Z"
    When je pointe sur "OF collision FIN"
      | id             | 00000000-0000-0000-0000-000000000332 |
      | operateur      | dupont                               |
      | type           | FIN                                  |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 201
    When je pointe sur "OF collision FIN"
      | id             | 00000000-0000-0000-0000-000000000332 |
      | operateur      | dupont                               |
      | type           | FIN                                  |
      | dateDeSurvenue | 2026-05-10T12:30:00Z                 |
    Then la reponse a le statut http 200
    When je pointe sur "OF collision FIN"
      | id             | 00000000-0000-0000-0000-000000000332 |
      | operateur      | dupont                               |
      | type           | FIN                                  |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi a survenu a "2026-05-10T12:00:00Z" et a ete saisi a "2026-05-10T13:00:00Z" par "gestionnaire"

  Scenario: L'identifiant d'un geste d'un autre OF est un rejeu qui n'ecrit rien
    # L'identifiant est unique dans toute la table des evenements, pas seulement dans le journal du suivi.
    Given l'entreprise a cree l'element de fabrication "OF source"
      | categorie | OF     |
      | reference | source |
    And l'entreprise a cree l'element de fabrication "OF autre"
      | categorie | OF    |
      | reference | autre |
    And j'ai engage l'element "OF source" en atelier
    And j'ai engage l'element "OF autre" en atelier
    And il est "2026-05-10T08:00:00Z"
    And I am logged in as "user" with role "USER"
    And j'ai pointe sur "OF source"
      | id        | 00000000-0000-0000-0000-000000000341 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
    When je pointe sur "OF autre"
      | id        | 00000000-0000-0000-0000-000000000341 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
    Then la reponse a le statut http 200
    And le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 0 evenements
    When je consulte "OF source"
    Then le journal du suivi contient 1 evenements

  Scenario: Un pointage ignore rejoue est refuse de nouveau, sans seconde ligne d'audit
    # Le pupitre n'a pas recu le refus et renvoie le meme corps : le serveur reconnait l'identifiant dans l'audit.
    Given l'entreprise a cree l'element de fabrication "OF ignore"
      | categorie | OF     |
      | reference | ignore |
    And j'ai engage l'element "OF ignore" en atelier
    And il est "2026-05-10T08:00:00Z"
    And I am logged in as "user" with role "USER"
    And j'ai pointe sur "OF ignore"
      | id        | 00000000-0000-0000-0000-000000000351 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
    Given il est "2026-05-10T09:00:00Z"
    When je pointe sur "OF ignore"
      | id        | 00000000-0000-0000-0000-000000000352 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
    Then le pointage est ignore
    Given il est "2026-05-10T10:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then le pointage est ignore
    And la table d'audit des pointages ignores de "OF ignore" contient
      | id                                   | type  | raison        | dateDeSurvenue       | dateDeReception      | dernierAccepte |
      | 00000000-0000-0000-0000-000000000352 | DEBUT | DEJA_EN_COURS | 2026-05-10T09:00:00Z | 2026-05-10T09:00:00Z | evenement 0    |
    When je consulte "OF ignore"
    Then le journal du suivi contient 1 evenements

  Scenario: Un pointage accepte rejoue repond 200 et ne laisse aucune ligne d'audit
    Given l'entreprise a cree l'element de fabrication "OF accepte"
      | categorie | OF      |
      | reference | accepte |
    And j'ai engage l'element "OF accepte" en atelier
    And il est "2026-05-10T08:00:00Z"
    And I am logged in as "user" with role "USER"
    When je pointe sur "OF accepte"
      | id        | 00000000-0000-0000-0000-000000000361 |
      | operateur | dupont                               |
      | type      | DEBUT                                |
    Then le pointage est accepte
    Given il est "2026-05-10T09:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then le pointage est un rejeu
    And la table d'audit des pointages ignores de "OF accepte" est vide
    When je consulte "OF accepte"
    Then le journal du suivi contient 1 evenements
