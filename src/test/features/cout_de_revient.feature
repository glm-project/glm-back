Feature: Cout de revient d'un element de fabrication

  # L'atelier capture le temps sans jamais le chiffrer : il copie le cout horaire du poste et le taux horaire de
  # l'operateur sur chaque evenement du journal, mais aucune arithmetique ne les combine chez lui. C'est ici que le
  # calcul se fait, sur ces valeurs figees a la saisie — un tarif revise depuis ne reecrit pas le prix d'heures deja
  # passees.
  #
  # Deux regles gouvernent les montants, enoncees deux fois de suite par le client : le cout horaire de chaque machine
  # active court en entier, et le taux horaire de l'operateur se divise par le nombre de machines qu'il utilise.
  #
  # Ce scenario pointe par l'API d'atelier et relit par celle du cout de revient : c'est ce qui tient alignes les deux
  # replis de journal, qu'aucun import Java ne relie.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And le rapport connait le poste "fraiseuse" de nature "Fraisage" a "45.00" de l'heure
    And le rapport connait le poste "tour" de nature "Tournage" a "60.00" de l'heure
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
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3002" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3002" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "OF 3002" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # 45 EUR de machine et 20 EUR d'operateur, pendant deux heures.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | Fraisage | PT2H    | PT0S          | 90.00   | 40.00       |

  Scenario: La pause de midi retire son creux du temps valorise
    Given l'entreprise fabrique "OF 3003"
    And "OF 3003" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3003" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe sa presence "PAUSE" a "2026-05-11T12:00:00Z"
    And "dupont" pointe sa presence "REPRISE" a "2026-05-11T13:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3003" au poste "fraiseuse" a "2026-05-11T14:00:00Z"
    When je consulte le cout de revient de "OF 3003" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Quatre heures pointees, trois heures de presence effective : la pause n'a jamais eu besoin d'etre recopiee dans
    # le journal de l'element.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | Fraisage | PT3H    | PT0S          | 135.00  | 60.00       |

  Scenario: La reprise de non conformite se compte a part, datee
    Given l'entreprise fabrique "OF 3004"
    And "OF 3004" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "NON_CONFORMITE" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3004" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je consulte le cout de revient de "OF 3004" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Une piece ratee se refait au meme tarif : le cout ne bouge pas, seul le partage du temps change.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | Fraisage | PT1H    | PT1H          | 90.00   | 40.00       |
    And le rapport porte la non conformite de "2026-05-11T10:00:00Z" a "2026-05-11T11:00:00Z"

  Scenario: Deux machines menees de front divisent l'operateur, jamais les machines
    Given l'entreprise fabrique "OF 3005"
    And "OF 3005" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
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
      | Fraisage | PT3H    | PT0S          | 135.00  | 50.00       |
      | Tournage | PT1H    | PT0S          | 60.00   | 10.00       |

  Scenario: Un travail encore en cours est arrete au depart de l'operateur
    Given l'entreprise fabrique "OF 3006"
    And "OF 3006" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3006" au poste "fraiseuse" a "2026-05-11T14:00:00Z"
    And "dupont" pointe sa presence "DEPART" a "2026-05-11T17:00:00Z"
    When je consulte le cout de revient de "OF 3006" a "2026-05-11T23:00:00Z"
    Then la reponse a le statut http 200
    # Le depart referme ce que l'operateur a oublie d'arreter : trois heures, pas neuf.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | Fraisage | PT3H    | PT0S          | 135.00  | 60.00       |

  Scenario: La cloture referme le travail laisse ouvert
    Given l'entreprise fabrique "OF 3007"
    And "OF 3007" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3007" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "OF 3007" est cloture a "2026-05-11T10:00:00Z"
    When je consulte le cout de revient de "OF 3007" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | Fraisage | PT1H    | PT0S          | 45.00   | 20.00       |

  Scenario: Un pointage sans poste n'a ni nature ni cout machine
    Given l'entreprise fabrique "OF 3008"
    And "OF 3008" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3008" sans poste a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3008" sans poste a "2026-05-11T10:00:00Z"
    When je consulte le cout de revient de "OF 3008" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Comportement nominal d'une entreprise sans parc machine, pas un cas degrade : l'operateur reste paye.
    And le rapport porte les lignes
      | nature | travail | nonConformite | machine | mainDOeuvre |
      | null   | PT1H    | PT0S          | 0.00    | 20.00       |

  Scenario: Chaque nature d'operation a sa ligne
    Given l'entreprise fabrique "OF 3009"
    And "OF 3009" est mis en atelier a "2026-05-11T07:00:00Z"
    And "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3009" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And "dupont" pointe "FIN" sur "OF 3009" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    And "dupont" pointe "DEBUT" sur "OF 3009" au poste "tour" a "2026-05-11T10:30:00Z"
    And "dupont" pointe "FIN" sur "OF 3009" au poste "tour" a "2026-05-11T11:30:00Z"
    When je consulte le cout de revient de "OF 3009" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 200
    # Rien n'est mene de front : chaque heure est payee entiere, et les lignes sortent dans l'ordre des natures.
    And le rapport porte les lignes
      | nature   | travail | nonConformite | machine | mainDOeuvre |
      | Fraisage | PT1H    | PT0S          | 45.00   | 20.00       |
      | Tournage | PT1H    | PT0S          | 60.00   | 20.00       |

  Scenario: Un element inconnu renvoie 404
    When je consulte le cout de revient de l'element inconnu "11111111-2222-3333-4444-555555555555"
    Then la reponse a le statut http 404

  Scenario: Un operateur ne lit pas les couts
    Given l'entreprise fabrique "OF 3010"
    And I am logged in as "user" with role "USER"
    When je consulte le cout de revient de "OF 3010" a "2026-05-11T18:00:00Z"
    Then la reponse a le statut http 403
