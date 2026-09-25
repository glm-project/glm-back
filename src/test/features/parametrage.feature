@parametrage
Feature: Parametrage de l'entreprise

  # Strategie « bornes de fin de journee », decision D1 : l'amplitude maximale d'une journee de travail est parametree
  # par entreprise, 13 h par defaut. Au-dela, une journee sans depart est abandonnee (lot 3). Elle reste strictement
  # sous 24 h : c'est ce qui garantit qu'un retour le lendemain a la meme heure soit toujours une nouvelle arrivee.
  #
  # La valeur par defaut n'est pas une constante du code : elle est semee en base dans chaque schema d'entreprise.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"

  Scenario: Une entreprise neuve a une amplitude maximale de 13 h
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte le parametrage
    Then la reponse a le statut http 200
    And l'amplitude maximale est "PT13H"
    And le parametrage n'a jamais ete modifie

  Scenario: Le gestionnaire fixe l'amplitude maximale, et sa modification est tracee
    Given il est "2026-09-25T09:00:00Z"
    When je fixe l'amplitude maximale a "PT10H"
    Then la reponse a le statut http 200
    And l'amplitude maximale est "PT10H"
    And la derniere modification du parametrage est de "gestionnaire" a "2026-09-25T09:00:00Z"
    When je consulte le parametrage
    Then l'amplitude maximale est "PT10H"
    And la derniere modification du parametrage est de "gestionnaire" a "2026-09-25T09:00:00Z"

  Scenario Outline: Une amplitude a la minute, strictement entre 0 et 24 h, est acceptee
    When je fixe l'amplitude maximale a "<valeur>"
    Then la reponse a le statut http 200
    And l'amplitude maximale est "<valeur>"

    Examples:
      | valeur   |
      | PT1M     |
      | PT12H30M |
      | PT23H59M |

  Scenario Outline: Une amplitude hors bornes ou plus fine que la minute est refusee
    When je fixe l'amplitude maximale a "<valeur>"
    Then la reponse a le statut http 400
    When je consulte le parametrage
    Then l'amplitude maximale est "PT13H"
    And le parametrage n'a jamais ete modifie

    Examples:
      | valeur      |
      | PT0S        |
      | PT-1H       |
      | PT24H       |
      | PT25H       |
      | PT10H0M30S  |
      | PT12H0.5S   |
      | PT23H59M59S |

  Scenario Outline: Une amplitude absente ou illisible est refusee
    When je fixe l'amplitude maximale avec le corps
      """
      <corps>
      """
    Then la reponse a le statut http 400
    When je consulte le parametrage
    Then l'amplitude maximale est "PT13H"

    Examples:
      | corps             |
      | {}                |
      | {"valeur": null}  |
      | {"valeur": "13h"} |
      | {"valeur": 13}    |

  Scenario: La derniere de deux modifications gagne
    Given il est "2026-09-24T09:00:00Z"
    And je fixe l'amplitude maximale a "PT10H"
    Given I am logged in as "leroy" with role "GESTIONNAIRE"
    And il est "2026-09-25T09:00:00Z"
    When je fixe l'amplitude maximale a "PT12H"
    Then la reponse a le statut http 200
    When je consulte le parametrage
    Then l'amplitude maximale est "PT12H"
    And la derniere modification du parametrage est de "leroy" a "2026-09-25T09:00:00Z"

  Scenario: Refixer la meme valeur met la trace a jour
    Given il est "2026-09-24T09:00:00Z"
    And je fixe l'amplitude maximale a "PT13H"
    Then la reponse a le statut http 200
    And la derniere modification du parametrage est de "gestionnaire" a "2026-09-24T09:00:00Z"

  Scenario: Un operateur lit l'amplitude maximale mais ne la modifie pas
    Given I am logged in as "dupont" with role "USER"
    When je consulte le parametrage
    Then la reponse a le statut http 200
    And l'amplitude maximale est "PT13H"
    When je fixe l'amplitude maximale a "PT10H"
    Then la reponse a le statut http 403
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    When je consulte le parametrage
    Then l'amplitude maximale est "PT13H"

  Scenario: Un administrateur technique n'a pas acces au parametrage metier
    Given I am logged in as "admin" with role "ADMIN"
    When je consulte le parametrage
    Then la reponse a le statut http 403
    When je fixe l'amplitude maximale a "PT10H"
    Then la reponse a le statut http 403

  Scenario: Acces refuse a un utilisateur sans entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" without tenant
    When je consulte le parametrage
    Then la reponse a le statut http 403

  Scenario: Le parametrage d'une entreprise ne touche pas celui d'une autre
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je fixe l'amplitude maximale a "PT10H"
    Then la reponse a le statut http 200
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte le parametrage
    Then l'amplitude maximale est "PT13H"
    And le parametrage n'a jamais ete modifie
