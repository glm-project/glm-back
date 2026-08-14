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
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2001                 |
    When j'engage l'element "OF 2001" en atelier
    Then la reponse a le statut http 201
    # Rien n'a encore ete pointe : l'element attend son premier operateur.
    And le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 0 evenements

  Scenario: Un element deja engage ne peut pas l'etre deux fois
    Given l'entreprise a cree l'element de fabrication "OF 2002"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2002                 |
    And j'ai engage l'element "OF 2002" en atelier
    When j'engage l'element "OF 2002" en atelier
    Then la reponse a le statut http 409

  Scenario: Engager un element inexistant renvoie 404
    When j'engage l'element inconnu "8b5f3d02-7c94-4e16-af28-0315b7c9d2e4" en atelier
    Then la reponse a le statut http 404

  Scenario: Un debut de travail met l'element en cours
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2003"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2003                 |
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

  Scenario: Une non conformite interrompt l'element, une reprise se pointe comme un debut
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2004"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2004                 |
    And j'ai engage l'element "OF 2004" en atelier
    And j'ai pointe sur "OF 2004"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Given il est "2026-05-10T09:00:00Z"
    When je pointe sur "OF 2004"
      | type      | NON_CONFORMITE |
      | operateur | dupont         |
      | poste     | fraiseuse-1    |
    # L'element reste EN_COURS : une non conformite laisse l'activite ouverte, elle en change seulement la categorie,
    # car ce temps-la se compte aussi. INTERROMPU est reserve a l'element sur lequel plus personne ne travaille.
    Then le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "NON_CONFORMITE"
    # La reprise n'a pas de type propre : c'est un DEBUT, et la categorie atteinte suffit a la distinguer.
    Given il est "2026-05-10T10:00:00Z"
    When je pointe sur "OF 2004"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "TRAVAIL"

  Scenario: Un element sur lequel plus personne ne travaille est interrompu
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2013"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2013                 |
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
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2017                 |
    And j'ai engage l'element "OF 2017" en atelier
    When je pointe sur "OF 2017"
      | type      | DEBUT       |
      | operateur | martin      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 409

  Scenario: Pointer pour un operateur inconnu du referentiel renvoie 404
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2018"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2018                 |
    And j'ai engage l'element "OF 2018" en atelier
    When je pointe sur "OF 2018"
      | type      | DEBUT                                |
      | operateur | 9c1f4a67-0d38-4b52-8e91-6a7c5d4b3e20 |
    Then la reponse a le statut http 404

  Scenario: Pointer sur un poste inconnu du referentiel renvoie 404
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2019"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2019                 |
    And j'ai engage l'element "OF 2019" en atelier
    When je pointe sur "OF 2019"
      | type      | DEBUT                                |
      | operateur | dupont                               |
      | poste     | 4b8e2d31-95c0-4f76-a1d3-7e6b0c5a9f42 |
    Then la reponse a le statut http 404

  Scenario: Deux debuts consecutifs sur la meme activite sont refuses
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2005"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2005                 |
    And j'ai engage l'element "OF 2005" en atelier
    And j'ai pointe sur "OF 2005"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When je pointe sur "OF 2005"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    Then la reponse a le statut http 409

  Scenario: Cloturer un element, puis le rouvrir
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2006"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2006                 |
    And j'ai engage l'element "OF 2006" en atelier
    Given il est "2026-05-10T18:00:00Z"
    When je cloture "OF 2006" a l'instant present
    Then la reponse a le statut http 200
    And le suivi a l'etat "CLOTURE"
    # Un element cloture n'accepte plus de pointage.
    When je pointe sur "OF 2006"
      | type      | DEBUT  |
      | operateur | dupont |
    Then la reponse a le statut http 409
    When je rouvre "OF 2006"
    Then la reponse a le statut http 200
    And le suivi a l'etat "EN_ATTENTE"

  Scenario: Une saisie oubliee est rattrapee a l'heure ou elle a eu lieu
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2007"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2007                 |
    And j'ai engage l'element "OF 2007" en atelier
    Given il est "2026-05-11T09:00:00Z"
    When je regularise sur "OF 2007"
      | type           | DEBUT                |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T09:00:00Z |
    Then la reponse a le statut http 201
    And le suivi a l'etat "EN_COURS"
    When je consulte "OF 2007"
    # Une regularisation se reconnait a l'ecart de ses deux dates, jamais a l'identite de son auteur.
    Then l'evenement 0 du suivi est une regularisation de "dupont" saisie par "gestionnaire"

  Scenario: Une saisie en trop est annulee, mais reste au journal
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2008"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2008                 |
    And j'ai engage l'element "OF 2008" en atelier
    And j'ai pointe sur "OF 2008"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When j'annule l'evenement 0 de "OF 2008"
      | motif | Pointe sur le mauvais ordre |
    Then la reponse a le statut http 200
    And le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 1 evenements
    And l'evenement 0 du suivi est annule avec le motif "Pointe sur le mauvais ordre"

  Scenario: Une heure fausse est corrigee en un seul acte
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2009"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2009                 |
    And j'ai engage l'element "OF 2009" en atelier
    And j'ai pointe sur "OF 2009"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    # La correction est saisie apres coup : l'heure corrigee ne peut pas etre dans le futur de la saisie.
    Given il est "2026-05-10T09:00:00Z"
    When je corrige l'evenement 0 de "OF 2009"
      | motif          | Demarre a 8h30       |
      | type           | DEBUT                |
      | operateur      | dupont               |
      | poste          | fraiseuse-1          |
      | dateDeSurvenue | 2026-05-10T08:30:00Z |
    Then la reponse a le statut http 200
    And le suivi a l'etat "EN_COURS"
    And le journal du suivi contient 2 evenements

  Scenario: Consulter un suivi inexistant renvoie 404
    When je consulte le suivi inconnu "7a4e2c91-6b83-4d05-9e17-f204a6b8c1d3"
    Then la reponse a le statut http 404

  Scenario: Annuler un evenement d'atelier inexistant renvoie 404
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2014"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2014                 |
    And j'ai engage l'element "OF 2014" en atelier
    When j'annule l'evenement inconnu "2e1b6a9f-3c4d-4e6f-9021-b2c3d4e5f607" de "OF 2014"
      | motif | Evenement inconnu |
    Then la reponse a le statut http 404

  Scenario: Annuler deux fois le meme evenement d'atelier est refuse
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2015"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2015                 |
    And j'ai engage l'element "OF 2015" en atelier
    And j'ai pointe sur "OF 2015"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When j'annule l'evenement 0 de "OF 2015"
      | motif | Pointe sur le mauvais ordre |
    Then la reponse a le statut http 200
    When j'annule l'evenement 0 de "OF 2015"
      | motif | Deja annule |
    Then la reponse a le statut http 409

  Scenario: Une regularisation anterieure a l'engagement est refusee
    # Un element ne peut pas avoir ete travaille avant d'avoir ete mis en atelier.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2016"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2016                 |
    And j'ai engage l'element "OF 2016" en atelier
    Given il est "2026-05-10T09:00:00Z"
    When je regularise sur "OF 2016"
      | type           | DEBUT                |
      | operateur      | dupont               |
      | dateDeSurvenue | 2026-05-10T06:00:00Z |
    Then la reponse a le statut http 409

  Scenario: Un poste sur lequel du temps a ete pointe ne se supprime plus, meme sans habilitation restante
    # Le journal ne retient que l'identifiant du poste : le supprimer laisserait des heures de travail sans machine.
    # Poste et operateur dedies a ce scenario : "fraiseuse-1" est partage par toute la campagne, une habilitation
    # laissee par un scenario precedent y masquerait ce refus-la derriere celui, revocable, du poste encore habilite.
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a declare le poste de travail "fraiseuse-solo" de nature "fraisage"
    And l'entreprise a declare l'operateur "solo" habilite sur "fraiseuse-solo"
    And l'entreprise a cree l'element de fabrication "OF 2020"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2020                 |
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
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2021                 |
    And j'ai engage l'element "OF 2021" en atelier
    And j'ai pointe sur "OF 2021"
      | type      | DEBUT       |
      | operateur | dupont      |
      | poste     | fraiseuse-1 |
    When je tente de supprimer l'operateur declare "dupont"
    Then la reponse a le statut http 409

  Scenario: Un operateur qui n'a fait que pointer sa presence ne se supprime pas davantage
    # La presence seule suffit : c'est elle qui porte les heures a payer.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    When je tente de supprimer l'operateur declare "dupont"
    Then la reponse a le statut http 409

  Scenario: Un operateur qui n'a jamais pointe se supprime
    When je tente de supprimer l'operateur declare "martin"
    Then la reponse a le statut http 204

  Scenario: Le tableau d'atelier se filtre par etat
    Given il est "2026-05-10T08:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 2010"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2010                 |
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

  Scenario: Un operateur peut pointer mais pas engager
    Given l'entreprise a cree l'element de fabrication "OF 2011"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2011                 |
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
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2012                 |
    And j'ai engage l'element "OF 2012" en atelier
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte "OF 2012"
    Then la reponse a le statut http 404

  Scenario: Une journee d'atelier complete, deux machines menees de front
    # Le scenario de reference du contexte : Dupont arrive a 7 h, demarre l'OF 42 sur la fraiseuse 1 a 8 h,
    # l'OF 43 sur la fraiseuse 2 a 9 h, part en pause a midi, reprend a 13 h, termine l'OF 43 a 16 h, puis rentre
    # chez lui a 17 h SANS RIEN POINTER — ni son depart, ni la fin de l'OF 42. Le lendemain, le gestionnaire
    # regularise le depart oublie.
    Given il est "2026-05-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 42"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2042                 |
    And l'entreprise a cree l'element de fabrication "OF 43"
      | type      | ORDRE_DE_FABRICATION |
      | reference | 2043                 |
    And j'ai engage l'element "OF 42" en atelier
    And j'ai engage l'element "OF 43" en atelier
    And je suis arrive
      | operateur | dupont |

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

    # « Ne mettez qu'un bouton pas trois » : un seul geste de pause, jamais recopie dans les ordres en cours.
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |

    Given il est "2026-05-10T13:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont  |
      | type      | REPRISE |

    Given il est "2026-05-10T16:00:00Z"
    And j'ai pointe sur "OF 43"
      | type      | FIN         |
      | operateur | dupont      |
      | poste     | fraiseuse-2 |

    # Le lendemain, le gestionnaire rattrape le depart jamais pointe.
    Given il est "2026-05-11T09:15:00Z"
    And j'ai regularise ma journee
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T17:00:00Z |

    # L'OF 42 n'a recu aucun pointage apres son debut a 8 h : c'est la presence seule qui le scinde a midi
    # et le referme au depart regularise.
    When je consulte le temps effectif de "OF 42"
    Then la reponse a le statut http 200
    And le temps effectif contient
      | poste.libelle | debut                | fin                  |
      | fraiseuse-1   | 2026-05-10T08:00:00Z | 2026-05-10T12:00:00Z |
      | fraiseuse-1   | 2026-05-10T13:00:00Z | 2026-05-10T17:00:00Z |

    # L'OF 43 s'arrete a sa propre fin, sans attendre le depart.
    When je consulte le temps effectif de "OF 43"
    And le temps effectif contient
      | poste.libelle | debut                | fin                  |
      | fraiseuse-2   | 2026-05-10T09:00:00Z | 2026-05-10T12:00:00Z |
      | fraiseuse-2   | 2026-05-10T13:00:00Z | 2026-05-10T16:00:00Z |

    # Le journal de chaque ordre ne contient que ce que l'operateur y a pointe : jamais la pause.
    When je consulte "OF 42"
    Then le journal du suivi ne contient que les types
      | DEBUT |
    When je consulte "OF 43"
    Then le journal du suivi ne contient que les types
      | DEBUT |
      | FIN   |

    # Une seule regularisation de depart a suffi a refermer tout ce qui restait ouvert.
    When je consulte le temps effectif de "OF 42"
    Then le temps effectif ne contient aucun intervalle ouvert
