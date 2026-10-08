Feature: Synthese des heures hebdomadaire d'un operateur

  # Activites interpretees par atelier, calendrier Europe/Paris et journal brut independant.
  # Les tableaux d'activite restent paralleles a feuille_de_temps.feature. Les heures sont en UTC.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And la synthese des heures suit l'operateur "dupont"
    And la synthese des heures connait le poste "DMU 50" de nature "Fraisage"
    And la synthese des heures connait le poste "Tour" de nature "Tournage"
    And la synthese des heures suit l'operateur "martin" habilite sur
      | DMU 50 |
      | Tour   |
    And la synthese des heures connait l'element "carter"
    And la synthese des heures connait l'element "bride"

  Scenario: Une semaine sans pointage rend sept jours vides
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la synthese des heures porte les jours
      | 2026-05-11 |
      | 2026-05-12 |
      | 2026-05-13 |
      | 2026-05-14 |
      | 2026-05-15 |
      | 2026-05-16 |
      | 2026-05-17 |
    And chaque jour de la synthese ne porte aucun pointage et une duree de "PT0S"
    And la duree operationnelle totale de la semaine est "PT0S"

  Scenario Outline: La synthese refuse un instant mal forme ou vide explicitement fourni
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026 avec evaluation "<evaluation>"
    Then la reponse a le statut http 400
    And la synthese des heures refusee ne porte aucun rapport

    Examples:
      | evaluation     |
      | pas-un-instant |
      |                |

  Scenario: La synthese refuse le depassement minimal des deux minutes futures
    Given il est "2026-05-11T21:00:05Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T21:02:05.000000001Z"
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:synthese-des-heures:evaluation-future"
    And la synthese des heures refusee ne porte aucun rapport

  Scenario Outline: La synthese accepte un instant passe et la limite future incluse
    Given il est "2026-05-11T21:00:05Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026 avec evaluation "<evaluation>"
    Then la reponse a le statut http 200
    And la synthese des heures est evaluee a "<echo>"
    And chaque jour de la synthese ne porte aucun pointage et une duree de "PT0S"
    And la duree operationnelle totale de la semaine est "PT0S"

    Examples:
      | evaluation                     | echo                           |
      | 2020-01-01T00:00:00Z           | 2020-01-01T00:00:00Z           |
      | 2026-05-11T21:02:05Z           | 2026-05-11T21:02:05Z           |
      | 2026-05-11T21:02:04.999999999Z | 2026-05-11T21:02:04.999999999Z |
      | 2026-05-11T23:02:05+02:00      | 2026-05-11T21:02:05Z           |

  Scenario: Une fin coupe le travail, un debut le relance, et la coupure ne compte pas
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T11:00:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And le jour "2026-05-11" a une duree operationnelle de "PT8H55M"
    And la duree operationnelle totale de la semaine est "PT8H55M"

  Scenario: Une non conformite compte dans l'element, a part
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" enregistre "NON_CONFORMITE" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T09:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     | categorie | duree   | dureeNonConformite |
      | carter | MOULE     | PT3H55M | PT1H               |

  Scenario: Un element travaille sur deux postes porte deux couples de poste et de nature
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "Tour" a "2026-05-11T06:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then l'element "carter" de la synthese porte les postes
      | poste  | nature   |
      | DMU 50 | Fraisage |
      | Tour   | Tournage |

  Scenario: Un element reengage apres cloture reste un seul element
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And pour la synthese, l'element "carter" est cloture a "2026-05-11T07:00:00Z"
    And pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T08:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:05:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T12:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     | duree   |
      | carter | PT5H50M |

  Scenario: Le journal brut conserve les seuls gestes d'element
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:00:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le journal du "2026-05-11" est
      | type  | dateDeSurvenue       | element | poste  |
      | DEBUT | 2026-05-11T05:00:00Z | carter  | DMU 50 |
      | FIN   | 2026-05-11T15:00:00Z | carter  | DMU 50 |

  Scenario: Les elements suivent leur premiere apparition dans la semaine
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And pour la synthese, l'element "bride" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "bride" au poste "Tour" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T06:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     |
      | bride  |
      | carter |

  Scenario: La reference et la description d'un element sont relues au referentiel
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And la fiche de l'element "carter" est revisee avec la reference "1015-B" et la description "Carter de pompe revise"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then l'element "carter" de la synthese porte sa fiche revisee

  Scenario: Une synthese ne se lit pas pour un operateur inconnu
    When je consulte la synthese des heures de l'operateur "11111111-2222-3333-4444-555555555555" pour la semaine 20 de 2026
    Then la reponse a le statut http 404

  Scenario: Une semaine hors bornes est refusee
    When je consulte la synthese des heures de "dupont" pour la semaine 54 de 2026
    Then la reponse a le statut http 400

  Scenario: Un intervalle termine sans arrivee garde toutes ses bornes
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT2H"

  Scenario: Un poste de nuit termine sans arrivee est coupe entre deux semaines
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-10T18:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 19 de 2026
    Then la duree operationnelle totale de la semaine est "PT4H"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT8H"

  Scenario: Une activite oubliee est terminee automatiquement a treize heures
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And il est "2026-05-11T19:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"
    And la synthese des heures est evaluee a "2026-05-11T19:00:00Z"

  Scenario: A vingt heures cinquante-neuf une activite sans poste est encore en cours
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | survenue             |
      | A     | DEBUT | OUVERTURE | dupont    | 2026-05-11T06:00:00Z |
    And il est "2026-05-11T18:59:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT0S"

  Scenario: La fin automatique du dimanche se retrouve sans pointage du lundi
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-10T20:00:00Z |
    And il est "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT11H"
    And les pointages du "2026-05-11" sont
      | type | dateDeSurvenue |
    And les elements de la synthese sont
      | id     | duree |
      | carter | PT11H |
    When je consulte la synthese des heures de "martin" pour la semaine 19 de 2026
    Then la duree operationnelle totale de la semaine est "PT2H"

  Scenario: Une fin du lundi partage une activite du dimanche entre deux semaines
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-10T20:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T01:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 19 de 2026
    Then la duree operationnelle totale de la semaine est "PT2H"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT3H"

  Scenario: Une relance avant echeance termine sa precedente activite
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | B     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T12:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT6H"

  Scenario: Une relance apres echeance conserve un trou entre les activites
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | B     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"

  Scenario: Le passage de travail a NC termine seulement le travail vise
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T10:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT4H"
    And les elements de la synthese sont
      | id     | duree | dureeNonConformite |
      | carter | PT4H  | PT0S               |

  Scenario: Une fin a dix-sept heures recue le lendemain remplace la fin automatique
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT9H"

  Scenario: Une fin pointee a vingt-trois heures conserve la borne automatique
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"

  Scenario: Une fin puis une NC pointees apres echeance ouvrent la NC sans prolonger le travail
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"
    And les elements de la synthese sont
      | id     | duree | dureeNonConformite |
      | carter | PT13H | PT0S               |

  Scenario: Une fin regularisee apres echeance prolonge reellement le travail
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | acte           |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | POINTAGE       |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z | REGULARISATION |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT15H"

  Scenario: Une fin et une NC recues le lendemain conservent leurs heures metier et la propre echeance de la NC
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z | 2026-05-12T08:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T10:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT17H"
    And le jour "2026-05-11" a une duree operationnelle de "PT16H"
    And le jour "2026-05-12" a une duree operationnelle de "PT1H"
    And les elements de la synthese sont
      | id     | duree | dureeNonConformite |
      | carter | PT17H | PT13H              |

  Scenario: Une fin exactement a echeance recue le lendemain est une fin reelle
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T19:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"

  Scenario: Une cloture apres echeance conserve la fin automatique
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And pour la synthese, l'element "carter" est cloture a "2026-05-11T21:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"

  Scenario: Une fin avant cloture recue apres cloture remplace sa borne
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And pour la synthese, l'element "carter" est cloture a "2026-05-11T18:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type | intention | cible | operateur | poste  | survenue             | reception            |
      | F     | FIN  | FIN       | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT9H"

  Scenario: Une regularisation tres longue recouvre la semaine sans borne basse de debut
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-01T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | acte           |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-01T06:00:00Z | POINTAGE       |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z | REGULARISATION |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT10H"

  Scenario: Deux elements de huit a neuf heures se cumulent sans presence
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And pour la synthese, l'element "bride" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T07:00:00Z |
    And la synthese des heures recoit sur l'element "bride" les pointages
      | alias | type  | intention | cible | operateur | poste | survenue             |
      | B     | DEBUT | OUVERTURE |       | martin    | Tour  | 2026-05-11T06:00:00Z |
      | G     | FIN   | FIN       | B     | martin    | Tour  | 2026-05-11T07:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree operationnelle de "PT2H"
    And la duree operationnelle totale de la semaine est "PT2H"
    And les elements de la synthese sont
      | id     | duree |
      | carter | PT1H  |
      | bride  | PT1H  |
    And la synthese ne porte aucun champ de presence ni temps presume

  Scenario: Travail puis NC puis travail conservent un seul total et la part de NC
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T08:00:00Z |
      | FN    | FIN            | FIN       | N     | martin    | DMU 50 | 2026-05-11T09:00:00Z |
      | B     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T09:00:00Z |
      | F     | FIN            | FIN       | B     | martin    | DMU 50 | 2026-05-11T12:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT6H"
    And les elements de la synthese sont
      | id     | duree | dureeNonConformite |
      | carter | PT6H  | PT1H               |

  Scenario: Un jour entier traverse sans pointage propre compte vingt-quatre heures terminees
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | survenue             | acte           |
      | A     | DEBUT | OUVERTURE |       | dupont    | 2026-05-10T18:00:00Z | POINTAGE       |
      | F     | FIN   | FIN       | A     | dupont    | 2026-05-13T06:00:00Z | REGULARISATION |
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then le jour "2026-05-12" a une duree operationnelle de "PT24H"
    And les pointages du "2026-05-12" sont
      | type | dateDeSurvenue |
    And la duree operationnelle totale de la semaine est "PT56H"
    And l'element "carter" de la synthese porte les postes
      | poste | nature |

  Scenario: Une activite du dimanche en cours conserve son element sur lundi puis compte trois heures terminees
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-10T20:00:00Z |
    And il est "2026-05-10T23:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT0S"
    And les elements de la synthese sont
      | id     | duree |
      | carter | PT0S  |
    And les pointages du "2026-05-11" sont
      | type | dateDeSurvenue |
    Given la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type | intention | cible | operateur | poste  | survenue             |
      | F     | FIN  | FIN       | A     | martin    | DMU 50 | 2026-05-11T01:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT3H"

  Scenario: La synthese et son journal restent dans leur entreprise et sont lisibles par USER
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | 2026-05-11T08:00:00Z |
    Given I am logged in as "user" with role "USER" for tenant "katilys"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 404
    Given I am logged in as "user" with role "USER" for tenant "impeccmold"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la duree operationnelle totale de la semaine est "PT2H"
    And le journal du "2026-05-11" est
      | type  | dateDeSurvenue       | element | poste |
      | DEBUT | 2026-05-11T06:00:00Z | carter  |       |
      | FIN   | 2026-05-11T08:00:00Z | carter  |       |
