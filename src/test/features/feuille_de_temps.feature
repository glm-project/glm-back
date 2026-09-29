Feature: Feuille de temps hebdomadaire d'un operateur

  # L'atelier ne connait ni fuseau horaire ni jour calendaire : une journee de travail y va d'une arrivee a un
  # depart, et rien n'y dit a quel jour appartient une heure. La feuille de temps est le premier contexte a ramener
  # ces instants au calendrier de l'entreprise, semaine par semaine et jour par jour.
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
    And la feuille de temps ne porte aucune presence

  Scenario: Une journee se lit dans le jour qui la porte, pause de midi comprise
    Given "dupont" est arrive a "2026-05-11T06:00:00Z"
    And "dupont" a pointe "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    # La pause de midi se pointe sur les ordres, jamais sur la presence : la journee se lit en une seule fenetre, de
    # l'arrivee au depart.
    And la presence du "2026-05-11" est
      | debut                | fin                  |
      | 2026-05-11T06:00:00Z | 2026-05-11T15:00:00Z |
    And la presence du "2026-05-12" est vide

  Scenario: Une equipe de nuit compte sur les deux jours qu'elle traverse
    Given "dupont" est arrive a "2026-05-13T20:00:00Z"
    And "dupont" a pointe "DEPART" a "2026-05-14T00:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    # Minuit a Paris, c'est 22:00Z : c'est la que la venue bascule d'un jour a l'autre.
    And la presence du "2026-05-13" est
      | debut                | fin                  |
      | 2026-05-13T20:00:00Z | 2026-05-13T22:00:00Z |
    And la presence du "2026-05-14" est
      | debut                | fin                  |
      | 2026-05-13T22:00:00Z | 2026-05-14T00:00:00Z |

  Scenario: Une journee sans depart reste ouverte, sur son seul jour
    Given "dupont" est arrive a "2026-05-15T06:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la presence du "2026-05-15" commence a "2026-05-15T06:00:00Z" et n'est pas terminee
    And la presence du "2026-05-16" est vide

  Scenario: Une journee abandonnee signale sa plage presumee
    # E2 de la strategie « bornes de fin de journee », lot 5 : lu mardi, lundi s'arrete a son dernier fait connu, un
    # ordre demarre a 16 h. Sans depart, la journee n'a qu'une plage, presumee en entier jusqu'a la regularisation du
    # depart.
    Given "dupont" est arrive a "2026-05-11T05:00:00Z"
    And "dupont" a demarre un ordre de fabrication a "2026-05-11T14:00:00Z"
    And il est "2026-05-12T08:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la presence du "2026-05-11" est
      | debut                | fin                  | presumee |
      | 2026-05-11T05:00:00Z | 2026-05-11T14:00:00Z | true     |
    Given le depart de "dupont" est regularise a "2026-05-11T15:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la presence du "2026-05-11" est
      | debut                | fin                  | presumee |
      | 2026-05-11T05:00:00Z | 2026-05-11T15:00:00Z | false    |

  Scenario: Une journee fermee de plus de 24 h s'arrete a sa fin presumee
    # Issue #59 : lundi, Dupont ne pointe pas son depart ; le gestionnaire le saisit mercredi sur la meme journee. Plus
    # de 24 h ne se vivent pas d'une traite : lundi s'arrete a son dernier fait connu, un ordre demarre a 16 h, et
    # mardi comme mercredi restent vides au lieu de compter 24 h.
    Given "dupont" est arrive a "2026-05-11T05:00:00Z"
    And "dupont" a demarre un ordre de fabrication a "2026-05-11T14:00:00Z"
    And il est "2026-05-13T10:00:00Z"
    And le depart de "dupont" est regularise a "2026-05-13T08:00:00Z"
    And la reponse a le statut http 201
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And la presence du "2026-05-11" est
      | debut                | fin                  | presumee |
      | 2026-05-11T05:00:00Z | 2026-05-11T14:00:00Z | true     |
    And la presence du "2026-05-12" est vide
    And la presence du "2026-05-13" est vide

  Scenario: Un poste de nuit du dimanche au lundi se lit sur deux semaines
    # E7 : minuit a Paris, 22:00Z, coupe la venue entre la semaine 19 et la semaine 20.
    Given "dupont" est arrive a "2026-05-10T18:00:00Z"
    And "dupont" a pointe "DEPART" a "2026-05-11T06:00:00Z"
    When je consulte la feuille de temps de "dupont" pour la semaine 19 de 2026
    Then la presence du "2026-05-10" est
      | debut                | fin                  |
      | 2026-05-10T18:00:00Z | 2026-05-10T22:00:00Z |
    When je consulte la feuille de temps de "dupont" pour la semaine 20 de 2026
    Then la presence du "2026-05-11" est
      | debut                | fin                  |
      | 2026-05-10T22:00:00Z | 2026-05-11T06:00:00Z |

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

  # Le travail par element est coupe a minuit, sans reduction a la presence.
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
    And "martin" pointe "NON_CONFORMITE" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" pointe "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T09:00:00Z"
    And "martin" a pointe "DEPART" a "2026-05-11T15:00:00Z"
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
    And "martin" a pointe "DEPART" a "2026-05-11T15:00:00Z"
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

  Scenario: Un pointage annule disparait de la feuille
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And le dernier pointage sur l'element "carter" est annule a "2026-05-11T06:00:00Z"
    And "martin" a pointe "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then le "2026-05-11" ne porte aucune activite

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
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z |
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

  Scenario: Une fin pointee a vingt-trois heures conserve la borne automatique sans conflit
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    Then le suivi de la feuille de temps de "carter" ne porte aucun conflit
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat                     |
      | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT |

  Scenario: Une transition pointee apres echeance ouvre une NC sans prolonger le travail
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T21:00:00Z |
    Then le suivi de la feuille de temps de "carter" ne porte aucun conflit
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

  Scenario: Une transition recue le lendemain conserve ses heures metier et sa propre echeance
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type           | intention  | cible | operateur | poste  | survenue             | reception            |
      | A     | DEBUT          | OUVERTURE  |       | martin    | DMU 50 | 2026-05-11T06:00:00Z | 2026-05-11T06:00:00Z |
      | N     | NON_CONFORMITE | TRANSITION | A     | martin    | DMU 50 | 2026-05-11T10:00:00Z | 2026-05-12T08:00:00Z |
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

  Scenario: La correction du debut garde son identite et rend le travail de nouveau en cours
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE | martin    | DMU 50 | 2026-05-11T06:00:00Z |
    And la feuille de temps corrige le pointage "A" sur "carter" a "2026-05-11T20:00:00Z" vers "2026-05-11T10:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin | idActivite | etat     | debutActivite        | finActivite |
      | 2026-05-11T10:00:00Z |     | A          | EN_COURS | 2026-05-11T10:00:00Z |             |

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

  Scenario: Annuler la fin fait reapparaitre la borne automatique
    Given l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And la feuille de temps recoit sur l'element "carter" les pointages
      | alias | type  | intention | cible | operateur | poste  | survenue             |
      | A     | DEBUT | OUVERTURE |       | martin    | DMU 50 | 2026-05-11T06:00:00Z |
      | F     | FIN   | FIN       | A     | martin    | DMU 50 | 2026-05-11T15:00:00Z |
    And le dernier pointage sur l'element "carter" est annule a "2026-05-12T08:00:00Z"
    When je consulte la feuille de temps de "martin" pour la semaine 20 de 2026
    Then les activites du "2026-05-11" sont
      | debut                | fin                  | idActivite | etat                     |
      | 2026-05-11T06:00:00Z | 2026-05-11T19:00:00Z | A          | TERMINEE_AUTOMATIQUEMENT |

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
