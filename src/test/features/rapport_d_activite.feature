Feature: Rapport d'activite d'un operateur sur une semaine ou un mois

  # Les memes activites que la synthese des heures, sur une semaine ISO ou un mois civil, calendrier Europe/Paris.
  # Les heures sont en UTC. Le rapport parle de temps pointe, jamais de montant.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And la synthese des heures connait le poste "DMU 50" de nature "fraisage"
    And la synthese des heures connait le poste "Tour" de nature "tournage"
    And la synthese des heures suit l'operateur "martin" habilite sur
      | DMU 50 |
      | Tour   |
    And la synthese des heures connait l'element "carter"
    And la synthese des heures connait l'element "bride"

  Scenario: Le resume du mois cumule les elements simultanes et compte une fin automatique jusqu'a son echeance
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And pour la synthese, l'element "bride" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | operateur | poste  | survenue             |
      | A     | DEBUT | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | martin    | DMU 50 | 2026-05-11T07:00:00Z |
      | B     | DEBUT | martin    | DMU 50 | 2026-05-12T06:00:00Z |
    And la synthese des heures recoit sur l'element "bride" les pointages
      | alias | type  | operateur | poste | survenue             |
      | C     | DEBUT | martin    | Tour  | 2026-05-11T06:00:00Z |
      | G     | FIN   | martin    | Tour  | 2026-05-11T07:00:00Z |
    And il est "2026-05-20T10:00:00Z"
    When je consulte le rapport d'activite de "martin" pour le mois 5 de 2026
    Then la reponse a le statut http 200
    And le resume du rapport d'activite est
      | premierJour               | 2026-05-01           |
      | dernierJour               | 2026-05-31           |
      | evaluation                | 2026-05-20T10:00:00Z |
      | tempsPointe.valeur        | PT15H                |
      | dureeNonConformite.valeur | PT0S                 |
      | joursAvecPointage         | 2                    |
      | pointages                 | 3                    |
      | finsAutomatiques          | 1                    |

  Scenario: Le resume d'une semaine ne garde que ses sept jours
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-08T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | operateur | poste  | survenue             |
      | A     | DEBUT | martin    | DMU 50 | 2026-05-08T06:00:00Z |
      | F     | FIN   | martin    | DMU 50 | 2026-05-08T08:00:00Z |
      | B     | DEBUT | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | G     | FIN   | martin    | DMU 50 | 2026-05-11T09:00:00Z |
    And il est "2026-05-20T10:00:00Z"
    When je consulte le rapport d'activite de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And le resume du rapport d'activite est
      | premierJour        | 2026-05-11 |
      | dernierJour        | 2026-05-17 |
      | tempsPointe.valeur | PT3H       |
      | pointages          | 1          |
      | finsAutomatiques   | 0          |

  Scenario Outline: La periode se demande par une semaine ou par un mois, et un seul
    When je consulte le rapport d'activite de "martin" avec "<parametres>"
    Then la reponse a le statut http 400

    Examples:
      | parametres                   |
      | annee=2026                   |
      | annee=2026&semaine=20&mois=5 |
      | annee=2026&mois=13           |
      | annee=2026&mois=0            |
      | annee=2026&semaine=54        |
      | annee=1999&mois=5            |

  Scenario: Un mois qui n'a pas commence est refuse
    Given il est "2026-05-20T10:00:00Z"
    When je consulte le rapport d'activite de "martin" pour le mois 6 de 2026
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:synthese-des-heures:periode-future"

  Scenario: Le rapport d'un operateur inconnu n'existe pas
    Given il est "2026-05-20T10:00:00Z"
    When je consulte le rapport d'activite de l'operateur "99999999-9999-9999-9999-999999999999" pour le mois 5 de 2026
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:synthese-des-heures:operateur-introuvable"

  Scenario: Le rapport est reserve au gestionnaire de l'entreprise
    Given il est "2026-05-20T10:00:00Z"
    And I am logged in as "user" with role "USER" for tenant "impeccmold"
    When je consulte le rapport d'activite de "martin" pour le mois 5 de 2026
    Then la reponse a le statut http 403
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte le rapport d'activite de "martin" pour le mois 5 de 2026
    Then la reponse a le statut http 404
