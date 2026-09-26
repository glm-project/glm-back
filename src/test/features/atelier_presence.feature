Feature: Presence des operateurs en atelier

  # La presence est le socle du temps passe : elle est saisie une seule fois, dans la journee de travail de
  # l'operateur, et jamais recopiee dans le journal des elements sur lesquels il travaille.
  #
  # Une journee de travail n'est pas un jour calendaire : c'est une venue, bornee par une arrivee et un depart.
  # L'operateur est designe par son identifiant dans le referentiel : la presence sert de socle a la paie, elle ne
  # peut pas reposer sur une chaine saisie. Aucun poste n'y intervient, donc aucune habilitation.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare l'operateur "dupont"

  Scenario: Une arrivee ouvre la journee de travail
    Given il est "2026-05-10T07:00:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 201
    And la journee a l'etat "PRESENT"
    And la journee n'a pas d'amplitude

  Scenario: L'arrivee du pupitre conserve son identifiant et son heure de geste
    Given il est "2026-05-10T14:00:00Z"
    When j'arrive
      | id             | 00000000-0000-0000-0000-000000000021 |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T08:00:00Z                 |
    Then la reponse a le statut http 201
    And l'evenement 0 de la journee a l'identifiant "00000000-0000-0000-0000-000000000021"
    And l'evenement 0 de la journee a survenu a "2026-05-10T08:00:00Z" et a ete saisi a "2026-05-10T14:00:00Z" par "gestionnaire"

  Scenario: Une arrivee identique rejouee ne cree pas un second evenement
    Given il est "2026-05-10T08:00:00Z"
    When j'arrive
      | id        | 00000000-0000-0000-0000-000000000031 |
      | operateur | dupont                               |
    Then la reponse a le statut http 201
    When j'arrive
      | id        | 00000000-0000-0000-0000-000000000031 |
      | operateur | dupont                               |
    Then la reponse a le statut http 200
    And le journal de la journee contient 1 evenements

  Scenario: Un UUID reutilise avec un autre contenu est mis en attente
    # Lot 8c : defaut du pupitre, jamais de l'operateur. Le geste est conserve sous un identifiant du serveur, et son
    # rejeu ne le double pas.
    Given l'entreprise a declare l'operateur "lambert"
    And il est "2026-05-10T08:00:00Z"
    When j'arrive
      | id        | 00000000-0000-0000-0000-000000000032 |
      | operateur | lambert                              |
    Then la reponse a le statut http 201
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000032 |
      | operateur | lambert                              |
      | type      | PAUSE                                |
    Then la reponse a le statut http 202
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000032 |
      | operateur | lambert                              |
      | type      | PAUSE                                |
    Then la reponse a le statut http 202
    When je consulte les pointages en attente de "lambert"
    Then il y a 1 pointages en attente
    And le pointage en attente 0 porte le motif "IDENTIFIANT_REUTILISE"
    And le pointage en attente 0 est un geste "PRESENCE" de type "PAUSE"

  Scenario: Une arrivee datee dans le futur est ramenee a sa reception et signalee
    # Lot 8b : l'horloge du pupitre avance. Le geste n'est pas refuse, il est ramene a sa reception, et le
    # gestionnaire voit l'ecart. Rejoue a l'identique, il rend la meme journee.
    Given il est "2026-05-10T08:00:00Z"
    When j'arrive
      | id             | 00000000-0000-0000-0000-000000000033 |
      | operateur      | dupont                               |
      | dateDeSurvenue | 2026-05-10T09:00:00Z                 |
    Then la reponse a le statut http 201
    And l'evenement 0 de la journee a survenu a "2026-05-10T08:00:00Z" et a ete saisi a "2026-05-10T08:00:00Z" par "gestionnaire"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    When je consulte les pointages signales de "dupont"
    Then il y a 1 pointages signales
    And le pointage signale 0 porte le motif "DATE_FUTURE"
    And le pointage signale 0 a ete declare a "2026-05-10T09:00:00Z"

  Scenario: Une journee complete, de l'arrivee au depart, avec une pause de midi
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Given il est "2026-05-10T13:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont  |
      | type      | REPRISE |
    Given il est "2026-05-10T17:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    When je consulte ma journee
    Then la reponse a le statut http 200
    And la journee a l'etat "ABSENT"
    # « Les heures de presence, c'est les heures ou il arrive a la societe, il pointe et il part. »
    And la journee a l'amplitude de "2026-05-10T07:00:00Z" a "2026-05-10T17:00:00Z"
    # La pause n'ote que son propre creux : elle scinde la presence en deux fenetres.
    And les fenetres de presence sont
      | debut                | fin                  |
      | 2026-05-10T07:00:00Z | 2026-05-10T12:00:00Z |
      | 2026-05-10T13:00:00Z | 2026-05-10T17:00:00Z |

  Scenario: Le pointage de presence du pupitre conserve son identifiant et son heure de geste
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T14:00:00Z"
    When je pointe ma presence
      | id             | 00000000-0000-0000-0000-000000000022 |
      | operateur      | dupont                               |
      | type           | PAUSE                                |
      | dateDeSurvenue | 2026-05-10T12:00:00Z                 |
    Then la reponse a le statut http 201
    And l'evenement 1 de la journee a l'identifiant "00000000-0000-0000-0000-000000000022"
    And l'evenement 1 de la journee a survenu a "2026-05-10T12:00:00Z" et a ete saisi a "2026-05-10T14:00:00Z" par "gestionnaire"

  Scenario: Un pointage de presence identique est rejoue sur sa journee d'origine
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T08:00:00Z"
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000034 |
      | operateur | dupont                               |
      | type      | PAUSE                                |
    Then la reponse a le statut http 201
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000034 |
      | operateur | dupont                               |
      | type      | PAUSE                                |
    Then la reponse a le statut http 200
    And le journal de la journee contient 2 evenements

  Scenario: Une arrivee redondante est absorbee dans la journee en cours
    # D4 : sous le seuil, l'operateur est deja la. Rien n'est ajoute, et le pupitre recoit la journee en cours.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "matin"
    Given il est "2026-05-10T09:00:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"
    And le journal de la journee contient 1 evenements
    # Le geste absorbe reste reserve : rejoue, il rend la meme journee.
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"
    And le journal de la journee contient 1 evenements

  Scenario: Le poste de nuit se reidentifie a 3 h sans ouvrir de seconde journee
    # E1 de la strategie « bornes de fin de journee ».
    Given il est "2026-05-10T20:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "nuit"
    Given il est "2026-05-11T03:00:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 200
    And la reponse designe la journee "nuit"
    Given il est "2026-05-11T08:00:00Z"
    When je pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    Then la reponse a le statut http 201
    And la reponse designe la journee "nuit"
    And la journee a l'amplitude de "2026-05-10T20:00:00Z" a "2026-05-11T08:00:00Z"

  Scenario: Une arrivee au seuil pile reste dans la journee
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-10T20:00:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 200
    And la reponse designe la journee "lundi"

  Scenario: Le lendemain d'un depart oublie, l'arrivee ouvre une nouvelle journee
    # E2 : lundi, Dupont part sans rien pointer. Mardi, son arrivee n'est plus perdue.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-11T07:00:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 201
    And la reponse ne designe pas la journee "lundi"
    And je retiens la journee sous le nom "mardi"
    And la journee a l'etat "PRESENT"
    Given il est "2026-05-11T12:00:00Z"
    When je pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Then la reponse a le statut http 201
    And la reponse designe la journee "mardi"
    # La journee de lundi reste telle quelle : sans depart, en attente de regularisation.
    When je consulte la journee "lundi"
    Then la journee a l'etat "PRESENT"
    And le journal de la journee contient 1 evenements

  Scenario: Un depart recu d'un pupitre en retard ouvre et ferme une journee sans rien refuser
    # E5 : le pupitre hors ligne depuis lundi envoie un depart mardi a 08:30. L'arrivee implicite porte une identite
    # du serveur, jamais celle du geste.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-11T09:00:00Z"
    When je pointe ma presence
      | id             | 00000000-0000-0000-0000-000000000051 |
      | operateur      | dupont                               |
      | type           | DEPART                               |
      | dateDeSurvenue | 2026-05-11T08:30:00Z                 |
    Then la reponse a le statut http 201
    And la reponse ne designe pas la journee "lundi"
    And je retiens la journee sous le nom "mardi"
    And la journee a l'etat "ABSENT"
    And la journee a l'amplitude de "2026-05-11T08:30:00Z" a "2026-05-11T08:30:00Z"
    And le journal du suivi ne contient que les types
      | ARRIVEE |
      | DEPART  |
    And l'evenement 0 de la journee n'a pas l'identifiant "00000000-0000-0000-0000-000000000051"
    And l'evenement 1 de la journee a l'identifiant "00000000-0000-0000-0000-000000000051"
    # Rejoue, le meme geste rend la meme journee : pas de troisieme journee.
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And la reponse designe la journee "mardi"

  Scenario: Une pause tardive ouvre une journee qui commence en pause
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T08:30:00Z"
    When je pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Then la reponse a le statut http 201
    And la journee a l'etat "EN_PAUSE"
    And le journal du suivi ne contient que les types
      | ARRIVEE |
      | PAUSE   |

  Scenario: Une reprise tardive n'ouvre qu'une arrivee
    # Une reprise suppose une pause : sur une journee abandonnee, seule l'arrivee a un sens.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Given il est "2026-05-11T08:30:00Z"
    When je pointe ma presence
      | operateur | dupont  |
      | type      | REPRISE |
    Then la reponse a le statut http 201
    And la journee a l'etat "PRESENT"
    And le journal du suivi ne contient que les types
      | ARRIVEE |

  Scenario: Un geste rejoue hors ligne est juge a son heure, pas a sa reception
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-11T09:00:00Z"
    When je pointe ma presence
      | operateur      | dupont               |
      | type           | PAUSE                |
      | dateDeSurvenue | 2026-05-10T12:00:00Z |
    Then la reponse a le statut http 201
    And la reponse designe la journee "lundi"
    And la journee a l'etat "EN_PAUSE"

  @parametrage
  Scenario: Le seuil fixe par le gestionnaire vaut pour les gestes suivants
    # E8 : le seuil passe a 10 h. Une arrivee a 17:30 ouvre une nouvelle journee la ou 13 h l'aurait absorbee.
    Given il est "2026-05-10T06:00:00Z"
    And je fixe l'amplitude maximale a "PT10H"
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "matin"
    Given il est "2026-05-10T16:59:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"
    Given il est "2026-05-10T17:30:00Z"
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 201
    And la reponse ne designe pas la journee "matin"

  Scenario: Regulariser le depart oublie de la veille est accepte avant la journee suivante
    # E6 : mardi est ouvert a 07:00. Le depart de lundi a 17:00 ne touche pas mardi.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-11T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T09:15:00Z"
    When je regularise la journee "lundi"
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T17:00:00Z |
    Then la reponse a le statut http 201
    And la journee a l'amplitude de "2026-05-10T07:00:00Z" a "2026-05-10T17:00:00Z"

  Scenario: Une regularisation qui ferait chevaucher deux journees est refusee au gestionnaire
    # E6 : le depart de lundi saisi a mardi 08:00 recouvrirait la journee de mardi.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-11T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T09:15:00Z"
    When je regularise la journee "lundi"
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-11T08:00:00Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:chevauchement-de-journees"
    When je consulte la journee "lundi"
    Then la journee a l'etat "PRESENT"
    And le journal de la journee contient 1 evenements

  Scenario: Une correction qui ferait chevaucher deux journees est refusee au gestionnaire
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "lundi"
    Given il est "2026-05-10T17:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    Given il est "2026-05-11T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T09:15:00Z"
    When je corrige l'evenement 1 de la journee "lundi"
      | motif          | Depart saisi a tort  |
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-11T08:00:00Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:chevauchement-de-journees"

  Scenario: Une pause sans journee ouverte en ouvre une qui commence en pause
    # Lot 8a : un geste sans journee n'est jamais refuse a l'operateur. Une arrivee implicite, sous une identite du
    # serveur, l'ouvre a l'heure du geste.
    Given il est "2026-05-10T12:00:00Z"
    When je pointe ma presence
      | id        | 00000000-0000-0000-0000-000000000061 |
      | operateur | dupont                               |
      | type      | PAUSE                                |
    Then la reponse a le statut http 201
    And la journee a l'etat "EN_PAUSE"
    And le journal du suivi ne contient que les types
      | ARRIVEE |
      | PAUSE   |
    And l'evenement 0 de la journee n'a pas l'identifiant "00000000-0000-0000-0000-000000000061"
    And l'evenement 1 de la journee a l'identifiant "00000000-0000-0000-0000-000000000061"

  Scenario: Un depart presse par un operateur jamais arrive donne une journee de duree nulle
    Given il est "2026-05-10T17:00:00Z"
    When je pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    Then la reponse a le statut http 201
    And la journee a l'etat "ABSENT"
    And la journee a l'amplitude de "2026-05-10T17:00:00Z" a "2026-05-10T17:00:00Z"

  Scenario: Une pause pour un operateur inconnu est mise en attente
    # Lot 8c : le geste ne se rattache a personne. Le pupitre recoit un succes, le gestionnaire le voit.
    When je pointe ma presence
      | operateur | 5e3d1c08-7f42-4a96-b0e5-2c8d9a1b3f74 |
      | type      | PAUSE                                |
    Then la reponse a le statut http 202
    When je consulte les pointages en attente de "5e3d1c08-7f42-4a96-b0e5-2c8d9a1b3f74"
    Then il y a 1 pointages en attente
    And le pointage en attente 0 porte le motif "OPERATEUR_INCONNU"

  Scenario: Une arrivee d'un operateur inconnu est mise en attente, puis ecartee
    Given il est "2026-05-10T07:00:00Z"
    When j'arrive
      | operateur | 1d2c3b4a-5e6f-4a70-8b91-c2d3e4f5a6b7 |
    Then la reponse a le statut http 202
    When j'applique le pointage en attente 0 de "1d2c3b4a-5e6f-4a70-8b91-c2d3e4f5a6b7"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:operateur-introuvable"
    When je consulte les pointages en attente de "1d2c3b4a-5e6f-4a70-8b91-c2d3e4f5a6b7"
    Then il y a 1 pointages en attente
    When j'ecarte le pointage en attente deja lu pour "Badge d'un visiteur"
    Then la reponse a le statut http 200
    When j'ecarte le pointage en attente deja lu pour "Badge d'un visiteur"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:pointage-en-attente-deja-traite"
    When j'applique le pointage en attente deja lu
    Then la reponse a le statut http 409
    When je consulte les pointages en attente de "1d2c3b4a-5e6f-4a70-8b91-c2d3e4f5a6b7"
    Then il y a 0 pointages en attente

  Scenario: Un geste rejoue dans une journee deja fermee est mis en attente, puis applique
    # Deux pupitres, l'un hors ligne : la pause de 12:00 arrive apres le depart de 17:00. Appliquee, elle s'inscrit
    # dans la journee qui contient sa date, comme une regularisation.
    Given l'entreprise a declare l'operateur "garnier"
    And il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | garnier |
    And je retiens la journee sous le nom "garnier-lundi"
    Given il est "2026-05-10T17:00:00Z"
    And j'ai pointe ma presence
      | operateur | garnier |
      | type      | DEPART  |
    Given il est "2026-05-10T17:30:00Z"
    When je pointe ma presence
      | operateur      | garnier              |
      | type           | PAUSE                |
      | dateDeSurvenue | 2026-05-10T12:00:00Z |
    Then la reponse a le statut http 202
    When je consulte les pointages en attente de "garnier"
    Then il y a 1 pointages en attente
    And le pointage en attente 0 porte le motif "GESTE_HORS_SEQUENCE"
    When j'applique le pointage en attente 0 de "garnier"
    Then la reponse a le statut http 200
    When je consulte la journee "garnier-lundi"
    Then le journal de la journee contient 3 evenements
    When je consulte les pointages en attente de "garnier"
    Then il y a 0 pointages en attente

  Scenario: Ecarter un pointage en attente exige un motif
    When je pointe ma presence
      | operateur | 2e3f4a5b-6c7d-4e8f-9a0b-1c2d3e4f5a6b |
      | type      | DEPART                               |
    Then la reponse a le statut http 202
    When j'ecarte le pointage en attente 0 de "2e3f4a5b-6c7d-4e8f-9a0b-1c2d3e4f5a6b" sans motif
    Then la reponse a le statut http 400
    When je consulte les pointages en attente de "2e3f4a5b-6c7d-4e8f-9a0b-1c2d3e4f5a6b"
    Then il y a 1 pointages en attente

  Scenario: Appliquer un pointage en attente inconnu renvoie 404
    When j'applique le pointage en attente inconnu "8c6e4d13-8da5-4f27-b039-1426c8d0e3f5"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:pointage-en-attente-introuvable"

  Scenario: Un operateur ne consulte ni ne traite les pointages en attente
    Given I am logged in as "operateur" with role "USER"
    When je consulte les pointages en attente
    Then la reponse a le statut http 403
    When j'applique le pointage en attente inconnu "8c6e4d13-8da5-4f27-b039-1426c8d0e3f5"
    Then la reponse a le statut http 403

  Scenario: Un administrateur technique n'a pas acces aux pointages en attente
    Given I am logged in as "admin" with role "ADMIN"
    When je consulte les pointages en attente
    Then la reponse a le statut http 403

  Scenario: Un motif de mise en attente inconnu est refuse
    When je consulte les pointages en attente de motif "INCONNU"
    Then la reponse a le statut http 400

  Scenario: Une reprise alors que l'operateur est deja present est absorbee
    # Lot 8a : le geste ne change rien, il n'est pas refuse. Rejoue, il rend la meme journee.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    And je retiens la journee sous le nom "matin"
    Given il est "2026-05-10T09:00:00Z"
    When je pointe ma presence
      | operateur | dupont  |
      | type      | REPRISE |
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"
    And le journal de la journee contient 1 evenements
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    And la reponse designe la journee "matin"

  Scenario: Un double appui sur pause est absorbe
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Given il est "2026-05-10T12:00:02Z"
    When je pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Then la reponse a le statut http 200
    And la journee a l'etat "EN_PAUSE"
    And le journal de la journee contient 2 evenements

  Scenario: Un depart oublie, regularise le lendemain par un tiers
    # « Il a oublie de pointer le matin... mais il faut compter son temps de presence aussi » —
    # « il faut pas que ce soit lui » : la saisie garde la date de l'acte et celle de la saisie,
    # sous le nom du gestionnaire.
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-11T09:15:00Z"
    When je regularise ma journee
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T17:00:00Z |
    Then la reponse a le statut http 201
    When je consulte ma journee
    Then la journee a l'etat "ABSENT"
    And la journee a l'amplitude de "2026-05-10T07:00:00Z" a "2026-05-10T17:00:00Z"
    # L'horodatage est bitemporel : l'heure du fait et l'heure de la saisie sont conservees toutes les deux.
    And l'evenement 1 de la journee a survenu a "2026-05-10T17:00:00Z" et a ete saisi a "2026-05-11T09:15:00Z" par "gestionnaire"

  Scenario: Une saisie en trop est annulee, mais reste au journal
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    When j'annule l'evenement 1 de ma journee
      | motif | Pause pointee par erreur |
    Then la reponse a le statut http 200
    And la journee a l'etat "PRESENT"
    # Personne ne supprime un evenement : le repli l'ecarte, le journal le garde.
    And le journal de la journee contient 2 evenements
    And l'evenement 1 de la journee est annule avec le motif "Pause pointee par erreur"

  Scenario: Une heure fausse est corrigee en un seul acte
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    When je corrige l'evenement 1 de ma journee
      | motif          | Pause prise a 11h30  |
      | type           | PAUSE                |
      | dateDeSurvenue | 2026-05-10T11:30:00Z |
    Then la reponse a le statut http 200
    And la journee a l'etat "EN_PAUSE"
    # L'ancien evenement reste, annule, et le remplacant prend sa place a l'heure corrigee.
    And le journal de la journee contient 3 evenements
    And les fenetres de presence sont
      | debut                | fin                  |
      | 2026-05-10T07:00:00Z | 2026-05-10T11:30:00Z |

  Scenario: Consulter une journee inexistante renvoie 404
    When je consulte la journee inconnue "3f2c7b0a-4d5e-4f70-a132-c3d4e5f60718"
    Then la reponse a le statut http 404

  Scenario: Annuler un evenement de presence inexistant renvoie 404
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    When j'annule l'evenement inconnu "1d0a5f8e-2b3c-4d5e-8f10-a1b2c3d4e5f6" de ma journee
      | motif | Evenement inconnu |
    Then la reponse a le statut http 404

  Scenario: Annuler deux fois le meme evenement de presence est refuse
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T12:00:00Z"
    And j'ai pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    When j'annule l'evenement 1 de ma journee
      | motif | Pause pointee par erreur |
    Then la reponse a le statut http 200
    When j'annule l'evenement 1 de ma journee
      | motif | Deja annulee |
    Then la reponse a le statut http 409

  Scenario: L'historique de presence se filtre par operateur et par periode
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    When je liste les journees de "dupont"
    Then la reponse a le statut http 200
    And la liste des journees contient 1 journees
    When je liste les journees de "dupont" entre "2026-05-10T00:00:00Z" et "2026-05-10T23:59:59Z"
    Then la reponse a le statut http 200
    And la liste des journees contient 1 journees
    # Hors de la periode, la journee ne ressort plus : la borne porte sur l'heure d'arrivee.
    When je liste les journees de "dupont" entre "2026-05-11T00:00:00Z" et "2026-05-11T23:59:59Z"
    Then la liste des journees contient 0 journees
    # Une borne seule ne fait pas une periode : le filtre est alors ignore plutot que devine.
    When je liste les journees de "dupont" depuis "2026-05-10T00:00:00Z" sans borne de fin
    Then la reponse a le statut http 200
    And la liste des journees contient 1 journees

  Scenario: Un operateur peut pointer sa presence mais pas la regulariser
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T17:00:00Z"
    And I am logged in as "user" with role "USER"
    When je pointe ma presence
      | operateur | dupont |
      | type      | DEPART |
    Then la reponse a le statut http 201
    When je regularise ma journee
      | type           | ARRIVEE              |
      | dateDeSurvenue | 2026-05-10T06:00:00Z |
    Then la reponse a le statut http 403

  Scenario: Un administrateur technique n'a pas acces aux actes de gestion
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given I am logged in as "admin" with role "ADMIN"
    When je regularise ma journee
      | type           | DEPART               |
      | dateDeSurvenue | 2026-05-10T17:00:00Z |
    Then la reponse a le statut http 403

  Scenario: Les journees d'une entreprise ne sont pas visibles depuis une autre
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "impeccmold"
    And il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je consulte ma journee
    Then la reponse a le statut http 404

  Scenario: Acces refuse a un utilisateur sans entreprise
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" without tenant
    When je liste les journees
    Then la reponse a le statut http 403
