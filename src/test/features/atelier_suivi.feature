Feature: Suivi des elements engages en atelier

  # Engager un element est un geste metier du back-office, distinct de sa creation : tout ce qui est cree n'est pas
  # forcement a faire, et c'est cet acte qui le fait apparaitre sur l'ecran des operateurs.
  #
  # Rien de ce qui se deduit n'est stocke. L'etat, les activites en cours et le temps passe sont recalcules du journal
  # a chaque lecture : c'est ce qui permet a une saisie rattrapee de compter a l'heure ou elle a eu lieu.
  # Le pointage designe l'operateur et le poste par leur identifiant dans les referentiels : deux orthographes ne
  # peuvent plus compter pour deux personnes. La nature de l'operation, elle, vient du poste et non de la personne.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-1" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "fraiseuse-2" de nature "tournage"
    And l'entreprise a declare l'operateur "dupont" habilite sur "fraiseuse-1" et "fraiseuse-2" avec un taux horaire de "22.5"
    And l'entreprise a declare l'operateur "martin" sans habilitation

  Scenario: Engager un element de fabrication le fait apparaitre en atelier
    Given il est "2026-05-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2001"
      | categorie | OF   |
      | reference | 2001 |
    When j'engage l'element "OF 2001" en atelier
    Then la reponse a le statut http 201
    # Rien n'a encore ete pointe : l'element attend son premier operateur.
    And le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 0 evenements

  Scenario: La grille conserve les informations du suivi sans son journal
    Given il est "2026-07-02T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2962"
      | categorie | OF   |
      | reference | 2962 |
    And j'ai engage l'element "OF 2962" en atelier
    And j'ai pointe sur "OF 2962"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-07-02T09:00:00Z"
    And j'ai pointe sur "OF 2962"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    And j'ai pointe sur "OF 2962"
      | type      | NON_CONFORMITE |
      | operateur | dupont         |
      | poste     | fraiseuse-1    |
    Then le journal du suivi contient 3 evenements
    # La non conformite est en cours : la comparaison porte sur une activite, pas sur deux listes vides.
    And le suivi a l'etat "EN_COURS"
    And je retiens les informations du suivi hors journal et conflits
    When je liste les elements engages entre "2026-07-02T00:00:00Z" et "2026-07-03T00:00:00Z"
    Then la grille contient les memes informations sans journal ni conflits
    When je consulte "OF 2962"
    Then le journal du suivi contient 3 evenements

  Scenario: Un element deja engage ne peut pas l'etre deux fois
    Given l'entreprise a cree l'element de fabrication "OF 2002"
      | categorie | OF   |
      | reference | 2002 |
    And j'ai engage l'element "OF 2002" en atelier
    When j'engage l'element "OF 2002" en atelier
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:element-deja-engage"

  Scenario: Engager un element inexistant renvoie 404
    When j'engage l'element inconnu "8b5f3d02-7c94-4e16-af28-0315b7c9d2e4" en atelier
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:element-de-fabrication-introuvable"

  Scenario: Un debut de travail met l'element en cours
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2003"
      | categorie | OF   |
      | reference | 2003 |
    And j'ai engage l'element "OF 2003" en atelier
    When je pointe sur "OF 2003"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 201
    And le suivi a l'etat "EN_COURS"
    And le suivi a 1 activites en cours
    # Le journal ne stocke que des identifiants : l'operateur et le poste sont resolus a la lecture.
    And l'evenement 0 du suivi porte l'operateur "dupont" et le poste "fraiseuse-1"
    # La nature vient du poste, jamais de la personne : personne ne l'a saisie sur la fiche de Dupont.
    And l'evenement 0 du suivi a la nature "fraisage"
    # Le cout horaire du poste et le taux horaire de l'operateur sont figes a la saisie, sur le meme patron.
    And l'evenement 0 du suivi a le cout horaire "45.5"
    And l'evenement 0 du suivi a le taux horaire "22.5"

  Scenario: Le pointage d'atelier du pupitre conserve son identifiant et son heure de geste
    Given il est "2026-05-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2999"
      | categorie | OF   |
      | reference | 2999 |
    And j'ai engage l'element "OF 2999" en atelier
    Given il est "2026-05-10T14:00:00Z"
    When je pointe sur "OF 2999"
      | id             | 00000000-0000-0000-0000-000000000023 |
      | type           | DEBUT                                |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-05-10T08:00:00Z                 |
    Then la reponse a le statut http 201
    And l'evenement 0 du suivi a l'identifiant "00000000-0000-0000-0000-000000000023"
    And l'evenement 0 du suivi a survenu a "2026-05-10T08:00:00Z" et a ete saisi a "2026-05-10T14:00:00Z" par "gestionnaire"
    # Saisi apres coup, et par un gestionnaire : c'est pourtant un pointage, la route des pointages n'etant jamais celle
    # d'une regularisation.
    And l'evenement 0 du suivi n'est pas une regularisation

  Scenario: Un pointage d'atelier identique est rejoue sans second evenement
    Given il est "2026-05-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2998"
      | categorie | OF   |
      | reference | 2998 |
    And j'ai engage l'element "OF 2998" en atelier
    Given il est "2026-05-10T08:00:00Z"
    When je pointe sur "OF 2998"
      | id        | 00000000-0000-0000-0000-000000000035 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    When je pointe sur "OF 2998"
      | id        | 00000000-0000-0000-0000-000000000035 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 200
    And le journal du suivi contient 1 evenements

  Scenario: Un pointage d'atelier futur est refuse
    Given il est "2026-05-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2997"
      | categorie | OF   |
      | reference | 2997 |
    And j'ai engage l'element "OF 2997" en atelier
    When je pointe sur "OF 2997"
      | id             | 00000000-0000-0000-0000-000000000036 |
      | type           | DEBUT                                |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-05-10T07:00:00Z                 |
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:date-de-survenue-future"

  Scenario: Un pointage date par un poste qui avance un peu sur le serveur est ramene a l'instant courant
    Given il est "2026-05-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2996"
      | categorie | OF   |
      | reference | 2996 |
    And j'ai engage l'element "OF 2996" en atelier
    When je pointe sur "OF 2996"
      | id             | 00000000-0000-0000-0000-000000000037 |
      | type           | DEBUT                                |
      | operateur      | dupont                               |
      | poste          | fraiseuse-1                          |
      | dateDeSurvenue | 2026-05-10T06:01:30Z                 |
    Then la reponse a le statut http 201
    And l'evenement 0 du suivi a survenu a "2026-05-10T06:00:00Z" et a ete saisi a "2026-05-10T06:00:00Z" par "gestionnaire"

  Scenario: Une non conformite pointee apres une fin ouvre une activite, une reprise se pointe comme un debut
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2004"
      | categorie | OF   |
      | reference | 2004 |
    And j'ai engage l'element "OF 2004" en atelier
    And j'ai pointe sur "OF 2004"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T09:00:00Z"
    And j'ai pointe sur "OF 2004"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When je pointe sur "OF 2004"
      | type      | NON_CONFORMITE |
      | operateur | dupont         |
      | poste     | fraiseuse-1    |
    # L'element reste EN_COURS : la non conformite ouvre une activite, car ce temps-la se compte aussi. INTERROMPU est
    # reserve a l'element sur lequel plus personne ne travaille.
    Then le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "NON_CONFORMITE"
    # La reprise n'a pas de type propre : c'est un DEBUT, et la categorie atteinte suffit a la distinguer.
    Given il est "2026-05-10T10:00:00Z"
    And j'ai pointe sur "OF 2004"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When je pointe sur "OF 2004"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "TRAVAIL"

  Scenario: Un element sur lequel plus personne ne travaille est interrompu
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2013"
      | categorie | OF   |
      | reference | 2013 |
    And j'ai engage l'element "OF 2013" en atelier
    And j'ai pointe sur "OF 2013"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF 2013"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then le suivi a l'etat "INTERROMPU"
    And le suivi a 0 activites en cours

  Scenario: Pointer sur un poste ou l'operateur n'est pas habilite est refuse
    # L'habilitation est la seule regle dure du contexte : le referentiel dit qui peut pointer ou.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2017"
      | categorie | OF   |
      | reference | 2017 |
    And j'ai engage l'element "OF 2017" en atelier
    When je pointe sur "OF 2017"
      | type      | DEBUT       |
      | operateur | martin      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 409

  Scenario: Pointer pour un operateur inconnu du referentiel renvoie 404
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2018"
      | categorie | OF   |
      | reference | 2018 |
    And j'ai engage l'element "OF 2018" en atelier
    When je pointe sur "OF 2018"
      | type      | DEBUT                                |
      | operateur | 9c1f4a67-0d38-4b52-8e91-6a7c5d4b3e20 |
    Then la reponse a le statut http 404

  Scenario: Pointer sur un poste inconnu du referentiel renvoie 404
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2019"
      | categorie | OF   |
      | reference | 2019 |
    And j'ai engage l'element "OF 2019" en atelier
    When je pointe sur "OF 2019"
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | 4b8e2d31-95c0-4f76-a1d3-7e6b0c5a9f42 |
    Then la reponse a le statut http 404

  Scenario: Demarrer une activite deja en cours la relance
    # D9 : l'operateur qui revient sur un element reste ouvert n'est jamais bloque. Son debut ferme la periode
    # precedente et en ouvre une nouvelle, sans trou ni recouvrement.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2005"
      | categorie | OF   |
      | reference | 2005 |
    And j'ai engage l'element "OF 2005" en atelier
    And j'ai pointe sur "OF 2005"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF 2005"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 201
    And le suivi a l'etat "EN_COURS"
    And le suivi a 1 activites en cours
    And l'activite en cours est de categorie "TRAVAIL"
    And le journal du suivi contient 2 evenements

  Scenario: Pointer une non conformite deja en cours la relance
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2090"
      | categorie | OF   |
      | reference | 2090 |
    And j'ai engage l'element "OF 2090" en atelier
    And j'ai pointe sur "OF 2090"
      | type      | NON_CONFORMITE |
      | operateur | dupont         |
      | poste     | fraiseuse-1    |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF 2090"
      | type      | NON_CONFORMITE |
      | operateur | dupont         |
      | poste     | fraiseuse-1    |
    Then la reponse a le statut http 201
    And le suivi a 1 activites en cours
    And l'activite en cours est de categorie "NON_CONFORMITE"
    And le journal du suivi contient 2 evenements

  Scenario: Une relance rejouee par le pupitre ne cree pas un troisieme evenement
    # La relance est un nouveau geste, sous un nouvel identifiant. Le meme geste rejoue reste absorbe.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2091"
      | categorie | OF   |
      | reference | 2091 |
    And j'ai engage l'element "OF 2091" en atelier
    And j'ai pointe sur "OF 2091"
      | id        | 00000000-0000-0000-0000-000000000041 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF 2091"
      | id        | 00000000-0000-0000-0000-000000000042 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    When je pointe sur "OF 2091"
      | id        | 00000000-0000-0000-0000-000000000042 |
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And le suivi a 1 activites en cours

  Scenario: Un debut pointe tardivement au milieu d'une activite deja terminee contredit sa fin
    # Le debut rattrape a 10 h ouvre une activite, donc remplace a son heure celle de 8 h : la fin de 12 h, qui la vise,
    # la dirait terminee apres son remplacement. Le pointage est admis, et la sequence est en conflit plutot que d'etre
    # lue selon une interpretation choisie par le serveur.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2092"
      | categorie | OF   |
      | reference | 2092 |
    And j'ai engage l'element "OF 2092" en atelier
    And j'ai pointe sur "OF 2092"
      | id        | 00000000-0000-0000-0000-000000000261 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe sur "OF 2092"
      | id        | 00000000-0000-0000-0000-000000000262 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000261 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-11T09:15:00Z"
    When je pointe sur "OF 2092"
      | type           | DEBUT                |
      | intention      | OUVERTURE            |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T10:00:00Z |
    Then la reponse a le statut http 201
    And le journal du suivi contient 3 evenements
    And le suivi a 0 activites en cours
    And le suivi a l'etat "INTERROMPU"

  Scenario: Arreter deux fois la meme activite met la sequence en conflit
    # Le double appui sur « arreter » n'est jamais refuse, mais il n'est plus absorbe : les deux fins visent la meme
    # activite, et la seconde la dit en cours apres que la premiere l'a terminee. Conservee, elle laisse l'activite a
    # resoudre.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2093"
      | categorie | OF   |
      | reference | 2093 |
    And j'ai engage l'element "OF 2093" en atelier
    And j'ai pointe sur "OF 2093"
      | id        | 00000000-0000-0000-0000-000000000271 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:00Z"
    And j'ai pointe sur "OF 2093"
      | id        | 00000000-0000-0000-0000-000000000272 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000271 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T10:00:02Z"
    When je pointe sur "OF 2093"
      | id        | 00000000-0000-0000-0000-000000000273 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000271 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 201
    And le journal du suivi contient 3 evenements
    And le suivi a l'etat "INTERROMPU"
    And le suivi porte une seule sequence en conflit, de "dupont" sur "fraiseuse-1"
      | activites | 00000000-0000-0000-0000-000000000271                                                                             |
      | pointages | 00000000-0000-0000-0000-000000000271, 00000000-0000-0000-0000-000000000272, 00000000-0000-0000-0000-000000000273 |

  Scenario: Cloturer un element, puis le rouvrir
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2006"
      | categorie | OF   |
      | reference | 2006 |
    And j'ai engage l'element "OF 2006" en atelier
    And j'ai pointe sur "OF 2006"
      | id        | 00000000-0000-0000-0000-000000000291 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T18:00:00Z"
    When je cloture "OF 2006" a l'instant present
    Then la reponse a le statut http 200
    And le suivi a l'etat "CLOTURE"
    # Un element cloture n'accepte plus qu'on y demarre : c'est la seule exception a la regle.
    When je pointe sur "OF 2006"
      | type      | DEBUT  |
      | operateur | dupont |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:suivi-d-atelier-cloture"
    # L'arreter apres la cloture, en revanche, ne change rien : la cloture l'a deja fait.
    Given il est "2026-05-10T18:30:00Z"
    When je pointe sur "OF 2006"
      | id        | 00000000-0000-0000-0000-000000000292 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000000291 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Then la reponse a le statut http 200
    And le suivi a l'etat "CLOTURE"
    And le journal du suivi contient 1 evenements
    # Rouvert, l'element retrouve l'activite que la cloture terminait : elle court jusqu'a son echeance.
    When je rouvre "OF 2006"
    Then la reponse a le statut http 200
    And le suivi a l'etat "EN_COURS"

  Scenario: Une saisie oubliee est rattrapee a l'heure ou elle a eu lieu
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2007"
      | categorie | OF   |
      | reference | 2007 |
    And j'ai engage l'element "OF 2007" en atelier
    Given il est "2026-05-11T09:00:00Z"
    When je pointe sur "OF 2007"
      | type           | DEBUT                |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T09:00:00Z |
    Then la reponse a le statut http 201
    # Rattrape le lendemain, le debut de 9 h a deja atteint son echeance de 22 h : l'activite est terminee
    # automatiquement, et plus personne n'est sur l'element.
    And le suivi a l'etat "INTERROMPU"

  Scenario: Une regularisation saisie a l'heure du fait reste une regularisation
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2022"
      | categorie | OF   |
      | reference | 2022 |
    And j'ai engage l'element "OF 2022" en atelier
    Given il est "2026-05-10T09:00:00Z"
    And j'ai pointe sur "OF 2022"
      | id        | 00000000-0000-0000-0000-000000002022 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-10T22:00:00Z"
    When je regularise sur "OF 2022"
      | activite       | 00000000-0000-0000-0000-000000002022 |
      | dateDeSurvenue | 2026-05-10T22:00:00Z                 |
    Then la reponse a le statut http 201
    And l'evenement 1 du suivi a survenu a "2026-05-10T22:00:00Z" et a ete saisi a "2026-05-10T22:00:00Z" par "gestionnaire"
    And l'evenement 1 du suivi est une regularisation de "dupont" saisie par "gestionnaire"

  Scenario: Consulter un suivi inexistant renvoie 404
    When je consulte le suivi inconnu "7a4e2c91-6b83-4d05-9e17-f204a6b8c1d3"
    Then la reponse a le statut http 404

  Scenario: Un pointage anterieur a l'engagement est refuse
    # Un element ne peut pas avoir ete travaille avant d'avoir ete mis en atelier.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2016"
      | categorie | OF   |
      | reference | 2016 |
    And j'ai engage l'element "OF 2016" en atelier
    Given il est "2026-05-10T09:00:00Z"
    When je pointe sur "OF 2016"
      | type           | DEBUT                |
      | operateur      | dupont               |
      | dateDeSurvenue | 2026-05-10T06:00:00Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:evenement-anterieur-a-l-engagement"

  Scenario: Un poste sur lequel du temps a ete pointe ne se supprime plus, meme sans habilitation restante
    # Le journal ne retient que l'identifiant du poste : le supprimer laisserait des heures de travail sans machine.
    # Poste et operateur dedies a ce scenario : "fraiseuse-1" est partage par toute la campagne, une habilitation
    # laissee par un scenario precedent y masquerait ce refus-la derriere celui, revocable, du poste encore habilite.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a declare le poste de travail "fraiseuse-solo" de nature "fraisage"
    And l'entreprise a declare l'operateur "solo" habilite sur "fraiseuse-solo"
    And l'entreprise a cree l'element de fabrication "OF 2020"
      | categorie | OF   |
      | reference | 2020 |
    And j'ai engage l'element "OF 2020" en atelier
    And j'ai pointe sur "OF 2020"
      | type      | DEBUT          |
      | operateur | solo           |
      | poste     | fraiseuse-solo |
    And l'operateur "solo" n'est plus habilite sur "fraiseuse-solo"
    When je tente de supprimer le poste de travail declare "fraiseuse-solo"
    Then la reponse a le statut http 409

  Scenario: Un operateur qui a pointe sur un element ne se supprime plus
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2021"
      | categorie | OF   |
      | reference | 2021 |
    And j'ai engage l'element "OF 2021" en atelier
    And j'ai pointe sur "OF 2021"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When je tente de supprimer l'operateur declare "dupont"
    Then la reponse a le statut http 409

  Scenario: Un operateur qui n'a jamais pointe se supprime
    When je tente de supprimer l'operateur declare "martin"
    Then la reponse a le statut http 204

  Scenario: Le tableau d'atelier se filtre par etat
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2010"
      | categorie | OF   |
      | reference | 2010 |
    And j'ai engage l'element "OF 2010" en atelier
    When je liste les elements engages
    Then la reponse a le statut http 200
    And la liste des elements engages contient au moins 1 elements
    When je liste les elements engages dans l'etat "CLOTURE"
    Then la reponse a le statut http 200
    # La periode, quand elle est fournie, porte sur la date d'engagement.
    When je liste les elements engages entre "2026-05-10T00:00:00Z" et "2026-05-10T23:59:59Z"
    Then la reponse a le statut http 200
    And la liste des elements engages contient au moins 1 elements
    When je liste les elements engages entre "2020-01-01T00:00:00Z" et "2020-12-31T23:59:59Z"
    Then la liste des elements engages contient 0 elements
    # Une borne seule ne fait pas une periode : le filtre est alors ignore plutot que devine.
    When je liste les elements engages depuis "2026-05-10T00:00:00Z" sans borne de fin
    Then la reponse a le statut http 200
    And la liste des elements engages contient au moins 1 elements

  Scenario: Un element dont l'activite atteint son echeance n'est plus en cours au tableau d'atelier
    # Le filtre juge l'etat a l'instant de la lecture : sans aucune ecriture, l'element passe d'en cours a interrompu
    # quand son activite atteint son echeance, 13 h apres son debut.
    Given il est "2026-04-06T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2023"
      | categorie | OF   |
      | reference | 2023 |
    And j'ai engage l'element "OF 2023" en atelier
    And il est "2026-04-06T08:00:00Z"
    And j'ai pointe sur "OF 2023"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-04-06T20:59:00Z"
    When je liste les elements engages dans l'etat "EN_COURS" entre "2026-04-06T00:00:00Z" et "2026-04-06T23:59:59Z"
    Then la liste des elements engages contient "OF 2023"
    Given il est "2026-04-06T21:00:00Z"
    When je liste les elements engages dans l'etat "EN_COURS" entre "2026-04-06T00:00:00Z" et "2026-04-06T23:59:59Z"
    Then la liste des elements engages ne contient pas "OF 2023"
    When je liste les elements engages dans l'etat "INTERROMPU" entre "2026-04-06T00:00:00Z" et "2026-04-06T23:59:59Z"
    Then la liste des elements engages contient "OF 2023"

  Scenario: Un operateur peut pointer mais pas engager
    Given l'entreprise a cree l'element de fabrication "OF 2011"
      | categorie | OF   |
      | reference | 2011 |
    And j'ai engage l'element "OF 2011" en atelier
    Given I am logged in as "user" with role "USER"
    When je pointe sur "OF 2011"
      | type      | DEBUT  |
      | operateur | dupont |
    Then la reponse a le statut http 201
    When j'engage l'element "OF 2011" en atelier
    Then la reponse a le statut http 403

  Scenario: Les elements engages d'une entreprise ne sont pas visibles depuis une autre
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    And l'entreprise a cree l'element de fabrication "OF 2012"
      | categorie | OF   |
      | reference | 2012 |
    And j'ai engage l'element "OF 2012" en atelier
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte "OF 2012"
    Then la reponse a le statut http 404

  Scenario: Une journee d'atelier complete, deux machines menees de front
    # Le scenario de reference du contexte : Dupont demarre l'OF 42 sur la fraiseuse 1 a 8 h, l'OF 43 sur la
    # fraiseuse 2 a 9 h, les arrete tous deux a midi pour sa pause et les redemarre a 13 h, termine l'OF 43 a 16 h,
    # puis rentre chez lui a 17 h SANS ARRETER l'OF 42. Le lendemain, le gestionnaire regularise la fin oubliee.
    Given il est "2026-05-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 42"
      | categorie | OF   |
      | reference | 2042 |
    And l'entreprise a cree l'element de fabrication "OF 43"
      | categorie | OF   |
      | reference | 2043 |
    And j'ai engage l'element "OF 42" en atelier
    And j'ai engage l'element "OF 43" en atelier

    Given il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF 42"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |

    Given il est "2026-05-10T09:00:00Z"
    And j'ai pointe sur "OF 43"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-2 |

    # « Pause / arret / reprise sont le meme mecanisme » : le pupitre pointe la pause de midi par une fin sur chaque
    # ordre en cours, puis un debut sur chacun a la reprise.
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe sur "OF 42"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    And j'ai pointe sur "OF 43"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-2 |

    Given il est "2026-05-10T13:00:00Z"
    And j'ai pointe sur "OF 42"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    And j'ai pointe sur "OF 43"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-2 |

    Given il est "2026-05-10T16:00:00Z"
    And j'ai pointe sur "OF 43"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-2 |

    # L'OF 42 n'a recu aucun pointage apres sa relance a 13 h : sa pause le scinde a midi, et rien ne le borne a
    # 17 h. Il se termine automatiquement a son echeance, 13 heures apres son debut, avec une anomalie.
    Given il est "2026-05-11T09:15:00Z"
    When je consulte le temps effectif de "OF 42"
    Then la reponse a le statut http 200
    And le temps effectif contient
      | poste.libelle | debut                | fin                  | finAutomatique |
      | fraiseuse-1   | 2026-05-10T08:00:00Z | 2026-05-10T12:00:00Z | false          |
      | fraiseuse-1   | 2026-05-10T13:00:00Z | 2026-05-11T02:00:00Z | true           |

    # L'OF 43 s'arrete a sa propre fin.
    When je consulte le temps effectif de "OF 43"
    And le temps effectif contient
      | poste.libelle | debut                | fin                  |
      | fraiseuse-2   | 2026-05-10T09:00:00Z | 2026-05-10T12:00:00Z |
      | fraiseuse-2   | 2026-05-10T13:00:00Z | 2026-05-10T16:00:00Z |

    # Le journal de chaque ordre porte la pause de midi : la fin et le debut que le pupitre y a pointes.
    When je consulte "OF 42"
    Then le journal du suivi ne contient que les types
      | DEBUT |
      | FIN   |
      | DEBUT |
    When je consulte "OF 43"
    Then le journal du suivi ne contient que les types
      | DEBUT |
      | FIN   |
      | DEBUT |
      | FIN   |

    # Le gestionnaire regularise la fin oubliee de l'OF 42 : la fin reelle remplace la fin automatique.
    When je regularise sur "OF 42" en visant l'activite de l'evenement 2
      | dateDeSurvenue | 2026-05-10T17:00:00Z |
    And je consulte le temps effectif de "OF 42"
    Then le temps effectif contient
      | poste.libelle | debut                | fin                  | finAutomatique |
      | fraiseuse-1   | 2026-05-10T08:00:00Z | 2026-05-10T12:00:00Z | false          |
      | fraiseuse-1   | 2026-05-10T13:00:00Z | 2026-05-10T17:00:00Z | false          |

  Scenario: Un ordre reste en cours la veille est relance le lendemain
    # Dupont oublie d'arreter l'OF 44 ; sa relance du lendemain ouvre une nouvelle activite apres l'echeance.
    Given il est "2026-05-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 44"
      | categorie | OF   |
      | reference | 2044 |
    And j'ai engage l'element "OF 44" en atelier
    Given il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF 44"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe sur "OF 44"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T13:00:00Z"
    And j'ai pointe sur "OF 44"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |

    Given il est "2026-05-11T07:05:00Z"
    When je pointe sur "OF 44"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 201
    And le suivi a l'etat "EN_COURS"
    And le suivi a 1 activites en cours
    Given il est "2026-05-11T10:00:00Z"
    And j'ai pointe sur "OF 44"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |

    # L'activite oubliee lundi s'est terminee automatiquement a son echeance, a 02:00 : la relance de mardi ne la
    # prolonge pas, et rien n'est compte de 02:00 a 07:05.
    When je consulte le temps effectif de "OF 44"
    Then le temps effectif contient
      | poste.libelle | debut                | fin                  | finAutomatique |
      | fraiseuse-1   | 2026-05-10T08:00:00Z | 2026-05-10T12:00:00Z | false          |
      | fraiseuse-1   | 2026-05-10T13:00:00Z | 2026-05-11T02:00:00Z | true           |
      | fraiseuse-1   | 2026-05-11T07:05:00Z | 2026-05-11T10:00:00Z | false          |

  Scenario: Un travail sans fin reste en cours avant son echeance
    Given il est "2026-05-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 49"
      | categorie | OF   |
      | reference | 2049 |
    And j'ai engage l'element "OF 49" en atelier
    Given il est "2026-05-10T08:00:00Z"
    And j'ai pointe sur "OF 49"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T19:00:00Z"
    When je consulte le temps effectif de "OF 49"
    Then le temps effectif contient
      | poste.libelle | debut                | finAutomatique |
      | fraiseuse-1   | 2026-05-10T08:00:00Z | false          |
    And le temps effectif ne contient aucun intervalle ferme

  Scenario: Releve d'un intervalle 08:00-10:00, sans prise de poste
    # L'activite compte de son debut pointe a sa fin pointee.
    Given il est "2026-05-12T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 50"
      | categorie | OF   |
      | reference | 2050 |
    And j'ai engage l'element "OF 50" en atelier
    Given il est "2026-05-12T08:00:00Z"
    And j'ai pointe sur "OF 50"
      | id        | 00000000-0000-0000-0000-0000000005f2 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    Given il est "2026-05-12T10:00:00Z"
    And j'ai pointe sur "OF 50"
      | id        | 00000000-0000-0000-0000-0000000005f3 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-0000000005f2 |
      | operateur | dupont                               |
      | poste     | fraiseuse-1                          |
    When je consulte le temps effectif de "OF 50"
    Then le temps effectif contient
      | activite                             | debut                | fin                  | finAutomatique |
      | 00000000-0000-0000-0000-0000000005f2 | 2026-05-12T08:00:00Z | 2026-05-12T10:00:00Z | false          |

  Scenario: Les tarifs historiques sont reserves au gestionnaire meme apres un pointage du pupitre
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2981"
      | categorie | OF   |
      | reference | 2981 |
    And j'ai engage l'element "OF 2981" en atelier
    Given I am logged in as "user" with role "USER"
    When je pointe sur "OF 2981"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 201
    And le journal du suivi contient 1 evenements
    And l'evenement 0 du suivi porte l'operateur "dupont" et le poste "fraiseuse-1"
    And la reponse ne contient aucun tarif horaire
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And la reponse ne contient aucun tarif horaire
    When je consulte "OF 2981"
    Then la reponse a le statut http 200
    And la reponse ne contient aucun tarif horaire
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    When je consulte "OF 2981"
    Then la reponse a le statut http 200
    And l'evenement 0 du suivi a le cout horaire "45.5"
    And l'evenement 0 du suivi a le taux horaire "22.5"
