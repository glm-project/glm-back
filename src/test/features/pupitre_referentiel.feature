Feature: Le referentiel que le pupitre met en cache

  # Le pupitre d'atelier travaille hors ligne : il garde sur disque de quoi designer un operateur, lui proposer ses
  # postes et afficher les elements pointables, puis rejoue ses gestes a la reconnexion. Cette route est la lecture
  # qui alimente ce cache, en un seul appel et dans une seule transaction — la pagination est exactement ce qui
  # empecherait de prouver que deux collections viennent du meme etat de la base.
  #
  # Ce scenario pointe par l'API d'atelier et relit par celle du pupitre : c'est ce qui tient alignes les deux replis
  # de journal, qu'aucun import Java ne relie.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And le pupitre connait le poste "fraiseuse"
    And le pupitre connait le poste "tour"
    And le pupitre connait l'operateur "dupont" habilite sur
      | fraiseuse |
      | tour      |

  Scenario: Le referentiel porte de quoi designer un operateur et lui proposer ses postes
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200
    And le referentiel du pupitre est date du "2026-05-11T07:00:00Z"
    And le referentiel du pupitre porte l'operateur "dupont" avec son matricule
    And les postes proposes a "dupont" sont
      | fraiseuse |
      | tour      |

  Scenario: Un operateur sans journee en cours est absent
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then "dupont" est "ABSENT" au referentiel du pupitre

  Scenario: Une arrivee rend l'operateur present
    Given au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T09:00:00Z"
    Then "dupont" est "PRESENT" au referentiel du pupitre

  Scenario: Une pause suspend la presence sans y mettre fin
    Given au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe sa presence "PAUSE" a "2026-05-11T12:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T12:30:00Z"
    Then "dupont" est "EN_PAUSE" au referentiel du pupitre

  Scenario: Un depart referme la journee et rend l'operateur absent
    Given au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe sa presence "DEPART" a "2026-05-11T17:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T18:00:00Z"
    Then "dupont" est "ABSENT" au referentiel du pupitre

  Scenario: Un poste de nuit ouvert la veille reste la journee en cours sous le seuil
    # E1 : arrive a 20 h, Dupont est toujours la a 3 h. Il le reste jusqu'a 09:00, son arrivee plus 13 h.
    Given au pupitre, "dupont" prend son poste a "2026-05-10T20:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T03:00:00Z"
    Then "dupont" est "PRESENT" au referentiel du pupitre
    And "dupont" reste present jusqu'a "2026-05-11T09:00:00Z" au referentiel du pupitre

  Scenario: Une pause garde son echeance de presence
    Given au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe sa presence "PAUSE" a "2026-05-11T12:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T12:30:00Z"
    Then "dupont" est "EN_PAUSE" au referentiel du pupitre
    And "dupont" reste present jusqu'a "2026-05-11T21:00:00Z" au referentiel du pupitre

  Scenario: Au seuil pile, l'operateur est encore present
    Given au pupitre, "dupont" prend son poste a "2026-05-11T07:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T20:00:00Z"
    Then "dupont" est "PRESENT" au referentiel du pupitre

  Scenario: Une journee abandonnee rend l'operateur absent
    # E2 : lundi 21:00, la journee ouverte a 07:00 est abandonnee depuis 20:00. Le pupitre ne propose plus que l'arrivee.
    Given au pupitre, "dupont" prend son poste a "2026-05-11T07:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T21:00:00Z"
    Then "dupont" est "ABSENT" au referentiel du pupitre
    And "dupont" n'a aucune echeance de presence au referentiel du pupitre

  Scenario: Un depart referme la journee sans echeance
    Given au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe sa presence "DEPART" a "2026-05-11T17:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T18:00:00Z"
    Then "dupont" n'a aucune echeance de presence au referentiel du pupitre

  Scenario: Apres une journee abandonnee, la nouvelle arrivee rend l'operateur present
    Given au pupitre, "dupont" prend son poste a "2026-05-10T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T07:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T09:00:00Z"
    Then "dupont" est "PRESENT" au referentiel du pupitre
    And "dupont" reste present jusqu'a "2026-05-11T20:00:00Z" au referentiel du pupitre

  Scenario: Un depart tardif ne laisse pas l'operateur present sur la journee abandonnee
    # E5 : le depart recu mardi ouvre et ferme une journee ; lundi, sans depart, reste abandonnee.
    Given au pupitre, "dupont" prend son poste a "2026-05-10T07:00:00Z"
    And au pupitre, "dupont" pointe sa presence "DEPART" a "2026-05-11T08:30:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T09:00:00Z"
    Then "dupont" est "ABSENT" au referentiel du pupitre

  Scenario: Un element mis en atelier apparait en attente, avec sa reference relue au referentiel
    Given le pupitre fabrique "OF 4001"
    And "OF 4001" est engage au pupitre a "2026-05-11T07:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T07:30:00Z"
    Then la reponse a le statut http 200
    And "OF 4001" figure au referentiel du pupitre dans l'etat "EN_ATTENTE"
    And "OF 4001" porte au referentiel du pupitre sa reference et son nom d'atelier
    And "OF 4001" ne porte aucune activite au referentiel du pupitre

  Scenario: Un debut ouvre une activite datee, que la non conformite ne referme pas
    Given le pupitre fabrique "OF 4002"
    And "OF 4002" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4002" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    Then "OF 4002" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And les activites de "OF 4002" au referentiel du pupitre sont
      | operateur | poste     | categorie | depuis               |
      | dupont    | fraiseuse | TRAVAIL   | 2026-05-11T09:00:00Z |

  Scenario: Une activite relancee reste unique et repart de la relance
    Given le pupitre fabrique "OF 4010"
    And "OF 4010" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4010" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4010" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T11:00:00Z"
    Then "OF 4010" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And les activites de "OF 4010" au referentiel du pupitre sont
      | operateur | poste     | categorie | depuis               |
      | dupont    | fraiseuse | TRAVAIL   | 2026-05-11T10:00:00Z |

  Scenario: Une non conformite relancee reste unique et repart de la relance
    Given le pupitre fabrique "OF 4011"
    And "OF 4011" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "NON_CONFORMITE" sur "OF 4011" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And au pupitre, "dupont" pointe "NON_CONFORMITE" sur "OF 4011" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T11:00:00Z"
    Then les activites de "OF 4011" au referentiel du pupitre sont
      | operateur | poste     | categorie      | depuis               |
      | dupont    | fraiseuse | NON_CONFORMITE | 2026-05-11T10:00:00Z |

  Scenario: Une non conformite change la categorie sans fermer l'activite
    Given le pupitre fabrique "OF 4003"
    And "OF 4003" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4003" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And au pupitre, "dupont" pointe "NON_CONFORMITE" sur "OF 4003" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T11:00:00Z"
    # L'etat de l'element ne bouge pas : ce temps-la se compte aussi. C'est la categorie qui le dit.
    Then "OF 4003" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And les activites de "OF 4003" au referentiel du pupitre sont
      | operateur | poste     | categorie      | depuis               |
      | dupont    | fraiseuse | NON_CONFORMITE | 2026-05-11T10:00:00Z |

  Scenario: Une fin laisse l'element interrompu, sans activite
    Given le pupitre fabrique "OF 4004"
    And "OF 4004" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4004" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And au pupitre, "dupont" pointe "FIN" sur "OF 4004" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T12:00:00Z"
    Then "OF 4004" figure au referentiel du pupitre dans l'etat "INTERROMPU"
    And "OF 4004" ne porte aucune activite au referentiel du pupitre

  Scenario: Un pointage sans poste de travail reste une activite a part entiere
    Given le pupitre fabrique "OF 4005"
    And "OF 4005" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4005" sans poste a "2026-05-11T09:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    Then "OF 4005" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And l'activite de "OF 4005" au referentiel du pupitre ne porte aucun poste

  Scenario: Un evenement annule disparait du repli
    Given le pupitre fabrique "OF 4006"
    And "OF 4006" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4006" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And le dernier pointage sur "OF 4006" est annule a "2026-05-11T09:30:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    # L'evenement reste au journal de l'atelier, porteur de son annulation ; le repli l'ecarte.
    Then "OF 4006" figure au referentiel du pupitre dans l'etat "EN_ATTENTE"
    And "OF 4006" ne porte aucune activite au referentiel du pupitre

  Scenario: Un element cloture quitte le referentiel du pupitre
    Given le pupitre fabrique "OF 4007"
    And "OF 4007" est engage au pupitre a "2026-05-11T07:00:00Z"
    And "OF 4007" est cloture au pupitre a "2026-05-11T17:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T18:00:00Z"
    Then "OF 4007" ne figure pas au referentiel du pupitre

  Scenario: Un element sans reference garde une tuile nominale
    Given le pupitre fabrique "PRD 4011" sans reference
    And "PRD 4011" est engage au pupitre a "2026-05-11T07:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T08:00:00Z"
    # Toutes les entreprises n'attribuent pas de reference : son absence redonne un comportement nominal, pas un cas
    # degrade. Le pupitre affiche alors le nom numerote par le serveur.
    Then "PRD 4011" figure au referentiel du pupitre dans l'etat "EN_ATTENTE"
    And "PRD 4011" ne porte aucune reference au referentiel du pupitre

  Scenario: Un element supprime du referentiel laisse sa tuile intacte au pupitre
    Given le pupitre fabrique "OF 4009"
    And "OF 4009" est engage au pupitre a "2026-05-11T07:00:00Z"
    And "OF 4009" est supprime du referentiel
    When je lis le referentiel du pupitre a "2026-05-11T08:00:00Z"
    # Le nom est copie a l'engagement, la tuile survit ; la reference est relue, elle disparait avec la fiche.
    Then "OF 4009" figure au referentiel du pupitre dans l'etat "EN_ATTENTE"
    And "OF 4009" ne porte aucune reference au referentiel du pupitre

  Scenario: Le referentiel d'une autre entreprise ne porte rien de celle-ci
    Given le pupitre fabrique "OF 4010"
    And "OF 4010" est engage au pupitre a "2026-05-11T07:00:00Z"
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je lis le referentiel du pupitre a "2026-05-11T08:00:00Z"
    Then la reponse a le statut http 200
    And le referentiel du pupitre ne porte aucun element
    And "OF 4010" ne figure pas au referentiel du pupitre

  Scenario: La reponse ne porte aucun montant
    Given le pupitre fabrique "OF 4008"
    And "OF 4008" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" prend son poste a "2026-05-11T08:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4008" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    # Un ecran d'atelier partage n'a aucune raison de recevoir ce que le rapport de cout de revient reserve au
    # gestionnaire.
    Then le referentiel du pupitre ne porte ni taux horaire ni cout horaire

  Scenario: L'operateur au pupitre lit son referentiel
    Given I am logged in as "pupitre-atelier-1" with role "USER"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200

  Scenario: Un jeton sans entreprise n'atteint pas le referentiel
    Given I am logged in as "pupitre-atelier-1" with role "USER" without tenant
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 403
