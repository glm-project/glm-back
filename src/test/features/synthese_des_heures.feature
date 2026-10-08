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
    And "martin" enregistre "NON_CONFORMITE" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T09:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     | categorie | type    | duree   | dureeNonConformite |
      | carter | MOULE     | PRODUIT | PT3H55M | PT1H               |

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

  Scenario: Un pointage annule disparait du journal
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And pour la synthese, le dernier pointage sur l'element "carter" est annule a "2026-05-11T06:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les pointages du "2026-05-11" sont
      | type | dateDeSurvenue |
    And les elements de la synthese sont
      | id |

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
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
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

  Scenario: Une fin pointee a vingt-trois heures conserve la borne automatique sans conflit
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    Then le suivi de la synthese des heures de "carter" ne porte aucun conflit
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"
    And la synthese porte les conflits
      | element | poste | activites | pointages |
    And la synthese restitue les identites et cibles du journal du "2026-05-11"
      | alias | intention | cible |
      | A     | OUVERTURE |       |
      | F     | FIN       | A     |

  Scenario: Une transition pointee apres echeance ouvre une NC sans prolonger le travail
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    Then le suivi de la synthese des heures de "carter" ne porte aucun conflit
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

  Scenario: Une transition recue le lendemain conserve ses heures metier et sa propre echeance
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z | 2026-05-12T08:00:00Z |
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

  Scenario: La correction du debut garde son identite et rend le travail de nouveau en cours
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And la synthese des heures corrige le pointage "A" sur "carter" a "2026-05-11T20:00:00Z" vers "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT0S"

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

  Scenario: Annuler la fin fait reapparaitre la borne automatique
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z |
    And pour la synthese, le dernier pointage sur l'element "carter" est annule a "2026-05-12T08:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT13H"

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
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z |
      | B     | DEBUT          | TRANSITION | N     | martin    | DMU 50 | 2026-05-11T09:00:00Z |
      | F     | FIN            | FIN        | B     | martin    | DMU 50 | 2026-05-11T12:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT6H"
    And les elements de la synthese sont
      | id     | duree | dureeNonConformite |
      | carter | PT6H  | PT1H               |

  Scenario: Une transition regularisee apres echeance peut etablir quinze heures de travail
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             | acte           |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | POINTAGE       |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z | REGULARISATION |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la duree operationnelle totale de la semaine est "PT15H"
    And les elements de la synthese sont
      | id     | duree | dureeNonConformite |
      | carter | PT15H | PT0S               |

  Scenario: Un pointage actif sans activite conserve son element et son poste au journal
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T07:00:00Z |
    And pour la synthese, le pointage "A" sur "carter" est annule a "2026-05-11T08:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le journal du "2026-05-11" est
      | type | dateDeSurvenue       | element | poste  |
      | FIN  | 2026-05-11T07:00:00Z | carter  | DMU 50 |
    And les elements de la synthese sont
      | id     | duree |
      | carter | PT0S  |
    And l'element "carter" de la synthese porte les postes
      | poste  | nature   |
      | DMU 50 | Fraisage |

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

  Scenario Outline: Le conflit rend les totaux dependants incomplets sans somme partielle quel que soit la reception
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias       | type            | intention            | cible | operateur | poste  | survenue             | reception            |
      | A           | DEBUT           | OUVERTURE            |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | <second>    | <typeSecond>    | <intentionSecond>    | A     | martin    | DMU 50 | <survenueSecond>     | 2026-05-11T15:00:00Z |
      | <troisieme> | <typeTroisieme> | <intentionTroisieme> | A     | martin    | DMU 50 | <survenueTroisieme>  | 2026-05-11T15:01:00Z |
      | C           | DEBUT           | OUVERTURE            |       | martin    | Tour   | 2026-05-11T16:00:00Z | 2026-05-11T16:00:00Z |
      | D           | FIN             | FIN                  | C     | martin    | Tour   | 2026-05-11T18:00:00Z | 2026-05-11T18:00:00Z |
    And pour la synthese, l'element "bride" est engage en atelier a "2026-05-13T04:00:00Z"
    And la synthese des heures recoit sur l'element "bride" les pointages
      | alias | type  | intention | cible | operateur | poste | survenue             |
      | E     | DEBUT | OUVERTURE |       | martin    | Tour  | 2026-05-13T06:00:00Z |
      | G     | FIN   | FIN       | E     | martin    | Tour  | 2026-05-13T08:00:00Z |
    And il est "2026-05-13T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-11" de la synthese est incomplet sans chiffre
    And le jour "2026-05-12" de la synthese est incomplet sans chiffre
    And la synthese laisse incomplet sans chiffre le total "$.dureeOperationnelleTotale"
    And l'element "carter" de la synthese est incomplet sans chiffre pour "duree"
    And l'element "carter" de la synthese est incomplet sans chiffre pour "dureeNonConformite"
    And le jour "2026-05-13" a une duree operationnelle de "PT2H"
    And l'element "bride" de la synthese porte les totaux
      | mesure             | complete | valeur |
      | duree              | true     | PT2H   |
      | dureeNonConformite | true     | PT0S   |
    And la synthese restitue les identites et cibles du journal du "2026-05-11"
      | alias | intention  | cible |
      | A     | OUVERTURE  |       |
      | N     | TRANSITION | A     |
      | F     | FIN        | A     |
      | C     | OUVERTURE  |       |
      | D     | FIN        | C     |
    And la synthese porte les conflits
      | element | poste  | activites | pointages |
      | carter  | DMU 50 | A,N       | A,N,F     |

    Examples:
      | second | typeSecond     | intentionSecond | survenueSecond       | troisieme | typeTroisieme  | intentionTroisieme | survenueTroisieme    |
      | N      | NON_CONFORMITE | TRANSITION      | 2026-05-11T10:00:00Z | F         | FIN            | FIN                | 2026-05-11T15:00:00Z |
      | F      | FIN            | FIN             | 2026-05-11T15:00:00Z | N         | NON_CONFORMITE | TRANSITION         | 2026-05-11T10:00:00Z |

  Scenario: Un conflit de travail laisse la part de NC certaine
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z |
      | G     | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T09:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | Tour   | 2026-05-11T10:00:00Z |
      | H     | FIN            | FIN       | N     | martin    | Tour   | 2026-05-11T12:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then l'element "carter" de la synthese porte les totaux
      | mesure             | complete | valeur |
      | duree              | false    |        |
      | dureeNonConformite | true     | PT2H   |
    And la synthese laisse incomplet sans chiffre le total "$.dureeOperationnelleTotale"
    And la synthese porte les conflits
      | element | poste  | activites | pointages |
      | carter  | DMU 50 | A         | A,F,G     |

  Scenario: Un conflit sans activite ni poste reste visible et les totaux restent complets
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | 2026-05-11T08:00:00Z |
    And pour la synthese, le pointage "A" sur "carter" est annule a "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la synthese porte les conflits
      | element | poste | activites | pointages |
      | carter  |       |           | F         |
    And la duree operationnelle totale de la semaine est "PT0S"
    And le jour "2026-05-11" a une duree operationnelle de "PT0S"
    And la synthese restitue les identites et cibles du journal du "2026-05-11"
      | alias | intention | cible |
      | F     | FIN       | A     |
    When je consulte la synthese des heures de "martin" pour la semaine 21 de 2026
    Then la synthese porte les conflits
      | element | poste | activites | pointages |
    And la duree operationnelle totale de la semaine est "PT0S"

  Scenario: Une plage possible de synthese depasse la semaine et se borne a evaluation
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-10T18:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | survenue             | reception            | acte           |
      | A     | DEBUT          | OUVERTURE  |       | martin    | 2026-05-10T20:00:00Z | 2026-05-10T20:00:00Z | POINTAGE       |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | 2026-05-10T21:00:00Z | 2026-05-10T21:00:00Z | POINTAGE       |
      | F     | FIN            | FIN        | A     | martin    | 2026-05-19T10:00:00Z | 2026-05-20T12:00:00Z | REGULARISATION |
    And pour la synthese, l'element "carter" est cloture a "2026-05-20T13:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 21 de 2026 avec evaluation "2026-05-18T23:00:00Z"
    Then le jour "2026-05-18" de la synthese est incomplet sans chiffre
    And le jour "2026-05-19" de la synthese est incomplet sans chiffre
    And le jour "2026-05-20" a une duree operationnelle de "PT0S"
    And la synthese porte les conflits
      | element | poste | activites | pointages |
      | carter  |       | A,N       | A,N,F     |
    When je consulte la synthese des heures de "martin" pour la semaine 21 de 2026 avec evaluation "2026-05-20T13:00:00Z"
    Then le jour "2026-05-19" de la synthese est incomplet sans chiffre
    And le jour "2026-05-20" a une duree operationnelle de "PT0S"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T20:00:00Z"
    Then le jour "2026-05-11" de la synthese est incomplet sans chiffre
    And le jour "2026-05-12" a une duree operationnelle de "PT0S"
    And la synthese porte les conflits
      | element | poste | activites | pointages |
      | carter  |       | A,N       | A,N,F     |

  Scenario: Annuler la transition erronee resout le conflit et recalcule les totaux
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
      | F     | FIN            | FIN        | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la synthese laisse incomplet sans chiffre le total "$.dureeOperationnelleTotale"
    Given pour la synthese, le pointage "N" sur "carter" est annule a "2026-05-11T18:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la synthese porte les conflits
      | element | poste | activites | pointages |
    And la duree operationnelle totale de la semaine est "PT9H"
    And l'element "carter" de la synthese porte les totaux
      | mesure             | complete | valeur |
      | duree              | true     | PT9H   |
      | dureeNonConformite | true     | PT0S   |
    And la synthese restitue les identites et cibles du journal du "2026-05-11"
      | alias | intention | cible |
      | A     | OUVERTURE |       |
      | F     | FIN       | A     |

  Scenario: Corriger le debut et la cible resout le conflit avec l'identite originale
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
      | F     | FIN            | FIN        | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la synthese laisse incomplet sans chiffre le total "$.dureeOperationnelleTotale"
    Given la synthese des heures corrige le pointage "A" sur "carter" a "2026-05-11T17:00:00Z" vers "2026-05-11T07:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la synthese porte les conflits
      | element | poste  | activites | pointages     |
      | carter  | DMU 50 | A,N       | A-corrige,N,F |
    Given la synthese des heures corrige la cible du pointage "F" sur "carter" vers "N" a "2026-05-11T18:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la synthese porte les conflits
      | element | poste | activites | pointages |
    And la duree operationnelle totale de la semaine est "PT8H"
    And l'element "carter" de la synthese porte les totaux
      | mesure             | complete | valeur |
      | duree              | true     | PT8H   |
      | dureeNonConformite | true     | PT5H   |
    And la synthese restitue les identites et cibles du journal du "2026-05-11"
      | alias     | intention  | cible |
      | A-corrige | OUVERTURE  |       |
      | N         | TRANSITION | A     |
      | F-corrige | FIN        | N     |

  Scenario: Avant le debut possible le journal garde le conflit mais les durees restent completes
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la synthese des heures recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
      | F     | FIN            | FIN        | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z |
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T05:00:00Z"
    Then la duree operationnelle totale de la semaine est "PT0S"
    And la synthese porte les conflits
      | element | poste  | activites | pointages |
      | carter  | DMU 50 | A,N       | A,N,F     |
