Feature: Liste des anomalies de presence

  # Strategie « bornes de fin de journee », lot 6 (D11) : sans cette liste, la fin presumee cacherait l'oubli au lieu
  # de le signaler. Deux types : la journee abandonnee sans depart, et la journee fermee au-dela du seuil
  # d'amplitude (13 h par defaut). Rien n'est stocke : une ligne disparait des que la regularisation la resout.
  #
  # Les scenarios filtrent sur leur operateur : le schema est partage avec tous les autres.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare l'operateur "dupont"

  Scenario: Une journee sans depart apparait une fois abandonnee, puis disparait a la regularisation
    # E2 : Dupont part lundi sans rien pointer.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T19:00:00Z"
    When je consulte les anomalies de "dupont"
    Then la reponse a le statut http 200
    And il n'y a aucune anomalie
    Given il est "2026-05-11T09:00:00Z"
    When je consulte les anomalies de "dupont"
    Then les anomalies sont
      | type                | arrivee              | operateur.nom |
      | JOURNEE_SANS_DEPART | 2026-05-10T07:00:00Z | dupont        |
    Given il est "2026-05-11T09:15:00Z"
    And j'ai regularise ma journee
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T17:00:00Z |
    When je consulte les anomalies de "dupont"
    Then il n'y a aucune anomalie

  Scenario: Une journee fermee au-dela du seuil est une amplitude excessive
    # E4 : un depart saisi a 23:00 sur une journee arrivee a 07:00 donne 16 h. Depuis le lot 3, un geste pointe a
    # 23:00 ouvrirait une nouvelle journee : seul un acte du gestionnaire peut produire cette amplitude.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T09:15:00Z"
    And j'ai regularise ma journee
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T23:00:00Z |
    When je consulte les anomalies de "dupont"
    Then les anomalies sont
      | type                | arrivee              | depart               | amplitude |
      | AMPLITUDE_EXCESSIVE | 2026-05-10T07:00:00Z | 2026-05-10T23:00:00Z | PT16H     |

  Scenario: Un depart pointe apres le seuil laisse une journee sans depart, jamais une amplitude excessive
    # Le geste tardif ouvre une journee de duree nulle ; c'est la journee du matin, sans depart, qui est signalee.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T23:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    When je consulte les anomalies de "dupont"
    Then les anomalies sont
      | type                | arrivee              |
      | JOURNEE_SANS_DEPART | 2026-05-10T07:00:00Z |

  Scenario: Les anomalies se filtrent par type, la plus recente d'abord
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T06:00:00Z"
    And j'ai regularise ma journee
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T23:00:00Z |
    Given il est "2026-05-11T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-12T09:00:00Z"
    When je consulte les anomalies de "dupont"
    Then les anomalies sont
      | type                | arrivee              |
      | JOURNEE_SANS_DEPART | 2026-05-11T07:00:00Z |
      | AMPLITUDE_EXCESSIVE | 2026-05-10T07:00:00Z |
    When je consulte les anomalies de "dupont" de type "AMPLITUDE_EXCESSIVE"
    Then les anomalies sont
      | type                | arrivee              |
      | AMPLITUDE_EXCESSIVE | 2026-05-10T07:00:00Z |

  Scenario: Un type d'anomalie inconnu est refuse
    When je consulte les anomalies de "dupont" de type "INCONNU"
    Then la reponse a le statut http 400

  Scenario: Un operateur ne consulte pas la liste des anomalies
    Given I am logged in as "dupont" with role "USER"
    When je consulte les anomalies
    Then la reponse a le statut http 403

  Scenario: Un administrateur technique n'a pas acces aux anomalies
    Given I am logged in as "admin" with role "ADMIN"
    When je consulte les anomalies
    Then la reponse a le statut http 403

  Scenario: Les anomalies d'une entreprise ne sont pas visibles depuis une autre
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T09:00:00Z"
    And I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte les anomalies de "dupont"
    Then la reponse a le statut http 200
    And il n'y a aucune anomalie
