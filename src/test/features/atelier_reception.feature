@regle-de-reception
Feature: Regle de reception des pointages

  # Le serveur juge chaque pointage a son arrivee, par cle (operateur, OF, poste), dans cet ordre : les controles
  # existants (operateur, poste, habilitation, cloture), puis ANTERIEUR (y compris une fin a l'heure du debut de
  # l'activite qu'elle fermerait), puis l'echeance, puis le tableau.
  #
  #   | Etat de la cle                         | DEBUT                  | NON_CONFORMITE         | FIN                                    |
  #   | Rien en cours (jamais ouvert, termine, | accepte                | accepte                | ignore : APRES_ECHEANCE si la derniere |
  #   | cloture ou echu)                       |                        |                        | activite est echue et sans fin, sinon  |
  #   |                                        |                        |                        | AUCUNE_ACTIVITE                        |
  #   | Activite en cours                      | ignore : DEJA_EN_COURS | ignore : DEJA_EN_COURS | accepte, termine l'activite            |
  #
  # Un pointage accepte repond 201. Un pointage ignore repond 409 pointage-ignore et laisse une ligne dans la table
  # d'audit, relue en base faute d'endpoint. J1 est le 3 mars 2046 et J2 le 4 mars ; l'echeance d'une activite est son
  # debut plus 13 heures.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-reception" de nature "fraisage"
    And l'entreprise a declare le poste de travail "rectifieuse-reception" de nature "rectification"
    And l'entreprise a declare l'operateur "paul-reception" habilite sur "fraiseuse-reception" et "rectifieuse-reception"

  Scenario: Situation 1, journee normale : demarrage, passage en NC, fin de NC, arret
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 normale"
      | categorie | OF      |
      | reference | REC1042 |
    And j'ai engage l'element "OF 1042 normale" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 normale" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 normale" au poste "fraiseuse-reception" a "2046-03-03T10:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe NON_CONFORMITE sur "OF 1042 normale" au poste "fraiseuse-reception" a "2046-03-03T10:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 normale" au poste "fraiseuse-reception" a "2046-03-03T10:40:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 normale" au poste "fraiseuse-reception" a "2046-03-03T10:40:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 normale" au poste "fraiseuse-reception" a "2046-03-03T12:00:00Z"
    Then le pointage est accepte
    When je consulte "OF 1042 normale"
    Then le journal du suivi ne contient que les types
      | DEBUT          |
      | FIN            |
      | NON_CONFORMITE |
      | FIN            |
      | DEBUT          |
      | FIN            |
    And le suivi a l'etat "INTERROMPU"
    And le suivi a 0 activites en cours
    # Trois activites (travail, NC, travail), rien pour le gestionnaire.
    And la table d'audit des pointages ignores de "OF 1042 normale" est vide
    When je consulte le dossier d'anomalie de "OF 1042 normale" depuis l'evenement 0
    Then la reponse a le statut http 404

  Scenario: Situation 2, double demarrage : le second est ignore, l'activite continue depuis 07:00
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 double debut"
      | categorie | OF       |
      | reference | REC1042B |
    And j'ai engage l'element "OF 1042 double debut" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 double debut" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 double debut" au poste "fraiseuse-reception" a "2046-03-03T07:01:00Z"
    Then le pointage est ignore
    And la reponse a le statut http 409
    When je consulte "OF 1042 double debut"
    Then le journal du suivi contient 1 evenements
    And le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "TRAVAIL" depuis "2046-03-03T07:00:00Z"
    And la table d'audit des pointages ignores de "OF 1042 double debut" contient
      | type  | raison        | dateDeSurvenue       | dernierAccepte |
      | DEBUT | DEJA_EN_COURS | 2046-03-03T07:01:00Z | evenement 0    |

  Scenario: Une non conformite pendant une activite en cours est ignoree comme un demarrage
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 nc pendant nc"
      | categorie | OF       |
      | reference | REC1042C |
    And j'ai engage l'element "OF 1042 nc pendant nc" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe NON_CONFORMITE sur "OF 1042 nc pendant nc" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe NON_CONFORMITE sur "OF 1042 nc pendant nc" au poste "fraiseuse-reception" a "2046-03-03T07:05:00Z"
    Then le pointage est ignore
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 nc pendant nc" au poste "fraiseuse-reception" a "2046-03-03T07:06:00Z"
    Then le pointage est ignore
    When je consulte "OF 1042 nc pendant nc"
    Then le journal du suivi contient 1 evenements
    And l'activite en cours est de categorie "NON_CONFORMITE" depuis "2046-03-03T07:00:00Z"
    And la table d'audit des pointages ignores de "OF 1042 nc pendant nc" contient
      | type           | raison        | dateDeSurvenue       | dernierAccepte |
      | NON_CONFORMITE | DEJA_EN_COURS | 2046-03-03T07:05:00Z | evenement 0    |
      | DEBUT          | DEJA_EN_COURS | 2046-03-03T07:06:00Z | evenement 0    |

  Scenario: Situation 3, double arret : la seconde fin a la meme heure n'est pas anterieure, elle est sans activite
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 double arret"
      | categorie | OF       |
      | reference | REC1042D |
    And j'ai engage l'element "OF 1042 double arret" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 double arret" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 double arret" au poste "fraiseuse-reception" a "2046-03-03T12:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 double arret" au poste "fraiseuse-reception" a "2046-03-03T12:00:00Z"
    Then le pointage est ignore
    When je consulte "OF 1042 double arret"
    Then le journal du suivi contient 2 evenements
    And le suivi a l'etat "INTERROMPU"
    And la table d'audit des pointages ignores de "OF 1042 double arret" contient
      | type | raison          | dateDeSurvenue       | dernierAccepte |
      | FIN  | AUCUNE_ACTIVITE | 2046-03-03T12:00:00Z | evenement 1    |

  Scenario: Situation 4, arret oublie : le redemarrage du lendemain est accepte, la fin de J1 reste a regulariser
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 arret oublie"
      | categorie | OF       |
      | reference | REC1042E |
    And j'ai engage l'element "OF 1042 arret oublie" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 arret oublie" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 arret oublie" au poste "fraiseuse-reception" a "2046-03-04T07:00:00Z"
    Then le pointage est accepte
    When je consulte "OF 1042 arret oublie"
    Then le journal du suivi contient 2 evenements
    And le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "TRAVAIL" depuis "2046-03-04T07:00:00Z"
    And la table d'audit des pointages ignores de "OF 1042 arret oublie" est vide
    # L'activite de J1 est echue a 20:00 : c'est une fin automatique, que le gestionnaire regularise au plus tard au
    # debut de J2.
    When je consulte le dossier d'anomalie de "OF 1042 arret oublie" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2046-03-03T07:00:00Z | 2046-03-03T20:00:00Z | PT13H |
    And le dossier d'anomalie donne la borne de fin "2046-03-04T07:00:00Z"

  Scenario: Situation 5, arret apres l'echeance : la fin de 21 h 30 est ignoree, la fin automatique de 20 h reste a regulariser
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 arret tardif"
      | categorie | OF       |
      | reference | REC1042F |
    And j'ai engage l'element "OF 1042 arret tardif" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 arret tardif" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 arret tardif" au poste "fraiseuse-reception" a "2046-03-03T21:30:00Z"
    Then le pointage est ignore
    When je consulte "OF 1042 arret tardif"
    Then le journal du suivi contient 1 evenements
    And le suivi a l'etat "INTERROMPU"
    And le suivi a 0 activites en cours
    And la table d'audit des pointages ignores de "OF 1042 arret tardif" contient
      | type | raison         | dateDeSurvenue       | dernierAccepte |
      | FIN  | APRES_ECHEANCE | 2046-03-03T21:30:00Z | evenement 0    |
    When je consulte le dossier d'anomalie de "OF 1042 arret tardif" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne l'activite
      | evenement | debut                | fin                  | duree |
      | 0         | 2046-03-03T07:00:00Z | 2046-03-03T20:00:00Z | PT13H |
    And le dossier d'anomalie ne donne aucune borne de fin

  Scenario: Une fin pile a l'echeance n'est plus acceptee, une fin d'une seconde avant l'est
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 echeance"
      | categorie | OF       |
      | reference | REC1042G |
    And l'entreprise a cree l'element de fabrication "OF 1042 juste avant"
      | categorie | OF       |
      | reference | REC1042H |
    And j'ai engage l'element "OF 1042 echeance" en atelier
    And j'ai engage l'element "OF 1042 juste avant" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 echeance" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    And l'operateur "paul-reception" pointe FIN sur "OF 1042 echeance" au poste "fraiseuse-reception" a "2046-03-03T20:00:00Z"
    Then le pointage est ignore
    And la table d'audit des pointages ignores de "OF 1042 echeance" contient
      | type | raison         | dateDeSurvenue       |
      | FIN  | APRES_ECHEANCE | 2046-03-03T20:00:00Z |
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 juste avant" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    And l'operateur "paul-reception" pointe FIN sur "OF 1042 juste avant" au poste "fraiseuse-reception" a "2046-03-03T19:59:59Z"
    Then le pointage est accepte

  Scenario: Une seconde fin sur une activite echue et sans fin est encore apres l'echeance
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 deux fins tardives"
      | categorie | OF       |
      | reference | REC1042I |
    And j'ai engage l'element "OF 1042 deux fins tardives" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 deux fins tardives" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    And l'operateur "paul-reception" pointe FIN sur "OF 1042 deux fins tardives" au poste "fraiseuse-reception" a "2046-03-03T21:30:00Z"
    Then le pointage est ignore
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 deux fins tardives" au poste "fraiseuse-reception" a "2046-03-03T22:00:00Z"
    Then le pointage est ignore
    And la table d'audit des pointages ignores de "OF 1042 deux fins tardives" contient
      | type | raison         | dateDeSurvenue       |
      | FIN  | APRES_ECHEANCE | 2046-03-03T21:30:00Z |
      | FIN  | APRES_ECHEANCE | 2046-03-03T22:00:00Z |

  Scenario: Situation 6, arret hors ligne recu tard : l'echeance se juge sur l'heure du geste
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 hors ligne"
      | categorie | OF       |
      | reference | REC1042J |
    And j'ai engage l'element "OF 1042 hors ligne" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 hors ligne" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 hors ligne" au poste "fraiseuse-reception" a "2046-03-03T18:00:00Z" et le serveur le recoit a "2046-03-03T23:00:00Z"
    Then le pointage est accepte
    When je consulte "OF 1042 hors ligne"
    Then le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi a survenu a "2046-03-03T18:00:00Z" et a ete saisi a "2046-03-03T23:00:00Z" par "user"
    And le suivi a l'etat "INTERROMPU"
    And la table d'audit des pointages ignores de "OF 1042 hors ligne" est vide
    When je consulte le dossier d'anomalie de "OF 1042 hors ligne" depuis l'evenement 0
    Then la reponse a le statut http 404

  Scenario: Situation 7, pointage plus ancien : le premier arrive est servi, les deux pointages de 10 h sont anterieurs
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 plus ancien"
      | categorie | OF       |
      | reference | REC1042K |
    And j'ai engage l'element "OF 1042 plus ancien" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 plus ancien" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 plus ancien" au poste "fraiseuse-reception" a "2046-03-03T12:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 plus ancien" au poste "fraiseuse-reception" a "2046-03-03T10:00:00Z" et le serveur le recoit a "2046-03-03T12:05:00Z"
    Then le pointage est ignore
    When l'operateur "paul-reception" pointe NON_CONFORMITE sur "OF 1042 plus ancien" au poste "fraiseuse-reception" a "2046-03-03T10:00:00Z" et le serveur le recoit a "2046-03-03T12:05:00Z"
    Then le pointage est ignore
    When je consulte "OF 1042 plus ancien"
    Then le journal du suivi ne contient que les types
      | DEBUT |
      | FIN   |
    And l'evenement 1 du suivi a survenu a "2046-03-03T12:00:00Z" et a ete saisi a "2046-03-03T12:00:00Z" par "user"
    And le suivi a l'etat "INTERROMPU"
    And la table d'audit des pointages ignores de "OF 1042 plus ancien" contient
      | type           | raison    | dateDeSurvenue       | dateDeReception      | dernierAccepte |
      | FIN            | ANTERIEUR | 2046-03-03T10:00:00Z | 2046-03-03T12:05:00Z | evenement 1    |
      | NON_CONFORMITE | ANTERIEUR | 2046-03-03T10:00:00Z | 2046-03-03T12:05:00Z | evenement 1    |

  Scenario: Une fin a l'heure du debut de l'activite en cours est ignoree comme anterieure
    # Une activite de duree nulle n'existe pas : la fin n'est pas posterieure au debut qu'elle fermerait, l'activite
    # reste en cours. Les gestes composes restent acceptes (situations 1 et 8) : une fin a t ferme une activite ouverte
    # avant t.
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 duree nulle"
      | categorie | OF       |
      | reference | REC1042S |
    And j'ai engage l'element "OF 1042 duree nulle" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 duree nulle" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 duree nulle" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est ignore
    When je consulte "OF 1042 duree nulle"
    Then le journal du suivi contient 1 evenements
    And le suivi a l'etat "EN_COURS"
    And l'activite en cours est de categorie "TRAVAIL" depuis "2046-03-03T07:00:00Z"
    And la table d'audit des pointages ignores de "OF 1042 duree nulle" contient
      | type | raison    | dateDeSurvenue       | dernierAccepte |
      | FIN  | ANTERIEUR | 2046-03-03T07:00:00Z | evenement 0    |

  Scenario: Situation 8, NC sans travail : NC de 08 h a 09 h, puis travail jusqu'a 10 h
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 nc seule"
      | categorie | OF       |
      | reference | REC1042L |
    And j'ai engage l'element "OF 1042 nc seule" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe NON_CONFORMITE sur "OF 1042 nc seule" au poste "fraiseuse-reception" a "2046-03-03T08:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 nc seule" au poste "fraiseuse-reception" a "2046-03-03T09:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 nc seule" au poste "fraiseuse-reception" a "2046-03-03T09:00:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 nc seule" au poste "fraiseuse-reception" a "2046-03-03T10:00:00Z"
    Then le pointage est accepte
    When je consulte "OF 1042 nc seule"
    Then le journal du suivi ne contient que les types
      | NON_CONFORMITE |
      | FIN            |
      | DEBUT          |
      | FIN            |
    And le suivi a l'etat "INTERROMPU"
    And la table d'audit des pointages ignores de "OF 1042 nc seule" est vide

  Scenario: Une fin sur une cle qui n'a jamais ete ouverte n'a pas de dernier pointage accepte
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 jamais ouvert"
      | categorie | OF       |
      | reference | REC1042M |
    And j'ai engage l'element "OF 1042 jamais ouvert" en atelier
    And il est "2046-03-03T07:00:00Z"
    And I am logged in as "user" with role "USER"
    When je pointe sur "OF 1042 jamais ouvert"
      | id             | 00000000-0000-0000-0000-000000104201 |
      | type           | FIN                                  |
      | operateur      | paul-reception                       |
      | poste          | fraiseuse-reception                  |
      | dateDeSurvenue | 2046-03-03T07:00:00Z                 |
    Then le pointage est ignore
    # Toutes les colonnes de la table d'audit : l'identifiant du geste, qui, ou, quoi, quand, recu quand, pourquoi.
    And la table d'audit des pointages ignores de "OF 1042 jamais ouvert" contient
      | id                                   | operateur      | poste               | type | dateDeSurvenue       | dateDeReception      | raison          | dernierAccepte |
      | 00000000-0000-0000-0000-000000104201 | paul-reception | fraiseuse-reception | FIN  | 2046-03-03T07:00:00Z | 2046-03-03T07:00:00Z | AUCUNE_ACTIVITE |                |
    When je consulte "OF 1042 jamais ouvert"
    Then le suivi a l'etat "EN_ATTENTE"
    And le journal du suivi contient 0 evenements

  Scenario: Chaque cle est jugee a part : un autre poste, un autre OF
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 cle un"
      | categorie | OF       |
      | reference | REC1042N |
    And l'entreprise a cree l'element de fabrication "OF 1042 cle deux"
      | categorie | OF       |
      | reference | REC1042O |
    And j'ai engage l'element "OF 1042 cle un" en atelier
    And j'ai engage l'element "OF 1042 cle deux" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 cle un" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    # Un autre poste du meme OF, et le meme poste sur un autre OF, ne sont pas des activites en cours de la cle.
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 cle un" au poste "rectifieuse-reception" a "2046-03-03T07:10:00Z"
    Then le pointage est accepte
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 cle deux" au poste "fraiseuse-reception" a "2046-03-03T07:20:00Z"
    Then le pointage est accepte
    # Une fin ne ferme que l'activite de sa cle ; un pointage anterieur sur une autre cle n'est pas anterieur.
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 cle un" au poste "fraiseuse-reception" a "2046-03-03T07:15:00Z"
    Then le pointage est accepte
    When je consulte "OF 1042 cle un"
    Then le suivi a 1 activites en cours
    And les activites en cours sont
      | categorie | depuis               |
      | TRAVAIL   | 2046-03-03T07:10:00Z |
    And la table d'audit des pointages ignores de "OF 1042 cle un" est vide

  Scenario: Une regularisation compte parmi les pointages acceptes de la cle
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 regularise"
      | categorie | OF       |
      | reference | REC1042P |
    And j'ai engage l'element "OF 1042 regularise" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 regularise" au poste "fraiseuse-reception" a "2046-03-03T07:00:00Z"
    Then le pointage est accepte
    Given il est "2046-03-03T22:00:00Z"
    And I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    When je regularise sur "OF 1042 regularise" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2046-03-03T17:00:00Z |
    Then la reponse a le statut http 201
    Given I am logged in as "user" with role "USER"
    # Le gestionnaire a termine l'activite a 17 h : un demarrage de 16 h, recu apres, est anterieur a cette fin.
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 regularise" au poste "fraiseuse-reception" a "2046-03-03T16:00:00Z" et le serveur le recoit a "2046-03-03T22:30:00Z"
    Then le pointage est ignore
    # Et une fin apres une activite terminee par le gestionnaire ne trouve aucune activite, echue ou non.
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 regularise" au poste "fraiseuse-reception" a "2046-03-03T21:30:00Z" et le serveur le recoit a "2046-03-03T22:35:00Z"
    Then le pointage est ignore
    And la table d'audit des pointages ignores de "OF 1042 regularise" contient
      | type  | raison          | dateDeSurvenue       | dernierAccepte |
      | DEBUT | ANTERIEUR       | 2046-03-03T16:00:00Z | evenement 1    |
      | FIN   | AUCUNE_ACTIVITE | 2046-03-03T21:30:00Z | evenement 1    |

  Scenario: Un demarrage sur un OF cloture reste refuse et n'est pas audite, une fin apres la cloture est ignoree
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 cloture"
      | categorie | OF       |
      | reference | REC1042Q |
    And j'ai engage l'element "OF 1042 cloture" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 cloture" au poste "fraiseuse-reception" a "2046-03-03T08:00:00Z"
    Then le pointage est accepte
    Given il est "2046-03-03T13:00:00Z"
    And I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    When je cloture "OF 1042 cloture"
      | dateDeSurvenue | 2046-03-03T12:00:00Z |
    Then la reponse a le statut http 200
    Given I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 cloture" au poste "fraiseuse-reception" a "2046-03-03T13:00:00Z"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:suivi-d-atelier-cloture"
    When l'operateur "paul-reception" pointe NON_CONFORMITE sur "OF 1042 cloture" au poste "fraiseuse-reception" a "2046-03-03T13:01:00Z"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:suivi-d-atelier-cloture"
    # La cloture a ferme l'activite : une fin posterieure n'a plus rien a terminer.
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 cloture" au poste "fraiseuse-reception" a "2046-03-03T13:02:00Z"
    Then le pointage est ignore
    And la table d'audit des pointages ignores de "OF 1042 cloture" contient
      | type | raison          | dateDeSurvenue       | dernierAccepte |
      | FIN  | AUCUNE_ACTIVITE | 2046-03-03T13:02:00Z | evenement 0    |
    When je consulte "OF 1042 cloture"
    Then le suivi a l'etat "CLOTURE"
    And le journal du suivi contient 1 evenements

  Scenario: Une fin survenue avant la cloture et recue apres elle est acceptee a son heure
    Given il est "2046-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "OF 1042 fin avant cloture"
      | categorie | OF       |
      | reference | REC1042R |
    And j'ai engage l'element "OF 1042 fin avant cloture" en atelier
    And I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe DEBUT sur "OF 1042 fin avant cloture" au poste "fraiseuse-reception" a "2046-03-03T08:00:00Z"
    Then le pointage est accepte
    Given il est "2046-03-03T13:00:00Z"
    And I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    When je cloture "OF 1042 fin avant cloture"
      | dateDeSurvenue | 2046-03-03T12:00:00Z |
    Then la reponse a le statut http 200
    Given I am logged in as "user" with role "USER"
    When l'operateur "paul-reception" pointe FIN sur "OF 1042 fin avant cloture" au poste "fraiseuse-reception" a "2046-03-03T09:00:00Z" et le serveur le recoit a "2046-03-03T13:05:00Z"
    Then le pointage est accepte
    When je consulte "OF 1042 fin avant cloture"
    Then le journal du suivi contient 2 evenements
    And le suivi a l'etat "CLOTURE"
