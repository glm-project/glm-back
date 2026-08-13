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

  Scenario: Un operateur ne peut pas ouvrir deux journees a la fois
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    When j'arrive
      | operateur | dupont |
    Then la reponse a le statut http 409

  Scenario: Pointer une pause sans journee ouverte est refuse
    When je pointe ma presence
      | operateur | dupont |
      | type      | PAUSE  |
    Then la reponse a le statut http 404

  Scenario: Ouvrir une journee pour un operateur inconnu du referentiel renvoie 404
    When j'arrive
      | operateur | 5e3d1c08-7f42-4a96-b0e5-2c8d9a1b3f74 |
    Then la reponse a le statut http 404

  Scenario: Une reprise sans pause est refusee
    Given il est "2026-05-10T07:00:00Z"
    And je suis arrive
      | operateur | dupont |
    Given il est "2026-05-10T09:00:00Z"
    When je pointe ma presence
      | operateur | dupont  |
      | type      | REPRISE |
    Then la reponse a le statut http 409

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
