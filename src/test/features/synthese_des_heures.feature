Feature: Synthese des heures hebdomadaire d'un operateur

  # Meme couture que la feuille de temps : l'atelier ne connait ni fuseau horaire ni jour calendaire, donc c'est ici
  # que les instants du journal de presence sont ramenes au calendrier de l'entreprise, semaine par semaine et jour
  # par jour. La difference porte sur ce que ce contexte ajoute a la feuille de temps : le journal brut des
  # pointages plutot que des fenetres repliees, et une duree travaillee calculee par jour et pour la semaine.
  #
  # Un pointage qui casserait l'automate de presence n'apparaitrait pas dans le releve, sans jamais empecher sa
  # lecture — mais ce cas n'a pas de scenario ici : atelier valide tout le journal a chaque ecriture, y compris une
  # annulation, et refuse deja celle qui laisserait un pointage orphelin. Cette resilience n'est donc atteignable par
  # aucune sequence d'appels API ; elle reste verifiee au niveau domaine (SynthesesDesHeuresServiceTest,
  # JourneeDeTravailTest), en pure defense en profondeur.
  #
  # Les heures des scenarios sont en UTC, l'entreprise lit ses jours a Paris : en mai, 8h locales font 06:00Z.
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
    And la duree totale de la semaine est "PT0S"

  Scenario: Une journee porte son journal brut et sa duree, pause de midi comprise
    Given "dupont" pointe son arrivee a "2026-05-11T06:00:00Z"
    And "dupont" enregistre le pointage "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And les pointages du "2026-05-11" sont
      | type    | dateDeSurvenue       |
      | ARRIVEE | 2026-05-11T06:00:00Z |
      | DEPART  | 2026-05-11T15:00:00Z |
    # La pause de midi se pointe sur les ordres, jamais sur la presence : la duree court de l'arrivee au depart.
    And le jour "2026-05-11" a une duree de "PT9H"

  Scenario: Une equipe de nuit repartit sa duree sur les deux jours qu'elle traverse
    Given "dupont" pointe son arrivee a "2026-05-13T20:00:00Z"
    And "dupont" enregistre le pointage "DEPART" a "2026-05-14T00:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    # Minuit a Paris, c'est 22:00Z : c'est la que la duree bascule d'un jour a l'autre, comme les pointages restent
    # sur leur jour propre.
    And le jour "2026-05-13" a une duree de "PT2H"
    And le jour "2026-05-14" a une duree de "PT2H"

  Scenario: Une journee sans depart ne compte aucune duree, sans faire disparaitre le pointage
    Given "dupont" pointe son arrivee a "2026-05-15T06:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And les pointages du "2026-05-15" sont
      | type    | dateDeSurvenue       |
      | ARRIVEE | 2026-05-15T06:00:00Z |
    And le jour "2026-05-15" a une duree de "PT0S"

  Scenario: Une journee abandonnee separe ses heures pointees de ses heures presumees
    # E2 de la strategie « bornes de fin de journee », lot 5 : lundi, Dupont part sans pointer son depart. Lu mardi,
    # sa journee s'arrete a son dernier fait connu, un ordre demarre a 16 h. Sans depart, elle n'a qu'une fenetre :
    # ses 9 h sont presumees en entier.
    Given "dupont" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "dupont" demarre un ordre de fabrication a "2026-05-11T14:00:00Z"
    And il est "2026-05-12T08:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And le jour "2026-05-11" a une duree de "PT0S"
    And le jour "2026-05-11" a une duree presumee de "PT9H"
    And la duree totale de la semaine est "PT0S"
    And la duree presumee totale de la semaine est "PT9H"
    # Le gestionnaire regularise le depart a 17 h : le pointe remplace le presume.
    Given le gestionnaire regularise le depart de "dupont" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree de "PT10H"
    And le jour "2026-05-11" a une duree presumee de "PT0S"
    And la duree presumee totale de la semaine est "PT0S"

  Scenario: Une journee fermee de plus de 24 h ne compte que jusqu'a sa fin presumee
    # Issue #59 : lundi, Dupont ne pointe pas son depart ; le gestionnaire le saisit mercredi sur la meme journee. Plus
    # de 24 h ne se vivent pas d'une traite : 9 h presumees lundi, rien mardi ni mercredi.
    Given "dupont" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "dupont" demarre un ordre de fabrication a "2026-05-11T14:00:00Z"
    And il est "2026-05-13T10:00:00Z"
    And le gestionnaire regularise le depart de "dupont" a "2026-05-13T08:00:00Z"
    And la reponse a le statut http 201
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And le jour "2026-05-11" a une duree de "PT0S"
    And le jour "2026-05-11" a une duree presumee de "PT9H"
    And le jour "2026-05-12" a une duree de "PT0S"
    And le jour "2026-05-13" a une duree de "PT0S"
    And la duree totale de la semaine est "PT0S"
    And la duree presumee totale de la semaine est "PT9H"

  Scenario: Un poste de nuit oublie ne presume que ses cinq premieres minutes
    # E3 : arrive a 20 h, un ordre demarre a 20 h 05, rien d'autre. La fin presumee n'invente aucune heure.
    Given "dupont" pointe son arrivee a "2026-05-11T18:00:00Z"
    And "dupont" demarre un ordre de fabrication a "2026-05-11T18:05:00Z"
    And il est "2026-05-12T18:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree de "PT0S"
    And le jour "2026-05-11" a une duree presumee de "PT5M"

  Scenario: Un poste de nuit du dimanche au lundi se partage entre deux semaines
    # E7 : minuit a Paris, 22:00Z, coupe la venue ; le dimanche releve de la semaine 19, le lundi de la semaine 20.
    Given "dupont" pointe son arrivee a "2026-05-10T18:00:00Z"
    And "dupont" enregistre le pointage "DEPART" a "2026-05-11T06:00:00Z"
    When je consulte la synthese des heures de "dupont" pour la semaine 19 de 2026
    Then le jour "2026-05-10" a une duree de "PT4H"
    And la duree totale de la semaine est "PT4H"
    When je consulte la synthese des heures de "dupont" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree de "PT8H"
    And la duree totale de la semaine est "PT8H"

  # Le temps operationnel : les pointages de l'operateur sur ses elements, rejoues poste par poste avec l'automate
  # d'atelier, reduits a la presence de la journee ou chacun a commence, coupes a minuit, puis additionnes. Les
  # durees se cumulent par element : une heure passee sur deux elements compte deux fois.
  Scenario: Une fin coupe le travail, un debut le relance, et la coupure ne compte pas
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T10:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T11:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then la reponse a le statut http 200
    And le jour "2026-05-11" a une duree operationnelle de "PT8H55M"
    And la duree operationnelle totale de la semaine est "PT8H55M"

  Scenario: Une non conformite compte dans l'element, a part
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "NON_CONFORMITE" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:00:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T09:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     | type    | duree   | dureeNonConformite | dureePresumee |
      | carter | PRODUIT | PT3H55M | PT1H               | PT0S          |

  Scenario: Un depart arrete le travail que personne n'a arrete
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree operationnelle de "PT4H55M"

  Scenario: Deux elements travailles en meme temps comptent chacun, au-dela de la presence
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And pour la synthese, l'element "bride" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "DEBUT" sur l'element "bride" au poste "DMU 50" a "2026-05-11T06:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree de "PT5H"
    And le jour "2026-05-11" a une duree operationnelle de "PT8H55M"
    And les elements de la synthese sont
      | id     | duree   |
      | carter | PT4H55M |
      | bride  | PT4H    |

  Scenario: Un element travaille sur deux postes porte deux couples de poste et de nature
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "Tour" a "2026-05-11T06:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then l'element "carter" de la synthese porte les postes
      | poste  | nature   |
      | DMU 50 | Fraisage |
      | Tour   | Tournage |

  Scenario: Le travail d'un poste de nuit se repartit sur les deux jours
    # Minuit a Paris, c'est 22:00Z.
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-13T19:00:00Z"
    And "martin" pointe son arrivee a "2026-05-13T20:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-13T20:05:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-14T00:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-13" a une duree operationnelle de "PT1H55M"
    And le jour "2026-05-14" a une duree operationnelle de "PT2H"

  Scenario: Le travail d'une journee abandonnee est presume en entier
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T14:00:00Z"
    And il est "2026-05-12T08:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree operationnelle de "PT0S"
    And le jour "2026-05-11" a une duree operationnelle presumee de "PT8H55M"
    And la duree operationnelle presumee totale de la semaine est "PT8H55M"

  Scenario: Un travail en cours ne compte pas encore
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And il est "2026-05-11T09:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le jour "2026-05-11" a une duree operationnelle de "PT0S"
    And les elements de la synthese sont
      | id     | duree |
      | carter | PT0S  |

  Scenario: Un element reengage apres cloture reste un seul element
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And pour la synthese, l'element "carter" est cloture a "2026-05-11T07:00:00Z"
    And pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T08:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T08:05:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T12:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     | duree   |
      | carter | PT5H50M |

  Scenario: Un pointage annule disparait du journal
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And pour la synthese, le dernier pointage sur l'element "carter" est annule a "2026-05-11T06:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les pointages du "2026-05-11" sont
      | type    | dateDeSurvenue       |
      | ARRIVEE | 2026-05-11T05:00:00Z |
      | DEPART  | 2026-05-11T15:00:00Z |
    And les elements de la synthese sont
      | id |

  Scenario: Le journal du jour mele presence et elements, l'arrivee et le depart aux bornes d'un meme instant
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:00:00Z"
    And "martin" enregistre "FIN" sur l'element "carter" au poste "DMU 50" a "2026-05-11T15:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T15:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then le journal du "2026-05-11" est
      | type    | dateDeSurvenue       | element | poste  |
      | ARRIVEE | 2026-05-11T05:00:00Z |         |        |
      | DEBUT   | 2026-05-11T05:00:00Z | carter  | DMU 50 |
      | FIN     | 2026-05-11T15:00:00Z | carter  | DMU 50 |
      | DEPART  | 2026-05-11T15:00:00Z |         |        |

  Scenario: Le poste d'un pointage qui ne laisse aucun travail est nomme dans son element
    # Lu le lendemain, la journee s'arrete a son dernier fait, le debut lui-meme : son travail est reduit a un instant
    # et ne compte rien, mais le journal nomme le poste, que l'element doit porter.
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And il est "2026-05-12T08:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     | duree | dureePresumee |
      | carter | PT0S  | PT0S          |
    And l'element "carter" de la synthese porte les postes
      | poste  | nature   |
      | DMU 50 | Fraisage |

  Scenario: Les elements suivent leur premiere apparition dans la semaine
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And pour la synthese, l'element "bride" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "bride" au poste "Tour" a "2026-05-11T05:05:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T06:00:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T10:00:00Z"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then les elements de la synthese sont
      | id     |
      | bride  |
      | carter |

  Scenario: La reference et la description d'un element sont relues au referentiel
    Given pour la synthese, l'element "carter" est engage en atelier a "2026-05-11T04:00:00Z"
    And "martin" pointe son arrivee a "2026-05-11T05:00:00Z"
    And "martin" enregistre "DEBUT" sur l'element "carter" au poste "DMU 50" a "2026-05-11T05:05:00Z"
    And "martin" enregistre le pointage "DEPART" a "2026-05-11T10:00:00Z"
    And la fiche de l'element "carter" est revisee avec la reference "1015-B" et la description "Carter de pompe revise"
    When je consulte la synthese des heures de "martin" pour la semaine 20 de 2026
    Then l'element "carter" de la synthese porte sa fiche revisee

  Scenario: Une synthese ne se lit pas pour un operateur inconnu
    When je consulte la synthese des heures de l'operateur "11111111-2222-3333-4444-555555555555" pour la semaine 20 de 2026
    Then la reponse a le statut http 404

  Scenario: Une semaine hors bornes est refusee
    When je consulte la synthese des heures de "dupont" pour la semaine 54 de 2026
    Then la reponse a le statut http 400
