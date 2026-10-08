Feature: Le referentiel que le pupitre met en cache

  # Le pupitre d'atelier travaille hors ligne : il garde sur disque de quoi designer un operateur, lui proposer ses
  # postes et afficher les elements pointables, puis rejoue ses gestes a la reconnexion. Cette route est la lecture
  # qui alimente ce cache, en un seul appel et dans une seule transaction — la pagination est exactement ce qui
  # empecherait de prouver que deux collections viennent du meme etat de la base.
  #
  # Les scenarios ecrivent par l'API d'atelier et relisent ses projections par celle du pupitre, sans import Java
  # entre les deux contextes.
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
    And le referentiel du pupitre porte l'operateur "dupont" avec son identifiant
    And "dupont" ne porte aucun etat ni echeance de presence au referentiel du pupitre
    And les postes proposes a "dupont" sont
      | fraiseuse |
      | tour      |

  Scenario: Le referentiel range les categories de produit dans l'ordre choisi par le gestionnaire
    # Le pupitre range ses tuiles par categorie : il lui faut les codes, dans l'ordre de la gestion.
    Given la categorie de produit "PUPITREA" est declaree
    And la categorie de produit "PUPITREB" est declaree
    And je place la categorie de produit "PUPITREB" en tete
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200
    And les categories du referentiel du pupitre commencent par "PUPITREB"
    And les categories du referentiel du pupitre suivent l'ordre des categories de produit

  Scenario: Un element mis en atelier apparait en attente, avec sa reference relue au referentiel
    Given il est "2026-05-11T06:00:00Z"
    And le pupitre fabrique "OF 4001"
    And "OF 4001" est engage au pupitre a "2026-05-11T07:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T07:30:00Z"
    Then la reponse a le statut http 200
    And "OF 4001" figure au referentiel du pupitre dans l'etat "EN_ATTENTE"
    And "OF 4001" porte au referentiel du pupitre sa reference et son nom d'atelier
    And "OF 4001" ne porte aucune activite au referentiel du pupitre
    Given la reference de "OF 4001" devient "M-1187 revise" au referentiel de fabrication
    Then la reponse a le statut http 200
    When je lis le referentiel du pupitre a "2026-05-11T07:45:00Z"
    Then "OF 4001" porte au referentiel du pupitre sa reference et son nom d'atelier

  Scenario: Un debut ouvre une activite datee, en cours au referentiel
    Given le pupitre fabrique "OF 4002"
    And "OF 4002" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4002" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    Then "OF 4002" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And les activites de "OF 4002" au referentiel du pupitre sont
      | operateur | poste     | categorie | depuis               |
      | dupont    | fraiseuse | TRAVAIL   | 2026-05-11T09:00:00Z |

  Scenario: Une non conformite pointee apres une fin ouvre une activite de non conformite
    Given le pupitre fabrique "OF 4003"
    And "OF 4003" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4003" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And au pupitre, "dupont" pointe "FIN" sur "OF 4003" au poste "fraiseuse" a "2026-05-11T10:00:00Z"
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
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4004" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    And au pupitre, "dupont" pointe "FIN" sur "OF 4004" au poste "fraiseuse" a "2026-05-11T11:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T12:00:00Z"
    Then "OF 4004" figure au referentiel du pupitre dans l'etat "INTERROMPU"
    And "OF 4004" ne porte aucune activite au referentiel du pupitre

  Scenario: Un pointage sans poste de travail reste une activite a part entiere
    Given le pupitre fabrique "OF 4005"
    And "OF 4005" est engage au pupitre a "2026-05-11T07:00:00Z"
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4005" sans poste a "2026-05-11T09:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    Then "OF 4005" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And l'activite de "OF 4005" au referentiel du pupitre ne porte aucun poste

  Scenario: Un element cloture quitte le referentiel du pupitre
    Given le pupitre fabrique "OF 4007"
    And "OF 4007" est engage au pupitre a "2026-05-11T07:00:00Z"
    And "OF 4007" est cloture au pupitre a "2026-05-11T17:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T18:00:00Z"
    Then "OF 4007" ne figure pas au referentiel du pupitre

  Scenario: Une activite que rien n'a terminee quitte les activites en cours a son echeance
    # Chaque activite porte son ouverture, que visera une fin ou une transition, et son echeance, son debut plus
    # 13 heures : le pupitre hors ligne la sait expiree a cet instant sans attendre le referentiel suivant.
    Given le pupitre fabrique "OF 4101"
    And "OF 4101" est engage au pupitre a "2026-05-18T07:00:00Z"
    And au pupitre, "dupont" ouvre "A" en "DEBUT" sur "OF 4101" au poste "fraiseuse" a "2026-05-18T08:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-18T20:59:00Z"
    Then "OF 4101" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And les activites de "OF 4101" au referentiel du pupitre sont
      | operateur | poste     | categorie | depuis               | ouverture | echeance             |
      | dupont    | fraiseuse | TRAVAIL   | 2026-05-18T08:00:00Z | A         | 2026-05-18T21:00:00Z |
    # A 21:00 pile, l'activite est terminee automatiquement : elle n'est plus en cours, et le reste ensuite.
    When je lis le referentiel du pupitre a "2026-05-18T21:00:00Z"
    Then "OF 4101" figure au referentiel du pupitre dans l'etat "INTERROMPU"
    And "OF 4101" ne porte aucune activite au referentiel du pupitre
    When je lis le referentiel du pupitre a "2026-05-19T09:00:00Z"
    Then "OF 4101" ne porte aucune activite au referentiel du pupitre

  Scenario: Une fin et une non conformite a la meme heure laissent la non conformite en cours
    # Le bouton NC du pupitre envoie deux pointages a la meme heure, la fin d'abord. Recus hors ligne dans cet ordre, la
    # fin termine A a 10:00 et B, la non conformite, est la seule activite en cours.
    Given le pupitre fabrique "OF 4103"
    And "OF 4103" est engage au pupitre a "2026-05-20T07:00:00Z"
    And au pupitre, "dupont" ouvre "A" en "DEBUT" sur "OF 4103" au poste "fraiseuse" a "2026-05-20T08:00:00Z"
    And au pupitre, "dupont" termine par "F" sur "OF 4103" au poste "fraiseuse" a "2026-05-20T10:00:00Z", recu a "2026-05-20T10:05:00Z"
    And au pupitre, "dupont" ouvre "B" en "NON_CONFORMITE" sur "OF 4103" au poste "fraiseuse" a "2026-05-20T10:00:00Z", recu a "2026-05-20T10:10:00Z"
    When je lis le referentiel du pupitre a "2026-05-20T11:00:00Z"
    Then "OF 4103" figure au referentiel du pupitre dans l'etat "EN_COURS"
    And les activites de "OF 4103" au referentiel du pupitre sont
      | operateur | poste     | categorie      | depuis               | ouverture |
      | dupont    | fraiseuse | NON_CONFORMITE | 2026-05-20T10:00:00Z | B         |

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
    And au pupitre, "dupont" pointe "DEBUT" sur "OF 4008" au poste "fraiseuse" a "2026-05-11T09:00:00Z"
    When je lis le referentiel du pupitre a "2026-05-11T10:00:00Z"
    # Un ecran d'atelier partage n'a aucune raison de recevoir ce que le rapport de cout de revient reserve au
    # gestionnaire.
    Then le referentiel du pupitre ne porte ni taux horaire ni cout horaire

  Scenario: L'operateur au pupitre lit son referentiel
    Given I am logged in as "pupitre-atelier-1" with role "USER"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200

  Scenario: Le referentiel du pupitre donne la duree maximale d'une activite
    # L'echeance de chaque activite est son debut plus cette duree : le pupitre hors ligne la lit ici plutot que de la
    # coder, et le serveur n'en a qu'une source, qu'il partage avec sa regle de reception.
    Given I am logged in as "pupitre-atelier-1" with role "USER"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200
    And le referentiel du pupitre porte la duree maximale d'activite "PT13H"

  Scenario: Un jeton sans entreprise n'atteint pas le referentiel
    Given I am logged in as "pupitre-atelier-1" with role "USER" without tenant
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 403
