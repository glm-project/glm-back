Feature: Cout de revient d'un element de fabrication

  # L'atelier capture le temps sans jamais le chiffrer : il copie le cout horaire du poste et le taux horaire de
  # l'operateur sur chaque evenement du journal, mais aucune arithmetique ne les combine chez lui. C'est ici que le
  # calcul se fait, sur ces valeurs figees a la saisie — un tarif revise depuis ne reecrit pas le prix d'heures deja
  # passees.
  #
  # Deux regles gouvernent les montants, enoncees deux fois de suite par le client : le cout horaire de chaque machine
  # active court en entier, et le taux horaire de l'operateur se divise par le nombre de machines qu'il utilise.
  #
  # Ces scenarios ecrivent par l'API d'atelier et relisent sa projection interpretee par le cout, sans import Java.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And le rapport connait le poste "fraiseuse" de nature "fraisage" a "45.00" de l'heure
    And le rapport connait le poste "tour" de nature "tournage" a "60.00" de l'heure
    And le rapport connait l'operateur "dupont" a "20.00" de l'heure, habilite sur
      | fraiseuse |
      | tour      |

  Scenario: Un element jamais engage rend un rapport vide
    Given l'entreprise fabrique "OF 3001"
    When je consulte le cout de revient de "OF 3001" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le rapport ne porte aucune ligne
    And le cout total du rapport est "0.00" dont "0.00" de machine

  Scenario: Deux heures de fraisage valorisent la machine et l'operateur
    Given l'entreprise fabrique "OF 3002"
    And "OF 3002" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3002" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3002" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "OF 3002" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # 45 EUR de machine et 20 EUR d'operateur, pendant deux heures.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT2H    | PT0S          | 90.00   | 40.00       |

  Scenario: La pause de midi retire son creux du temps valorise
    Given l'entreprise fabrique "OF 3003"
    And "OF 3003" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3003" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3003" au poste "fraiseuse" a "2026-05-11T12:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3003" au poste "fraiseuse" a "2026-05-11T13:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3003" au poste "fraiseuse" a "2026-05-11T14:00:00Z"
    When je consulte le cout de revient de "OF 3003" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Quatre heures de 10 h a 14 h, trois de travail : le pupitre a pointe la pause de midi par une fin et un debut
    # dans le journal de l'element.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT3H    | PT0S          | 135.00  | 60.00       |

  Scenario: La reprise de non conformite se compte a part, datee
    Given l'entreprise fabrique "OF 3004"
    And "OF 3004" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "NON_CONFORMITE" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "OF 3004" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Une piece ratee se refait au meme tarif : le cout ne bouge pas, seul le partage du temps change.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT1H    | PT1H          | 90.00   | 40.00       |
    And le rapport porte la non conformite de "2026-05-11T10:00:00Z" a "2026-05-11T11:00:00Z"

  Scenario: Un double appui sur demarrer ne valorise rien de plus
    # Demarrer une activite deja en cours est ignore (DEJA_EN_COURS) : le second appui n'ouvre aucune activite.
    Given l'entreprise fabrique "OF 3010"
    And "OF 3010" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3010" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And pour le cout, "OF 3010" recoit les pointages
      | alias | type  | operateur | poste     | survenue             | reponse |
      | D     | DEBUT | dupont    | fraiseuse | 2026-05-11T09:00:03Z | ignore  |
    And "dupont" pointe "FIN" sur "OF 3010" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "OF 3010" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT2H    | PT0S          | 90.00   | 40.00       |

  Scenario: Un ordre relance le lendemain valorise les deux journees, sans la nuit
    # Les deux activites ont chacune leurs faits ouvrant et fermant : le rapport valorise deux heures lundi et une mardi.
    Given l'entreprise fabrique "OF 3011"
    And "OF 3011" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3011" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3011" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3011" au poste "fraiseuse" a "2026-05-12T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3011" au poste "fraiseuse" a "2026-05-12T10:00:00Z"
    When je consulte le cout de revient de "OF 3011" a "2026-05-12T18:00:00Z"
    Then la reponse a le statut http 200
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT3H    | PT0S          | 135.00  | 60.00       |

  Scenario: Deux machines menees de front divisent l'operateur, jamais les machines
    Given l'entreprise fabrique "OF 3005"
    And "OF 3005" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3005" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3005" au poste "tour" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3005" au poste "tour" a "2026-05-11T11:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3005" au poste "fraiseuse" a "2026-05-11T12:00:00Z"
    When je consulte le cout de revient de "OF 3005" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Seule l'heure de 10 h a 11 h est menee de front : la main d'oeuvre y est divisee par deux, de part et d'autre
    # elle reste entiere. Les deux machines, elles, courent leur temps entier — 3 h de fraiseuse et 1 h de tour.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT3H    | PT0S          | 135.00  | 50.00       |
      | tournage | PT1H    | PT0S          | 60.00   | 10.00       |
    # Le detail nomme le tour comme parallele, meme quand il porte sur le meme ordre.
    And le pointage de la ligne "fraisage" commence a "2026-05-11T09:00:00Z" se partage en
      | debut                | fin                  | diviseur | mainDOeuvre | paralleles   |
      | 2026-05-11T09:00:00Z | 2026-05-11T10:00:00Z | 1        | 20.00       |              |
      | 2026-05-11T10:00:00Z | 2026-05-11T11:00:00Z | 2        | 10.00       | tour@OF 3005 |
      | 2026-05-11T11:00:00Z | 2026-05-11T12:00:00Z | 1        | 20.00       |              |

  Scenario: Le detail justifie chaque pointage et le partage de son operateur
    Given l'entreprise fabrique "OF D1"
    And l'entreprise fabrique "OF D2"
    And "OF D1" est mis en atelier a "2026-05-11T07:00:00Z"
    And "OF D2" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF D1" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF D2" au poste "tour" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "FIN" sur "OF D2" au poste "tour" a "2026-05-11T11:00:00Z"
    And "dupont" pointe "FIN" sur "OF D1" au poste "fraiseuse" a "2026-05-11T12:00:00Z"
    When je consulte le cout de revient de "OF D1" a "2026-05-11T18:00:00Z"
    # La machine court ses trois heures ; l'operateur est divise par deux de 10 h a 11 h, ou il menait aussi le tour
    # de l'OF D2 : 20,00 + 10,00 + 20,00 EUR.
    Then la ligne "fraisage" detaille les pointages
      | operateur | poste     | debut                | fin                  | anomalies | machine | mainDOeuvre |
      | dupont    | fraiseuse | 2026-05-11T09:00:00Z | 2026-05-11T12:00:00Z |           | 135.00  | 50.00       |
    And le pointage de la ligne "fraisage" commence a "2026-05-11T09:00:00Z" se partage en
      | debut                | fin                  | diviseur | mainDOeuvre | paralleles |
      | 2026-05-11T09:00:00Z | 2026-05-11T10:00:00Z | 1        | 20.00       |            |
      | 2026-05-11T10:00:00Z | 2026-05-11T11:00:00Z | 2        | 10.00       | tour@OF D2 |
      | 2026-05-11T11:00:00Z | 2026-05-11T12:00:00Z | 1        | 20.00       |            |

  Scenario: Une periode partagee sur trois elements vaut exactement le cout de l'operateur
    Given l'entreprise fabrique "OF 3020"
    And l'entreprise fabrique "OF 3021"
    And l'entreprise fabrique "OF 3022"
    And "OF 3020" est mis en atelier a "2026-05-11T07:00:00Z"
    And "OF 3021" est mis en atelier a "2026-05-11T07:00:00Z"
    And "OF 3022" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3020" au poste "fraiseuse" a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3021" au poste "tour" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3022" sans poste a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3020" au poste "fraiseuse" a "2026-05-11T09:20:00Z"
    And "dupont" pointe "FIN" sur "OF 3021" au poste "tour" a "2026-05-11T09:20:00Z"
    And "dupont" pointe "FIN" sur "OF 3022" sans poste a "2026-05-11T09:20:00Z"
    # De 9 h a 9 h 20, trois postes se partagent 20 EUR x 1/3 h = 6,67 EUR : 2,23 + 2,22 + 2,22. Arrondi element par
    # element, l'operateur n'aurait coute que 3 x 2,22 = 6,66 EUR. Le centime va a la fraiseuse, commencee a 8 h,
    # qui porte aussi son heure seule : 20,00 + 2,23 EUR.
    When je consulte le cout de revient de "OF 3020" a "2026-05-11T18:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT1H20M | PT0S          | 60.00   | 22.23       |
    When je consulte le cout de revient de "OF 3021" a "2026-05-11T18:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | tournage | PT20M   | PT0S          | 20.00   | 2.22        |
    When je consulte le cout de revient de "OF 3022" a "2026-05-11T18:00:00Z"
    Then le rapport porte les lignes
      | nature | travail | nonConformite | machine | mainDOeuvre |
      | null   | PT20M   | PT0S          | 0.00    | 2.22        |

  Scenario: La cloture referme le travail laisse ouvert
    Given l'entreprise fabrique "OF 3007"
    And "OF 3007" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3007" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "OF 3007" est cloture a "2026-05-11T10:00:00Z"
    When je consulte le cout de revient de "OF 3007" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT1H    | PT0S          | 45.00   | 20.00       |

  Scenario: Un pointage sans poste n'a ni nature ni cout machine
    Given l'entreprise fabrique "OF 3008"
    And "OF 3008" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3008" sans poste a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3008" sans poste a "2026-05-11T10:00:00Z"
    When je consulte le cout de revient de "OF 3008" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Comportement nominal d'une entreprise sans parc machine, pas un cas degrade : l'operateur reste paye.
    And le rapport porte les lignes
      | nature | travail | nonConformite | machine | mainDOeuvre |
      | null   | PT1H    | PT0S          | 0.00    | 20.00       |

  Scenario: Un poste a 0 EUR de l'heure ne coute rien en machine, mais reste valorise
    Given le rapport connait le poste "etabli" de nature "montage" a "0" de l'heure
    And le rapport connait l'operateur "martin" a "20.00" de l'heure, habilite sur
      | etabli |
    And l'entreprise fabrique "OF 3020"
    And "OF 3020" est mis en atelier a "2026-05-11T07:00:00Z"
    And "martin" pointe "DEBUT" sur "OF 3020" au poste "etabli" a "2026-05-11T09:00:00Z"
    And "martin" pointe "FIN" sur "OF 3020" au poste "etabli" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "OF 3020" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Le poste ne demande que de la main d'oeuvre : sa machine coute 0 EUR, l'operateur ses deux heures.
    And le rapport porte les lignes
      | nature  | travail | nonConformite | machine | mainDOeuvre |
      | montage | PT2H    | PT0S          | 0.00    | 40.00       |
    # Un cout saisi a 0 se distingue d'un poste non valorise : le pointage le porte, il n'est pas absent.
    And le pointage de la ligne "montage" commence a "2026-05-11T09:00:00Z" a un cout horaire de "0.00"

  Scenario: Chaque nature d'operation a sa ligne
    Given l'entreprise fabrique "OF 3009"
    And "OF 3009" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3009" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3009" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3009" au poste "tour" a "2026-05-11T10:30:00Z"
    And "dupont" pointe "FIN" sur "OF 3009" au poste "tour" a "2026-05-11T11:30:00Z"
    When je consulte le cout de revient de "OF 3009" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Rien n'est mene de front : chaque heure est payee entiere, et les lignes sortent dans l'ordre des natures.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT1H    | PT0S          | 45.00   | 20.00       |
      | tournage | PT1H    | PT0S          | 60.00   | 20.00       |

  Scenario: Renommer une nature pendant un ordre garde une seule ligne, au nouveau libelle
    Given le rapport connait le poste "soudeuse" de nature "soudure CRA" a "50.00" de l'heure
    And le rapport connait l'operateur "martin" a "20.00" de l'heure, habilite sur
      | soudeuse |
    And l'entreprise fabrique "OF 3011"
    And "OF 3011" est mis en atelier a "2026-05-11T07:00:00Z"
    And "martin" pointe "DEBUT" sur "OF 3011" au poste "soudeuse" a "2026-05-11T09:00:00Z"
    And "martin" pointe "FIN" sur "OF 3011" au poste "soudeuse" a "2026-05-11T10:00:00Z"
    And la nature de travail "soudure CRA" est renommee en "soudage CRA"
    And "martin" pointe "DEBUT" sur "OF 3011" au poste "soudeuse" a "2026-05-11T10:30:00Z"
    And "martin" pointe "FIN" sur "OF 3011" au poste "soudeuse" a "2026-05-11T11:30:00Z"
    When je consulte le cout de revient de "OF 3011" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le rapport porte les lignes
      | nature      | travail | nonConformite | machine | mainDOeuvre |
      | soudage CRA | PT2H    | PT0S          | 100.00  | 40.00       |

  Scenario: Un element inconnu renvoie 404
    When je consulte le cout de revient de l'element inconnu "11111111-2222-3333-4444-555555555555"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:cout-de-revient:element-de-fabrication-introuvable"

  Scenario: Un operateur ne lit pas les couts
    Given l'entreprise fabrique "OF 3010"
    And I am logged in as "user" with role "USER"
    When je consulte le cout de revient de "OF 3010" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 403

  @t7_exclusion
  Scenario: Une activite ouverte est exclue du cout et du diviseur puis entre apres sa fin
    Given l'entreprise fabrique "OF T7 A"
    And l'entreprise fabrique "OF T7 B"
    And "OF T7 A" est mis en atelier a "2026-05-11T07:00:00Z"
    And "OF T7 B" est mis en atelier a "2026-05-11T07:00:00Z"
    And pour le cout, "OF T7 A" recoit les pointages
      | alias | type  | operateur | poste     | survenue             |
      | A     | DEBUT | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
      | F     | FIN   | dupont    | fraiseuse | 2026-05-11T10:00:00Z |
    And pour le cout, "OF T7 B" recoit les pointages
      | alias | type  | operateur | poste | survenue             |
      | B     | DEBUT | dupont    | tour  | 2026-05-11T09:00:00Z |
    When je consulte le cout de revient de "OF T7 A" a "2026-05-11T10:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT2H    | PT0S          | 90.00   | 40.00       |
    And le cout est evalue a "2026-05-11T10:00:00Z" avec 0 activites en cours exclues
    When je consulte le cout de revient de "OF T7 B" a "2026-05-11T10:00:00Z"
    Then le rapport ne porte aucune ligne
    And le cout total du rapport est "0.00" dont "0.00" de machine
    And le cout est evalue a "2026-05-11T10:00:00Z" avec 1 activites en cours exclues
    Given pour le cout, "OF T7 B" recoit les pointages
      | alias | type | operateur | poste | survenue             |
      | FB    | FIN  | dupont    | tour  | 2026-05-11T11:00:00Z |
    When je consulte le cout de revient de "OF T7 A" a "2026-05-11T11:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT2H    | PT0S          | 90.00   | 30.00       |
    When je consulte le cout de revient de "OF T7 B" a "2026-05-11T11:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | tournage | PT2H    | PT0S          | 120.00  | 30.00       |

  Scenario: La fin automatique compte treize heures exactement et rend sa periode
    Given l'entreprise fabrique "OF T7 auto"
    And "OF T7 auto" est mis en atelier a "2026-05-11T07:00:00Z"
    And pour le cout, "OF T7 auto" recoit les pointages
      | alias | type  | operateur | poste     | survenue             |
      | A     | DEBUT | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
    When je consulte le cout de revient de "OF T7 auto" a "2026-05-11T20:59:00Z"
    Then le rapport ne porte aucune ligne
    And le cout est evalue a "2026-05-11T20:59:00Z" avec 1 activites en cours exclues
    When je consulte le cout de revient de "OF T7 auto" a "2026-05-11T21:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT13H   | PT0S          | 585.00  | 260.00      |
    And le cout est evalue a "2026-05-11T21:00:00Z" avec 0 activites en cours exclues
    And le cout porte la fin automatique de "2026-05-11T08:00:00Z" a "2026-05-11T21:00:00Z"
    And la ligne "fraisage" detaille les pointages
      | operateur | poste     | debut                | fin                  | anomalies       | machine | mainDOeuvre |
      | dupont    | fraiseuse | 2026-05-11T08:00:00Z | 2026-05-11T21:00:00Z | FIN_AUTOMATIQUE | 585.00  | 260.00      |
    When je consulte le cout de revient de "OF T7 auto" a "2026-05-12T10:00:00Z"
    Then le cout porte la fin automatique de "2026-05-11T08:00:00Z" a "2026-05-11T21:00:00Z"

  Scenario Outline: La borne effective respecte les faits tardifs, l'echeance et la regularisation
    Given l'entreprise fabrique "fin tardive"
    And "fin tardive" est mis en atelier a "2026-05-11T05:00:00Z"
    And pour le cout, "fin tardive" recoit les pointages
      | alias | type  | operateur | poste     | survenue             |
      | A     | DEBUT | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
    When je consulte le cout de revient de "fin tardive" a "2026-05-11T21:00:00Z"
    Then le cout porte 1 fins automatiques
    Given pour le cout, "fin tardive" recoit les pointages
      | alias | type | cible | operateur | poste     | survenue | reception            | acte   | reponse   |
      | F     | FIN  | A     | dupont    | fraiseuse | <fin>    | 2026-05-12T08:00:00Z | <acte> | <reponse> |
    When je consulte le cout de revient de "fin tardive" a "2026-05-12T09:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine   | mainDOeuvre |
      | fraisage | <temps> | PT0S          | <machine> | <humain>    |
    And le cout porte <auto> fins automatiques

    Examples:
      | fin                  | acte           | reponse | temps | machine | humain | auto |
      | 2026-05-11T17:00:00Z |                | accepte | PT9H  | 405.00  | 180.00 | 0    |
      | 2026-05-11T23:00:00Z |                | ignore  | PT13H | 585.00  | 260.00 | 1    |
      | 2026-05-11T23:00:00Z | REGULARISATION | accepte | PT15H | 675.00  | 300.00 | 0    |
      | 2026-05-11T21:00:00Z |                | ignore  | PT13H | 585.00  | 260.00 | 1    |

  Scenario Outline: Une fin tardive suivie d'une non conformite conserve le trou ou la duree explicitement regularisee
    Given l'entreprise fabrique "fin puis NC tardives"
    And "fin puis NC tardives" est mis en atelier a "2026-05-11T05:00:00Z"
    And pour le cout, "fin puis NC tardives" recoit les pointages
      | alias | type           | cible | operateur | poste     | survenue             | acte   | reponse   |
      | A     | DEBUT          |       | dupont    | fraiseuse | 2026-05-11T08:00:00Z |        |           |
      | FA    | FIN            | A     | dupont    | fraiseuse | 2026-05-11T23:00:00Z | <acte> | <reponse> |
      | N     | NON_CONFORMITE |       | dupont    | fraiseuse | 2026-05-11T23:00:00Z |        |           |
      | FN    | FIN            |       | dupont    | fraiseuse | 2026-05-12T00:00:00Z |        |           |
    When je consulte le cout de revient de "fin puis NC tardives" a "2026-05-12T01:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine   | mainDOeuvre |
      | fraisage | <temps> | PT1H          | <machine> | <humain>    |
    And le cout porte <auto> fins automatiques
    And le rapport porte la non conformite de "2026-05-11T23:00:00Z" a "2026-05-12T00:00:00Z"

    Examples:
      | acte           | reponse | temps | machine | humain | auto |
      |                | ignore  | PT13H | 630.00  | 280.00 | 1    |
      | REGULARISATION | accepte | PT15H | 720.00  | 320.00 | 0    |

  Scenario: Le passage du travail a la NC laisse son activite ouverte exclue jusqu'a sa propre echeance
    Given l'entreprise fabrique "fin puis NC"
    And "fin puis NC" est mis en atelier a "2026-05-11T05:00:00Z"
    And pour le cout, "fin puis NC" recoit les pointages
      | alias | type           | operateur | poste     | survenue             |
      | A     | DEBUT          | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
      | FA    | FIN            | dupont    | fraiseuse | 2026-05-11T12:00:00Z |
      | N     | NON_CONFORMITE | dupont    | fraiseuse | 2026-05-11T12:00:00Z |
    When je consulte le cout de revient de "fin puis NC" a "2026-05-11T21:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT4H    | PT0S          | 180.00  | 80.00       |
    And le cout est evalue a "2026-05-11T21:00:00Z" avec 1 activites en cours exclues
    And le cout ne porte aucune fin automatique
    When je consulte le cout de revient de "fin puis NC" a "2026-05-12T01:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT4H    | PT13H         | 765.00  | 340.00      |
    And le cout porte 1 fins automatiques
    And le rapport porte la non conformite de "2026-05-11T12:00:00Z" a "2026-05-12T01:00:00Z"

  Scenario: Plusieurs elements sur le meme poste et sans poste ne multiplient pas les postes distincts
    Given l'entreprise fabrique "poste A"
    And l'entreprise fabrique "poste B"
    And l'entreprise fabrique "sans poste"
    And "poste A" est mis en atelier a "2026-05-11T05:00:00Z"
    And "poste B" est mis en atelier a "2026-05-11T05:00:00Z"
    And "sans poste" est mis en atelier a "2026-05-11T05:00:00Z"
    And pour le cout, "poste A" recoit les pointages
      | alias | type  | operateur | poste     | survenue             |
      | A     | DEBUT | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
      | FA    | FIN   | dupont    | fraiseuse | 2026-05-11T10:00:00Z |
    And pour le cout, "poste B" recoit les pointages
      | alias | type  | operateur | poste     | survenue             |
      | B     | DEBUT | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
      | FB    | FIN   | dupont    | fraiseuse | 2026-05-11T10:00:00Z |
    When je consulte le cout de revient de "poste A" a "2026-05-11T11:00:00Z"
    Then le total du cout "$.cout.mainDOeuvre" vaut "40.00"
    Given pour le cout, "sans poste" recoit les pointages
      | alias | type  | operateur | survenue             |
      | S     | DEBUT | dupont    | 2026-05-11T09:00:00Z |
      | FS    | FIN   | dupont    | 2026-05-11T10:00:00Z |
    When je consulte le cout de revient de "poste A" a "2026-05-11T11:00:00Z"
    Then le total du cout "$.cout.machine" vaut "90.00"
    And le total du cout "$.cout.mainDOeuvre" vaut "30.00"
    When je consulte le cout de revient de "sans poste" a "2026-05-11T11:00:00Z"
    Then le rapport porte les lignes
      | nature | travail | nonConformite | machine | mainDOeuvre |
      | null   | PT1H    | PT0S          | 0.00    | 10.00       |

  @cout-arrondi-relais
  Scenario: Un relais sur le meme poste conserve sa fenetre de partage sur deux elements
    Given le rapport connait l'operateur "relais_arrondi_2045" a "2.00" de l'heure, habilite sur
      | fraiseuse |
    And l'entreprise fabrique "OF RELAIS A 2045"
    And l'entreprise fabrique "OF RELAIS C 2045"
    And "OF RELAIS A 2045" est mis en atelier a "2045-05-11T07:00:00Z"
    And "OF RELAIS C 2045" est mis en atelier a "2045-05-11T07:00:00Z"
    And "relais_arrondi_2045" pointe "DEBUT" sur "OF RELAIS A 2045" au poste "fraiseuse" a "2045-05-11T08:00:00Z"
    And "relais_arrondi_2045" pointe "FIN" sur "OF RELAIS A 2045" au poste "fraiseuse" a "2045-05-11T08:01:00Z"
    And "relais_arrondi_2045" pointe "DEBUT" sur "OF RELAIS C 2045" au poste "fraiseuse" a "2045-05-11T08:01:00Z"
    And "relais_arrondi_2045" pointe "FIN" sur "OF RELAIS C 2045" au poste "fraiseuse" a "2045-05-11T08:02:00Z"
    When je consulte le cout de revient de "OF RELAIS A 2045" a "2045-05-11T18:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT1M    | PT0S          | 0.75    | 0.04        |
    When je consulte le cout de revient de "OF RELAIS C 2045" a "2045-05-11T18:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT1M    | PT0S          | 0.75    | 0.03        |

  Scenario: Le tarif capture a la saisie reste fige apres une revision des tarifs
    Given l'entreprise fabrique "tarif fige"
    And "tarif fige" est mis en atelier a "2026-05-11T05:00:00Z"
    And pour le cout, "tarif fige" recoit les pointages
      | alias | type  | operateur | poste     | survenue             |
      | A     | DEBUT | dupont    | fraiseuse | 2026-05-11T08:00:00Z |
      | F     | FIN   | dupont    | fraiseuse | 2026-05-11T10:00:00Z |
    And pour le cout, le poste "fraiseuse" est revise a "100.00" de l'heure et l'operateur "dupont" a "50.00" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "tarif fige" a "2026-05-11T12:00:00Z"
    Then le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | fraisage | PT2H    | PT0S          | 90.00   | 40.00       |
