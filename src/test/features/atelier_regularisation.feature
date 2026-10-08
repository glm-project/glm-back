@regularisation-directe
Feature: Regularisation directe de la fin d'une activite echue

  # Le gestionnaire regularise la fin d'une activite echue, sans fin reelle : le corps ne porte que l'identifiant de la
  # saisie (fourni par le client), l'activite et l'heure du fait. L'operateur, le poste et le type se deduisent de
  # l'activite, et seule cette fin garde une cible. La regularisation ne passe pas par la regle de reception.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-regul" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "rectifieuse-regul" de nature "rectification" et de cout horaire "60"
    And l'entreprise a declare l'operateur "dupont-regul" habilite sur "fraiseuse-regul" et "rectifieuse-regul" avec un taux horaire de "22"

  Scenario: Le gestionnaire regularise la fin d'une activite echue
    Given il est "2044-05-01T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9001"
      | categorie | OF      |
      | reference | REG9001 |
    And j'ai engage l'element "Regul 9001" en atelier
    And il est "2044-05-01T08:00:00Z"
    And j'ai pointe sur "Regul 9001"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-01T22:00:00Z"
    When je regularise sur "Regul 9001" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-01T17:00:00Z |
    Then la reponse a le statut http 201
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi vise l'activite de l'evenement 0
    And l'evenement 1 du suivi n'ouvre aucune activite
    And l'evenement 1 du suivi porte l'operateur "dupont-regul" et le poste "fraiseuse-regul"
    And l'evenement 1 du suivi est une regularisation de "dupont-regul" saisie par "gestionnaire"
    And l'evenement 1 du suivi a survenu a "2044-05-01T17:00:00Z" et a ete saisi a "2044-05-01T22:00:00Z" par "gestionnaire"
    When je consulte le temps effectif de "Regul 9001"
    Then le temps effectif contient
      | debut                | fin                  | finAutomatique |
      | 2044-05-01T08:00:00Z | 2044-05-01T17:00:00Z | false          |
    When je consulte le dossier d'anomalie de "Regul 9001" depuis l'evenement 0
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:fin-automatique-introuvable"

  Scenario: La regularisation est refusee tant que l'activite n'est pas echue
    Given il est "2044-05-02T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9002"
      | categorie | OF      |
      | reference | REG9002 |
    And j'ai engage l'element "Regul 9002" en atelier
    And il est "2044-05-02T08:00:00Z"
    And j'ai pointe sur "Regul 9002"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-02T20:59:59.999999999Z"
    When je regularise sur "Regul 9002" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-02T17:00:00Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-non-echue"
    Given il est "2044-05-02T21:00:00Z"
    When je regularise sur "Regul 9002" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-02T17:00:00Z |
    Then la reponse a le statut http 201

  Scenario: Une activite terminee par un pointage n'est pas une fin automatique a regulariser
    Given il est "2044-05-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9003"
      | categorie | OF      |
      | reference | REG9003 |
    And j'ai engage l'element "Regul 9003" en atelier
    And il est "2044-05-03T08:00:00Z"
    And j'ai pointe sur "Regul 9003"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-03T10:00:00Z"
    And j'ai pointe sur "Regul 9003"
      | type      | FIN             |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-03T22:00:00Z"
    When je regularise sur "Regul 9003" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-03T17:00:00Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-non-echue"

  Scenario: Une activite deja regularisee ne se regularise pas une seconde fois
    Given il est "2044-05-04T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9004"
      | categorie | OF      |
      | reference | REG9004 |
    And j'ai engage l'element "Regul 9004" en atelier
    And il est "2044-05-04T08:00:00Z"
    And j'ai pointe sur "Regul 9004"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-04T22:00:00Z"
    And je regularise sur "Regul 9004" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-04T17:00:00Z |
    When je regularise sur "Regul 9004" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-04T18:00:00Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-deja-regularisee"
    When je consulte "Regul 9004"
    Then le journal du suivi contient 2 evenements

  Scenario: Une activite introuvable dans ce suivi est refusee
    Given il est "2044-05-05T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9005"
      | categorie | OF      |
      | reference | REG9005 |
    And j'ai engage l'element "Regul 9005" en atelier
    And il est "2044-05-05T22:00:00Z"
    When je regularise sur "Regul 9005"
      | activite       | 00000000-0000-0000-0000-000000090051 |
      | dateDeSurvenue | 2044-05-05T17:00:00Z                 |
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-visee-introuvable"

  Scenario: La fin ne precede pas le debut de l'activite
    Given il est "2044-05-06T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9006"
      | categorie | OF      |
      | reference | REG9006 |
    And j'ai engage l'element "Regul 9006" en atelier
    And il est "2044-05-06T08:00:00Z"
    And j'ai pointe sur "Regul 9006"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-06T22:00:00Z"
    When je regularise sur "Regul 9006" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-06T07:59:59Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:fin-avant-debut"
    When je regularise sur "Regul 9006" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-06T08:00:00Z |
    Then la reponse a le statut http 201

  Scenario: La fin ne depasse pas maintenant
    Given il est "2044-05-07T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9007"
      | categorie | OF      |
      | reference | REG9007 |
    And j'ai engage l'element "Regul 9007" en atelier
    And il est "2044-05-07T08:00:00Z"
    And j'ai pointe sur "Regul 9007"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-07T22:00:00Z"
    When je regularise sur "Regul 9007" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-07T22:00:00.000000001Z |
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:date-de-survenue-future"
    When je regularise sur "Regul 9007" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-07T22:00:00Z |
    Then la reponse a le statut http 201

  Scenario: La fin ne depasse pas le debut suivant sur la cle
    Given il est "2044-05-08T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9008"
      | categorie | OF      |
      | reference | REG9008 |
    And j'ai engage l'element "Regul 9008" en atelier
    And il est "2044-05-08T08:00:00Z"
    And j'ai pointe sur "Regul 9008"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-08T23:00:00Z"
    And j'ai pointe sur "Regul 9008"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-09T06:00:00Z"
    When je regularise sur "Regul 9008" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-08T23:00:01Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:fin-apres-borne"
    When je regularise sur "Regul 9008" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-08T23:00:00Z |
    Then la reponse a le statut http 201

  Scenario: La fin ne depasse pas la cloture
    Given il est "2044-05-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9009"
      | categorie | OF      |
      | reference | REG9009 |
    And j'ai engage l'element "Regul 9009" en atelier
    And il est "2044-05-10T08:00:00Z"
    And j'ai pointe sur "Regul 9009"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-10T22:00:00Z"
    And j'ai cloture "Regul 9009"
      | dateDeSurvenue | 2044-05-10T22:00:00Z |
    And il est "2044-05-11T06:00:00Z"
    When je regularise sur "Regul 9009" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-10T22:00:01Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:fin-apres-borne"
    When je regularise sur "Regul 9009" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-10T22:00:00Z |
    Then la reponse a le statut http 201

  Scenario: Un debut sur une autre cle ne borne pas la fin
    Given il est "2044-05-12T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9010"
      | categorie | OF      |
      | reference | REG9010 |
    And j'ai engage l'element "Regul 9010" en atelier
    And il est "2044-05-12T08:00:00Z"
    And j'ai pointe sur "Regul 9010"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-12T22:00:00Z"
    And j'ai pointe sur "Regul 9010"
      | type      | DEBUT             |
      | operateur | dupont-regul      |
      | poste     | rectifieuse-regul |
    And il est "2044-05-13T06:00:00Z"
    When je regularise sur "Regul 9010" en visant l'activite de l'evenement 0
      | dateDeSurvenue | 2044-05-12T23:30:00Z |
    Then la reponse a le statut http 201

  Scenario: Le renvoi de la meme saisie repond comme un succes sans rien ecrire de plus
    Given il est "2044-05-14T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9011"
      | categorie | OF      |
      | reference | REG9011 |
    And j'ai engage l'element "Regul 9011" en atelier
    And il est "2044-05-14T08:00:00Z"
    And j'ai pointe sur "Regul 9011"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-14T22:00:00Z"
    When je regularise sur "Regul 9011" en visant l'activite de l'evenement 0
      | id             | 00000000-0000-0000-0000-000000090111 |
      | dateDeSurvenue | 2044-05-14T17:00:00Z                 |
    Then la reponse a le statut http 201
    # L'activite est desormais regularisee : seul l'identifiant deja au journal fait repondre 200 au renvoi, avant toute regle.
    When je renvoie la derniere regularisation
    Then la reponse a le statut http 200
    And le journal du suivi contient 2 evenements
    And l'evenement 1 du suivi a l'identifiant "00000000-0000-0000-0000-000000090111"

  Scenario: Le dossier d'une fin automatique sans debut suivant ni cloture n'a pas de borne de fin
    Given il est "2044-05-15T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9012"
      | categorie | OF      |
      | reference | REG9012 |
    And j'ai engage l'element "Regul 9012" en atelier
    And il est "2044-05-15T08:00:00Z"
    And j'ai pointe sur "Regul 9012"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-15T22:00:00Z"
    And j'ai pointe sur "Regul 9012"
      | type      | DEBUT             |
      | operateur | dupont-regul      |
      | poste     | rectifieuse-regul |
    When je consulte le dossier d'anomalie de "Regul 9012" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie ne donne aucune borne de fin

  Scenario: La borne de fin du dossier est le plus tot du debut suivant sur la cle et de la cloture
    Given il est "2044-05-16T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9013"
      | categorie | OF      |
      | reference | REG9013 |
    And j'ai engage l'element "Regul 9013" en atelier
    And il est "2044-05-16T08:00:00Z"
    And j'ai pointe sur "Regul 9013"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-16T22:00:00Z"
    And j'ai pointe sur "Regul 9013"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-16T23:30:00Z"
    And j'ai cloture "Regul 9013"
      | dateDeSurvenue | 2044-05-16T23:30:00Z |
    When je consulte le dossier d'anomalie de "Regul 9013" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne la borne de fin "2044-05-16T22:00:00Z"

  Scenario: La borne de fin du dossier est la cloture quand aucun debut ne suit
    Given il est "2044-05-18T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9014"
      | categorie | OF      |
      | reference | REG9014 |
    And j'ai engage l'element "Regul 9014" en atelier
    And il est "2044-05-18T08:00:00Z"
    And j'ai pointe sur "Regul 9014"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-18T23:00:00Z"
    And j'ai cloture "Regul 9014"
      | dateDeSurvenue | 2044-05-18T23:00:00Z |
    When je consulte le dossier d'anomalie de "Regul 9014" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne la borne de fin "2044-05-18T23:00:00Z"

  Scenario: La borne de fin du dossier est le debut suivant quand rien ne cloture
    Given il est "2044-05-19T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Regul 9015"
      | categorie | OF      |
      | reference | REG9015 |
    And j'ai engage l'element "Regul 9015" en atelier
    And il est "2044-05-19T08:00:00Z"
    And j'ai pointe sur "Regul 9015"
      | type      | DEBUT           |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    And il est "2044-05-19T23:00:00Z"
    And j'ai pointe sur "Regul 9015"
      | type      | NON_CONFORMITE  |
      | operateur | dupont-regul    |
      | poste     | fraiseuse-regul |
    When je consulte le dossier d'anomalie de "Regul 9015" depuis l'evenement 0
    Then la reponse a le statut http 200
    And le dossier d'anomalie donne la borne de fin "2044-05-19T23:00:00Z"
