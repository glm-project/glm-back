@dossier-fin-automatique
Feature: Dossier d'anomalie d'une fin automatique

  # Le dossier d'une fin automatique s'ouvre depuis l'ouvrant actif de l'activite echue. L'echeance se juge a
  # l'instant d'evaluation, a la nanoseconde, et n'est jamais stockee. Aucune heure n'est inventee : la regularisation
  # proposee n'en porte pas, une correction reprend celle du geste tardif qu'elle remplace.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-anomalie" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "rectifieuse-anomalie" de nature "rectification" et de cout horaire "60"
    And l'entreprise a declare l'operateur "dupont-anomalie" habilite sur "fraiseuse-anomalie" et "rectifieuse-anomalie" avec un taux horaire de "22"

  Scenario: Une activite oubliee ouvre un dossier de fin automatique avec une regularisation sans heure
    Given il est "2044-03-01T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4501"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4501              |
    And j'ai engage l'element "Anomalie 4501" en atelier
    And il est "2044-03-01T08:00:00Z"
    And j'ai pointe sur "Anomalie 4501"
      | id        | 00000000-0000-0000-0000-000000045011 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-01T22:00:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4501" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie signale une fin automatique
    And le dossier d'anomalie n'est pas en conflit
    And le dossier d'anomalie donne les activites
      | evenement | etat  | debut                | fin                  | duree |
      | 0         | ECHUE | 2044-03-01T08:00:00Z | 2044-03-01T21:00:00Z | PT13H |
    And le dossier d'anomalie propose
      | code            | kind           | pointage |
      | REGULARISER_FIN | REGULARISATION | 0        |
    And la proposition "REGULARISER_FIN" du dossier d'anomalie reprend l'ouvrant 0 sans aucune heure
    And le perimetre du dossier d'anomalie contient l'activite de l'evenement 0

  Scenario Outline: L'echeance se juge a l'instant de lecture, borne incluse, a la nanoseconde
    Given il est "2044-03-02T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4502 <rang>"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4502-<rang>       |
    And j'ai engage l'element "Anomalie 4502 <rang>" en atelier
    And il est "2044-03-02T08:00:00Z"
    And j'ai pointe sur "Anomalie 4502 <rang>"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "<lecture>"
    When je consulte le dossier d'anomalie de "Anomalie 4502 <rang>" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "<etat>"
    And le dossier d'anomalie declare finAutomatique "<finAutomatique>"
    And le dossier d'anomalie donne <activites> activites

    Examples:
      | rang | lecture                        | etat            | finAutomatique | activites |
      | 1    | 2044-03-02T20:59:00Z           | SANS_ANOMALIE   | false          | 0         |
      | 2    | 2044-03-02T20:59:59.999999999Z | SANS_ANOMALIE   | false          | 0         |
      | 3    | 2044-03-02T21:00:00Z           | FIN_AUTOMATIQUE | true           | 1         |

  Scenario: L'apercu d'une fin regularisee a 17 h ne modifie rien et passe l'activite de 13 h a 9 h
    Given il est "2044-03-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4503"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4503              |
    And j'ai engage l'element "Anomalie 4503" en atelier
    And il est "2044-03-03T08:00:00Z"
    And j'ai pointe sur "Anomalie 4503"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-03T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4503" depuis l'evenement 0
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-03T17:00:00Z"
    Then l'apercu du dossier d'anomalie est accepte
    And l'apercu du dossier d'anomalie donne les activites
      | moment | evenement | etat     | fin                  | duree |
      | avant  | 0         | ECHUE    | 2044-03-03T21:00:00Z | PT13H |
      | apres  | 0         | TERMINEE | 2044-03-03T17:00:00Z | PT9H  |
    And l'apercu du dossier d'anomalie ne modifie aucun fait

  Scenario: La confirmation d'une fin regularisee termine l'activite et sa reprise rend le meme recu
    Given il est "2044-03-04T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4504"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4504              |
    And j'ai engage l'element "Anomalie 4504" en atelier
    And il est "2044-03-04T08:00:00Z"
    And j'ai pointe sur "Anomalie 4504"
      | id        | 00000000-0000-0000-0000-000000045041 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-04T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4504" depuis l'evenement 0
    And je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-04T17:00:00Z"
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "SANS_ANOMALIE"
    And le dossier d'anomalie ne signale aucune fin automatique
    And le dossier d'anomalie donne les activites
      | evenement | etat     | debut                | fin                  | duree |
      | 0         | TERMINEE | 2044-03-04T08:00:00Z | 2044-03-04T17:00:00Z | PT9H  |
    And l'evenement cree par la confirmation est une fin regularisee a "2044-03-04T17:00:00Z"
    When je reprends la confirmation de l'apercu du dossier d'anomalie
    Then le recu de la reprise est identique et rien n'est ecrit une seconde fois
    When je consulte le temps effectif de "Anomalie 4504"
    Then le temps effectif contient
      | activite                             | debut                | fin                  | finAutomatique |
      | 00000000-0000-0000-0000-000000045041 | 2044-03-04T08:00:00Z | 2044-03-04T17:00:00Z | false          |

  Scenario: Une fin pointee a 23 h apres l'echeance se corrige a la meme heure et donne 15 h sans anomalie
    Given il est "2044-03-05T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4505"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4505              |
    And j'ai engage l'element "Anomalie 4505" en atelier
    And il est "2044-03-05T08:00:00Z"
    And j'ai pointe sur "Anomalie 4505"
      | id        | 00000000-0000-0000-0000-000000045051 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-05T23:00:00Z"
    And j'ai pointe sur "Anomalie 4505"
      | id        | 00000000-0000-0000-0000-000000045052 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000045051 |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-05T23:30:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4505" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie propose
      | code                 | kind       | pointage | instant              |
      | CORRIGER_FIN_TARDIVE | CORRECTION | 1        | 2044-03-05T23:00:00Z |
    When je previsualise la proposition "CORRIGER_FIN_TARDIVE" du dossier d'anomalie
    Then l'apercu du dossier d'anomalie est accepte
    And l'apercu du dossier d'anomalie donne les activites
      | moment | evenement | etat     | fin                  | duree |
      | avant  | 0         | ECHUE    | 2044-03-05T21:00:00Z | PT13H |
      | apres  | 0         | TERMINEE | 2044-03-05T23:00:00Z | PT15H |
    And l'apercu du dossier d'anomalie ne modifie aucun fait
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "SANS_ANOMALIE"
    And le dossier d'anomalie ne signale aucune fin automatique
    And le dossier d'anomalie n'est pas en conflit
    And l'evenement cree par la confirmation est une fin regularisee a "2044-03-05T23:00:00Z"

  Scenario: Deux fins tardives donnent une seule proposition, sur la plus tardive
    Given il est "2044-03-06T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4506"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4506              |
    And j'ai engage l'element "Anomalie 4506" en atelier
    And il est "2044-03-06T08:00:00Z"
    And j'ai pointe sur "Anomalie 4506"
      | id        | 00000000-0000-0000-0000-000000045061 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-06T22:00:00Z"
    And j'ai pointe sur "Anomalie 4506"
      | id        | 00000000-0000-0000-0000-000000045062 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000045061 |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-06T23:00:00Z"
    And j'ai pointe sur "Anomalie 4506"
      | id        | 00000000-0000-0000-0000-000000045063 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000045061 |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-06T23:30:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4506" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie propose
      | code                 | kind       | pointage | instant              |
      | CORRIGER_FIN_TARDIVE | CORRECTION | 2        | 2044-03-06T23:00:00Z |
    When je previsualise la proposition "CORRIGER_FIN_TARDIVE" du dossier d'anomalie
    Then l'apercu du dossier d'anomalie donne les activites
      | moment | evenement | etat     | fin                  | duree |
      | apres  | 0         | TERMINEE | 2044-03-06T23:00:00Z | PT15H |
    And l'apercu du dossier d'anomalie donne apres l'acte un dossier qui n'est pas en conflit
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "SANS_ANOMALIE"
    And le dossier d'anomalie ne signale aucune fin automatique

  Scenario: Une transition vers la non conformite pointee a 23 h se corrige sans proposer de fin regularisee
    Given il est "2044-03-07T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4507"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4507              |
    And j'ai engage l'element "Anomalie 4507" en atelier
    And il est "2044-03-07T08:00:00Z"
    And j'ai pointe sur "Anomalie 4507"
      | id        | 00000000-0000-0000-0000-000000045071 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-07T23:00:00Z"
    And j'ai pointe sur "Anomalie 4507"
      | id        | 00000000-0000-0000-0000-000000045072 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000045071 |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-07T23:30:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4507" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie propose
      | code                        | kind       | pointage | instant              |
      | CORRIGER_TRANSITION_TARDIVE | CORRECTION | 1        | 2044-03-07T23:00:00Z |
    And le dossier d'anomalie ne propose pas "REGULARISER_FIN"
    When je previsualise la proposition "CORRIGER_TRANSITION_TARDIVE" du dossier d'anomalie
    Then l'apercu du dossier d'anomalie donne les activites
      | moment | evenement | etat     | fin                  | duree |
      | avant  | 0         | ECHUE    | 2044-03-07T21:00:00Z | PT13H |
      | apres  | 0         | TERMINEE | 2044-03-07T23:00:00Z | PT15H |
      | apres  | 1         | EN_COURS |                      |       |
    And l'apercu du dossier d'anomalie donne apres l'acte un dossier qui n'est pas en conflit
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "SANS_ANOMALIE"
    And le dossier d'anomalie ne signale aucune fin automatique
    When je consulte le temps effectif de "Anomalie 4507"
    Then le temps effectif contient
      | activite                             | categorie      | debut                | fin                  | finAutomatique |
      | 00000000-0000-0000-0000-000000045071 | TRAVAIL        | 2044-03-07T08:00:00Z | 2044-03-07T23:00:00Z | false          |
      | 00000000-0000-0000-0000-000000045072 | NON_CONFORMITE | 2044-03-07T23:00:00Z |                      | false          |
    When je consulte "Anomalie 4507"
    Then le suivi n'a aucune sequence en conflit

  Scenario: Une transition tardive se corrige avant la fin tardive qui la suit, et le conflit qui en resulte est accepte
    Given il est "2044-03-30T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4530"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4530              |
    And j'ai engage l'element "Anomalie 4530" en atelier
    And il est "2044-03-30T08:00:00Z"
    And j'ai pointe sur "Anomalie 4530"
      | id        | 00000000-0000-0000-0000-000000045301 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-30T22:00:00Z"
    And j'ai pointe sur "Anomalie 4530"
      | id        | 00000000-0000-0000-0000-000000045302 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000045301 |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-30T23:00:00Z"
    And j'ai pointe sur "Anomalie 4530"
      | id        | 00000000-0000-0000-0000-000000045303 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000045301 |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-30T23:30:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4530" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie propose
      | code                        | kind       | pointage | instant              |
      | CORRIGER_TRANSITION_TARDIVE | CORRECTION | 1        | 2044-03-30T22:00:00Z |
    When je previsualise la proposition "CORRIGER_TRANSITION_TARDIVE" du dossier d'anomalie
    Then l'apercu du dossier d'anomalie est accepte
    And l'apercu du dossier d'anomalie donne apres l'acte un dossier a l'etat "EN_CONFLIT"
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "EN_CONFLIT"
    And le dossier d'anomalie est en conflit
    And le dossier d'anomalie ne signale aucune fin automatique

  Scenario Outline: Une fin regularisee avant la relance tardive reste sans conflit, apres elle le conflit est accepte
    Given il est "2044-03-08T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4508 <rang>"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4508-<rang>       |
    And j'ai engage l'element "Anomalie 4508 <rang>" en atelier
    And il est "2044-03-08T08:00:00Z"
    And j'ai pointe sur "Anomalie 4508 <rang>"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-08T23:00:00Z"
    And j'ai pointe sur "Anomalie 4508 <rang>"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-09T08:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4508 <rang>" depuis l'evenement 0
    And le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "<fin>"
    Then l'apercu du dossier d'anomalie est accepte
    And l'apercu du dossier d'anomalie donne apres l'acte un dossier a l'etat "<etat>"
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "<etat>"

    Examples:
      | rang | fin                  | etat          |
      | 1    | 2044-03-08T17:00:00Z | SANS_ANOMALIE |
      | 2    | 2044-03-08T23:30:00Z | EN_CONFLIT    |

  Scenario: Une fin regularisee avant le debut est acceptee et met la sequence en conflit
    Given il est "2044-03-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4510"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4510              |
    And j'ai engage l'element "Anomalie 4510" en atelier
    And il est "2044-03-10T08:00:00Z"
    And j'ai pointe sur "Anomalie 4510"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-10T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4510" depuis l'evenement 0
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-10T07:00:00Z"
    Then l'apercu du dossier d'anomalie est accepte
    And l'apercu du dossier d'anomalie donne apres l'acte un dossier a l'etat "EN_CONFLIT"
    And l'apercu du dossier d'anomalie donne apres l'acte la raison de conflit "GESTE_AVANT_OUVERTURE"
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "EN_CONFLIT"
    And le dossier d'anomalie est en conflit
    And le dossier d'anomalie ne signale aucune fin automatique

  Scenario: Corriger le debut pour repousser l'echeance annule l'ancre et garde l'activite en cours dans le perimetre
    Given il est "2044-03-11T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4511"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4511              |
    And j'ai engage l'element "Anomalie 4511" en atelier
    And il est "2044-03-11T08:00:00Z"
    And j'ai pointe sur "Anomalie 4511"
      | id        | 00000000-0000-0000-0000-000000045111 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-11T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4511" depuis l'evenement 0
    And le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    When je previsualise l'acte suivant sur le dossier d'anomalie
      | kind      | CORRECTION                   |
      | pointage  | 0                            |
      | motif     | Le travail a commence a midi |
      | type      | DEBUT                        |
      | intention | OUVERTURE                    |
      | instant   | 2044-03-11T12:00:00Z         |
    Then l'apercu du dossier d'anomalie est accepte
    And l'apercu du dossier d'anomalie donne les activites
      | moment | evenement | etat     | debut                | fin                  |
      | avant  | 0         | ECHUE    | 2044-03-11T08:00:00Z | 2044-03-11T21:00:00Z |
      | apres  | 0         | EN_COURS | 2044-03-11T12:00:00Z |                      |
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "ANCRE_ANNULEE"
    And le dossier d'anomalie ne signale aucune fin automatique
    And le dossier d'anomalie n'est pas en conflit
    And le perimetre du dossier d'anomalie contient l'activite de l'evenement 0
    And le dossier d'anomalie donne les activites
      | evenement | etat     |
      | 0         | EN_COURS |
    When je consulte le dossier d'anomalie de l'ancienne ancre
    Then le dossier d'anomalie est a l'etat "ANCRE_ANNULEE"

  Scenario: Un ouvrant corrige mais encore echu ouvre un nouveau dossier de fin automatique
    Given il est "2044-03-12T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4512"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4512              |
    And j'ai engage l'element "Anomalie 4512" en atelier
    And il est "2044-03-12T08:00:00Z"
    And j'ai pointe sur "Anomalie 4512"
      | id        | 00000000-0000-0000-0000-000000045121 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-anomalie                      |
      | poste     | fraiseuse-anomalie                   |
    And il est "2044-03-12T23:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4512" depuis l'evenement 0
    And le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    When je previsualise l'acte suivant sur le dossier d'anomalie
      | kind      | CORRECTION                          |
      | pointage  | 0                                   |
      | motif     | Le travail a commence a neuf heures |
      | type      | DEBUT                               |
      | intention | OUVERTURE                           |
      | instant   | 2044-03-12T09:00:00Z                |
    And je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "ANCRE_ANNULEE"
    And le dossier d'anomalie signale une fin automatique
    When je consulte le dossier d'anomalie du remplacant de l'ancre
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And le dossier d'anomalie donne les activites
      | evenement | etat  | debut                | fin                  |
      | 0         | ECHUE | 2044-03-12T09:00:00Z | 2044-03-12T22:00:00Z |
    And le dossier d'anomalie propose
      | code            | kind           |
      | REGULARISER_FIN | REGULARISATION |

  Scenario Outline: Un suivi cloture accepte une fin regularisee avant sa cloture et refuse celle d'apres
    Given il est "2044-03-13T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4513 <rang>"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4513-<rang>       |
    And j'ai engage l'element "Anomalie 4513 <rang>" en atelier
    And il est "2044-03-13T08:00:00Z"
    And j'ai pointe sur "Anomalie 4513 <rang>"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-13T23:30:00Z"
    And j'ai cloture "Anomalie 4513 <rang>"
      | dateDeSurvenue | 2044-03-13T22:00:00Z |
    And je consulte le dossier d'anomalie de "Anomalie 4513 <rang>" depuis l'evenement 0
    And le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "<fin>"
    Then la reponse a le statut http <statut>

    Examples:
      | rang | fin                  | statut |
      | 1    | 2044-03-13T17:00:00Z | 200    |
      | 2    | 2044-03-13T22:30:00Z | 409    |

  Scenario: Un suivi cloture refuse a l'apercu une fin posterieure a sa cloture avec son code
    Given il est "2044-03-14T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4514"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4514              |
    And j'ai engage l'element "Anomalie 4514" en atelier
    And il est "2044-03-14T08:00:00Z"
    And j'ai pointe sur "Anomalie 4514"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-14T23:30:00Z"
    And j'ai cloture "Anomalie 4514"
      | dateDeSurvenue | 2044-03-14T22:00:00Z |
    And je consulte le dossier d'anomalie de "Anomalie 4514" depuis l'evenement 0
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-14T22:30:00Z"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:suivi-d-atelier-cloture"
    And l'apercu du dossier d'anomalie ne modifie aucun fait

  Scenario: Une habilitation retiree depuis le pointage refuse l'apercu
    Given il est "2044-03-15T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4515"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4515              |
    And j'ai engage l'element "Anomalie 4515" en atelier
    And il est "2044-03-15T08:00:00Z"
    And j'ai pointe sur "Anomalie 4515"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-15T22:00:00Z"
    And l'operateur "dupont-anomalie" n'est plus habilite sur "fraiseuse-anomalie"
    And je consulte le dossier d'anomalie de "Anomalie 4515" depuis l'evenement 0
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-15T17:00:00Z"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:operateur-non-habilite"
    And l'apercu du dossier d'anomalie ne modifie aucun fait

  Scenario: Une habilitation retiree entre l'apercu et la confirmation rend l'apercu obsolete
    Given il est "2044-03-16T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4516"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4516              |
    And j'ai engage l'element "Anomalie 4516" en atelier
    And il est "2044-03-16T08:00:00Z"
    And j'ai pointe sur "Anomalie 4516"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-16T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4516" depuis l'evenement 0
    And je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-16T17:00:00Z"
    And l'apercu du dossier d'anomalie est accepte
    And l'operateur "dupont-anomalie" n'est plus habilite sur "fraiseuse-anomalie"
    When je tente de confirmer l'apercu du dossier d'anomalie
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:apercu-obsolete"
    And l'apercu du dossier d'anomalie ne modifie aucun fait

  Scenario: Une activite sans poste ouvre un dossier, un apercu et une confirmation sans poste
    Given il est "2044-03-17T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4517"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4517              |
    And j'ai engage l'element "Anomalie 4517" en atelier
    And il est "2044-03-17T08:00:00Z"
    And j'ai pointe sur "Anomalie 4517"
      | type      | DEBUT           |
      | intention | OUVERTURE       |
      | operateur | dupont-anomalie |
    And il est "2044-03-17T22:00:00Z"
    When je consulte le dossier d'anomalie de "Anomalie 4517" depuis l'evenement 0
    Then le dossier d'anomalie est a l'etat "FIN_AUTOMATIQUE"
    And la proposition "REGULARISER_FIN" du dossier d'anomalie reprend l'ouvrant 0 sans aucune heure
    And la proposition "REGULARISER_FIN" du dossier d'anomalie ne porte aucun poste
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-17T17:00:00Z"
    Then l'apercu du dossier d'anomalie est accepte
    And l'acte de l'apercu du dossier d'anomalie ne porte aucun poste
    When je confirme l'apercu du dossier d'anomalie
    Then le dossier d'anomalie est a l'etat "SANS_ANOMALIE"
    And l'evenement cree par la confirmation ne porte aucun poste

  Scenario: Une fin regularisee a une heure future est refusee sans ecriture
    Given il est "2044-03-18T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4518"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4518              |
    And j'ai engage l'element "Anomalie 4518" en atelier
    And il est "2044-03-18T08:00:00Z"
    And j'ai pointe sur "Anomalie 4518"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-18T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4518" depuis l'evenement 0
    When je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-18T22:00:00.000000001Z"
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:date-de-survenue-future"
    And l'apercu du dossier d'anomalie ne modifie aucun fait

  Scenario: Une ecriture concurrente entre l'apercu et la confirmation rend l'apercu obsolete
    Given il est "2044-03-19T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Anomalie 4519"
      | type      | ORDRE_DE_FABRICATION |
      | reference | ANO4519              |
    And j'ai engage l'element "Anomalie 4519" en atelier
    And il est "2044-03-19T08:00:00Z"
    And j'ai pointe sur "Anomalie 4519"
      | type      | DEBUT              |
      | intention | OUVERTURE          |
      | operateur | dupont-anomalie    |
      | poste     | fraiseuse-anomalie |
    And il est "2044-03-19T22:00:00Z"
    And je consulte le dossier d'anomalie de "Anomalie 4519" depuis l'evenement 0
    And je previsualise la proposition "REGULARISER_FIN" du dossier d'anomalie a l'instant "2044-03-19T17:00:00Z"
    And l'apercu du dossier d'anomalie est accepte
    And j'ai pointe sur "Anomalie 4519"
      | type      | DEBUT                |
      | intention | OUVERTURE            |
      | operateur | dupont-anomalie      |
      | poste     | rectifieuse-anomalie |
    When je tente de confirmer l'apercu du dossier d'anomalie
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:apercu-obsolete"
