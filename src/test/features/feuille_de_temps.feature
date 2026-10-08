Feature: Feuille de temps hebdomadaire d'un operateur

  # La feuille ramene les activites interpretees par atelier au calendrier de l'entreprise.
  # Elle conserve les bornes entieres et les coupe aux minuits locaux et aux semaines ISO.
  #
  # Les heures des scenarios sont en UTC, l'entreprise lit ses jours a Paris : en mai, 8h locales font 06:00Z.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And la feuille de temps suit l'operateur "dupont"
    And la feuille de temps connait le poste "DMU 50" de nature "Fraisage"
    And la feuille de temps connait le poste "Tour" de nature "Tournage"
    And la feuille de temps suit l'operateur "martin" habilite sur
      | DMU 50 |
      | Tour   |
    And la feuille de temps connait l'element "carter"
    And la feuille de temps connait l'element "bride"

  Scenario: Une semaine sans pointage rend sept jours vides
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la feuille de temps porte les jours
      | 2026-05-11 |
      | 2026-05-12 |
      | 2026-05-13 |
      | 2026-05-14 |
      | 2026-05-15 |
      | 2026-05-16 |
      | 2026-05-17 |
    And la feuille de temps ne porte aucune activite
    And la feuille de temps ne porte aucun champ de presence

  Scenario: La feuille et la synthese utilisent l'instant choisi avant echeance malgre une reception apres echeance
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T08:00:00Z |
    And il est "2026-05-11T21:00:05Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T20:59:59Z"
    Then la reponse a le statut http 200
    And les activites du "2026-05-11" sont
      | idActivite | etat     | debutActivite        | finActivite | fin |
      | A          | EN_COURS | 2026-05-11T08:00:00Z |             |     |
    And la feuille de temps est evaluee a "2026-05-11T20:59:59Z"
    When je lis la synthese du releve avec l'instant rendu par la feuille
    Then la reponse a le statut http 200
    And la synthese du releve compte "PT0S" a l'instant "2026-05-11T20:59:59Z"

  Scenario: La feuille refuse le depassement minimal des deux minutes futures
    Given il est "2026-05-11T21:00:05Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T21:02:05.000000001Z"
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:feuille-de-temps:evaluation-future"
    And la feuille de temps refusee ne porte aucun rapport

  Scenario Outline: La feuille accepte un instant passe et la limite future incluse
    Given il est "2026-05-11T21:00:05Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation "<evaluation>"
    Then la reponse a le statut http 200
    And la feuille de temps est evaluee a "<echo>"
    And la feuille de temps ne porte aucune activite

    Examples:
      | evaluation                     | echo                           |
      | 2020-01-01T00:00:00Z           | 2020-01-01T00:00:00Z           |
      | 2026-05-11T21:02:05Z           | 2026-05-11T21:02:05Z           |
      | 2026-05-11T21:02:04.999999999Z | 2026-05-11T21:02:04.999999999Z |
      | 2026-05-11T23:02:05+02:00      | 2026-05-11T21:02:05Z           |

  Scenario: La feuille refuse un instant mal forme
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation "pas-un-instant"
    Then la reponse a le statut http 400
    And la feuille de temps refusee ne porte aucun rapport

  Scenario: La feuille refuse un instant vide explicitement fourni
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation ""
    Then la reponse a le statut http 400
    And la feuille de temps refusee ne porte aucun rapport

  Scenario: L'instant choisi exactement a echeance termine automatiquement la feuille
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T08:00:00Z |
    And il est "2026-05-11T21:00:05Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T21:00:00Z"
    Then la reponse a le statut http 200
    And la feuille de temps est evaluee a "2026-05-11T21:00:00Z"
    And les activites du "2026-05-11" sont
      | idActivite | etat                     | debutActivite        | finActivite          | fin                  |
      | A          | TERMINEE_AUTOMATIQUEMENT | 2026-05-11T08:00:00Z | 2026-05-11T21:00:00Z | 2026-05-11T21:00:00Z |
    When je lis la synthese du releve avec l'instant rendu par la feuille
    Then la reponse a le statut http 200
    And la synthese du releve compte "PT13H" a l'instant "2026-05-11T21:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then la feuille de temps est evaluee a "2026-05-11T21:00:05Z"
    And les activites du "2026-05-11" sont
      | idActivite | etat                     | finActivite          |
      | A          | TERMINEE_AUTOMATIQUEMENT | 2026-05-11T21:00:00Z |

  Scenario: La feuille interprete une fin connue apres l'instant choisi sans lecture historique
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | 2026-05-11T08:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | 2026-05-11T21:00:00Z |
    And il est "2026-05-11T21:00:05Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026 avec evaluation "2026-05-11T20:59:59Z"
    Then la reponse a le statut http 200
    And la feuille de temps est evaluee a "2026-05-11T20:59:59Z"
    And les activites du "2026-05-11" sont
      | idActivite | etat     | debutActivite        | finActivite          |
      | A          | TERMINEE | 2026-05-11T08:00:00Z | 2026-05-11T21:00:00Z |
    When je lis la synthese du releve avec l'instant rendu par la feuille
    Then la reponse a le statut http 200
    And la synthese du releve compte "PT13H" a l'instant "2026-05-11T20:59:59Z"

  Scenario: Un intervalle termine sans arrivee garde toutes ses bornes
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-11T06:00:00Z | 2026-05-11T08:00:00Z |
    And la feuille de temps ne porte aucun champ de presence

  Scenario: Un poste de nuit termine sans arrivee est coupe entre deux semaines
    Given l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-10T18:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 19 de 2026
    Then les activites du "2026-05-10" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-10T18:00:00Z | 2026-05-10T22:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-10T22:00:00Z | 2026-05-11T06:00:00Z |

  # Le travail par element est coupe aux minuits locaux.
  Scenario: Une fin coupe le travail et un debut le relance
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T11:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T15:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And les activites du "2026-05-11" sont
      | element | poste  | nature   | categorie | debut                | fin                  |
      | carter  | DMU 50 | Fraisage | TRAVAIL   | 2026-05-11T05:05:00Z | 2026-05-11T10:00:00Z |
      | carter  | DMU 50 | Fraisage | TRAVAIL   | 2026-05-11T11:00:00Z | 2026-05-11T15:00:00Z |

  Scenario: Une non conformite suit le travail
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" pointe "NON_CONFORMITE" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T09:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | categorie      | debut                | fin                  |
      | carter  | TRAVAIL        | 2026-05-11T05:05:00Z | 2026-05-11T08:00:00Z |
      | carter  | NON_CONFORMITE | 2026-05-11T08:00:00Z | 2026-05-11T09:00:00Z |

  Scenario: Deux elements travailles en meme temps donnent deux activites qui se chevauchent
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And l'element "bride" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" pointe "DEBUT" sur l'element "bride" au poste "DMU 50" a "2026-05-11T06:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" pointe "FIN" sur l'element "bride" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | poste  | debut                | fin                  |
      | carter  | DMU 50 | 2026-05-11T05:05:00Z | 2026-05-11T10:00:00Z |
      | bride   | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T10:00:00Z |

  Scenario: Un element travaille sur deux postes a la fois donne deux activites
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "Tour" a "2026-05-11T06:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "Tour" a "2026-05-11T10:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | poste  | nature   | debut                | fin                  |
      | carter  | DMU 50 | Fraisage | 2026-05-11T05:05:00Z | 2026-05-11T10:00:00Z |
      | carter  | Tour   | Tournage | 2026-05-11T06:00:00Z | 2026-05-11T10:00:00Z |

  Scenario: Le travail d'un poste de nuit se coupe a minuit
    # Minuit a Paris, c'est 22:00Z.
    Given l'element "carter" est engage en atelier a "2026-05-13T19:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-13T20:05:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-14T00:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-13" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-13T20:05:00Z | 2026-05-13T22:00:00Z |
    And les activites du "2026-05-14" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-13T22:00:00Z | 2026-05-14T00:00:00Z |

  Scenario: Le travail termine garde ses bornes meme sans depart
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" pointe "NON_CONFORMITE" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T14:00:00Z"
    And il est "2026-05-12T08:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | categorie      | debut                | fin                  |
      | TRAVAIL        | 2026-05-11T05:05:00Z | 2026-05-11T10:00:00Z |
      | NON_CONFORMITE | 2026-05-11T10:00:00Z | 2026-05-11T14:00:00Z |

  Scenario: Un travail en cours n'a pas encore de fin
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And il est "2026-05-11T09:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | debut                | fin |
      | carter  | 2026-05-11T05:05:00Z |     |
    And le "2026-05-12" ne porte aucune activite

  Scenario: La cloture du suivi arrete le travail que personne n'a arrete
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And l'element "carter" est cloture a "2026-05-11T09:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-11T05:05:00Z | 2026-05-11T09:00:00Z |

  Scenario: Un element reengage apres cloture reste le meme element
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And l'element "carter" est cloture a "2026-05-11T07:00:00Z"
    And l'element "carter" est engage en atelier a "2026-05-11T08:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:05:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T12:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | debut                | fin                  |
      | carter  | 2026-05-11T05:05:00Z | 2026-05-11T07:00:00Z |
      | carter  | 2026-05-11T08:05:00Z | 2026-05-11T12:00:00Z |

  Scenario: Une feuille de temps ne se lit pas pour un operateur inconnu
    When je consulte la feuille de temps de l'operateur "11111111-2222-3333-4444-555555555555" pour la semaine 20 de 2026
    Then la reponse a le statut http 404

  Scenario: Une semaine hors bornes est refusee
    When je consulte la feuille de temps de "dupont" pour la semaine 54 de 2026
    Then la reponse a le statut http 400

  Scenario: Une activite oubliee est terminee automatiquement a treize heures
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And il est "2026-05-11T19:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | debut                | fin                  | idActivite | etat                     | debutActivite        | finActivite          |
      | carter  | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z |

  Scenario: A vingt heures cinquante-neuf une activite sans poste est encore en cours
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | survenue             |
      | A     | DEBUT | OUVERTURE | dupont    | 2026-05-11T06:00:00Z |
    And il est "2026-05-11T18:59:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | element | poste | nature | debut                | fin | idActivite | etat     | debutActivite        | finActivite |
      | carter  |       |        | 2026-05-11T06:00:00Z |     | A          | EN_COURS | 2026-05-11T06:00:00Z |             |

  Scenario: La fin automatique du dimanche se retrouve sans pointage du lundi
    Given l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-10T20:00:00Z |
    And il est "2026-05-11T10:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat                     | debutActivite        | finActivite          |
      | 2026-05-10T22:00:00Z | 2026-05-11T09:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT | 2026-05-10T20:00:00Z | 2026-05-11T09:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 19 de 2026
    Then les activites du "2026-05-10" sont
      | debut                | fin                  | etat                     | debutActivite        | finActivite          |
      | 2026-05-10T20:00:00Z | 2026-05-10T22:00:00Z | TERMINEE_AUTOMATIQUEMENT | 2026-05-10T20:00:00Z | 2026-05-11T09:00:00Z |

  Scenario: Une fin du lundi partage une activite du dimanche entre deux semaines
    Given l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-10T20:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T01:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 19 de 2026
    Then les activites du "2026-05-10" sont
      | debut                | fin                  | idActivite | etat     | debutActivite        | finActivite          |
      | 2026-05-10T20:00:00Z | 2026-05-10T22:00:00Z | A          | TERMINEE | 2026-05-10T20:00:00Z | 2026-05-11T01:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     | debutActivite        | finActivite          |
      | 2026-05-10T22:00:00Z | 2026-05-11T01:00:00Z | A          | TERMINEE | 2026-05-10T20:00:00Z | 2026-05-11T01:00:00Z |

  Scenario: Une relance avant echeance termine sa precedente activite
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | B     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T12:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     |
      | 2026-05-11T06:00:00Z | 2026-05-11T12:00:00Z | A          | TERMINEE |
      | 2026-05-11T12:00:00Z |                      | B          | EN_COURS |

  Scenario: Une relance apres echeance conserve un trou entre les activites
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | B     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat                     |
      | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT |
      | 2026-05-11T21:00:00Z |                      | B          | EN_COURS                 |

  Scenario: Le passage de travail a NC termine seulement le travail vise
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T10:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | categorie      | debut                | fin                  | idActivite | etat     |
      | TRAVAIL        | 2026-05-11T06:00:00Z | 2026-05-11T10:00:00Z | A          | TERMINEE |
      | NON_CONFORMITE | 2026-05-11T10:00:00Z |                      | N          | EN_COURS |

  Scenario: Une fin a dix-sept heures recue le lendemain remplace la fin automatique
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     |
      | 2026-05-11T06:00:00Z | 2026-05-11T15:00:00Z | A          | TERMINEE |

  Scenario: Une fin pointee a vingt-trois heures conserve la borne automatique
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat                     |
      | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT |

  Scenario: Une fin puis une NC pointees apres echeance ouvrent la NC sans prolonger le travail
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | categorie      | debut                | fin                  | idActivite | etat                     |
      | TRAVAIL        | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT |
      | NON_CONFORMITE | 2026-05-11T21:00:00Z |                      | N          | EN_COURS                 |

  Scenario: Une fin regularisee apres echeance prolonge reellement le travail
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | acte           |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | POINTAGE       |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z | REGULARISATION |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     |
      | 2026-05-11T06:00:00Z | 2026-05-11T21:00:00Z | A          | TERMINEE |

  Scenario: Une fin et une NC recues le lendemain conservent leurs heures metier et la propre echeance de la NC
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type           | intention | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT          | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | FA    | FIN            | FIN       | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z | 2026-05-12T08:00:00Z |
      | N     | NON_CONFORMITE | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T10:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | categorie      | debut                | fin                  | idActivite | etat                     | finActivite          |
      | TRAVAIL        | 2026-05-11T06:00:00Z | 2026-05-11T10:00:00Z | A          | TERMINEE                 | 2026-05-11T10:00:00Z |
      | NON_CONFORMITE | 2026-05-11T10:00:00Z | 2026-05-11T22:00:00Z | N          | TERMINEE_AUTOMATIQUEMENT | 2026-05-11T23:00:00Z |
    And les activites du "2026-05-12" sont
      | categorie      | debut                | fin                  | idActivite | etat                     |
      | NON_CONFORMITE | 2026-05-11T22:00:00Z | 2026-05-11T23:00:00Z | N          | TERMINEE_AUTOMATIQUEMENT |

  Scenario: Une fin exactement a echeance recue le lendemain est une fin reelle
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T19:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     |
      | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE |

  Scenario: Une cloture apres echeance conserve la fin automatique
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And l'element "carter" est cloture a "2026-05-11T21:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat                     |
      | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT |

  Scenario: Une fin avant cloture recue apres cloture remplace sa borne
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And l'element "carter" est cloture a "2026-05-11T18:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type | intention | cible | operateur | poste  | survenue             | reception            |
      | F     | FIN  | FIN       | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z | 2026-05-12T08:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     |
      | 2026-05-11T06:00:00Z | 2026-05-11T15:00:00Z | A          | TERMINEE |

  Scenario: Une regularisation tres longue recouvre la semaine sans borne basse de debut
    Given l'element "carter" est engage en atelier a "2026-05-01T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             | acte           |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-01T06:00:00Z | POINTAGE       |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T08:00:00Z | REGULARISATION |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     | debutActivite        |
      | 2026-05-10T22:00:00Z | 2026-05-11T08:00:00Z | A          | TERMINEE | 2026-05-01T06:00:00Z |

  Scenario: Une activite du dimanche est indiquee sur lundi sans fin a minuit puis terminee a trois heures
    Given l'element "carter" est engage en atelier a "2026-05-10T17:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-10T20:00:00Z |
    And il est "2026-05-10T23:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 19 de 2026
    Then les activites du "2026-05-10" sont
      | debut                | fin | idActivite | etat     | debutActivite        | finActivite |
      | 2026-05-10T20:00:00Z |     | A          | EN_COURS | 2026-05-10T20:00:00Z |             |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin | idActivite | etat     | debutActivite        | finActivite |
      | 2026-05-10T22:00:00Z |     | A          | EN_COURS | 2026-05-10T20:00:00Z |             |
    And le "2026-05-12" ne porte aucune activite
    Given la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type | intention | cible | operateur | poste  | survenue             |
      | F     | FIN  | FIN       | A     | martin    | DMU 50 | 2026-05-11T01:00:00Z |
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat     | debutActivite        | finActivite          |
      | 2026-05-10T22:00:00Z | 2026-05-11T01:00:00Z | A          | TERMINEE | 2026-05-10T20:00:00Z | 2026-05-11T01:00:00Z |

  Scenario Outline: Une fin visee sur le travail deja transforme laisse les activites a resoudre hors de la feuille
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias       | type            | intention            | cible | operateur | survenue             | reception            |
      | A           | DEBUT           | OUVERTURE            |       | martin    | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | <second>    | <typeSecond>    | <intentionSecond>    | A     | martin    | <survenueSecond>     | 2026-05-11T15:00:00Z |
      | <troisieme> | <typeTroisieme> | <intentionTroisieme> | A     | martin    | <survenueTroisieme>  | 2026-05-11T15:01:00Z |
    And il est "2026-05-11T20:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then la feuille de temps ne porte aucune activite

    Examples:
      | second | typeSecond     | intentionSecond | survenueSecond       | troisieme | typeTroisieme  | intentionTroisieme | survenueTroisieme    |
      | N      | NON_CONFORMITE | TRANSITION      | 2026-05-11T10:00:00Z | F         | FIN            | FIN                | 2026-05-11T15:00:00Z |
      | F      | FIN            | FIN             | 2026-05-11T15:00:00Z | N         | NON_CONFORMITE | TRANSITION         | 2026-05-11T10:00:00Z |

  Scenario: Une feuille de temps et ses activites restent dans leur entreprise
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | 2026-05-11T08:00:00Z |
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 404
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And les activites du "2026-05-11" sont
      | idActivite | element | etat     | debut                | fin                  |
      | A          | carter  | TERMINEE | 2026-05-11T06:00:00Z | 2026-05-11T08:00:00Z |
