@correction-des-activites
Feature: Le gestionnaire corrige la journee d'un operateur

  # Trois corrections sur un suivi, une lecture dediee. Une correction n'efface ni ne modifie aucun fait du journal : elle
  # y ajoute des faits d'origine regularisation (un ajout ecrit une ouverture et une fin, un retrait et un changement de
  # poste un fait date du moment de la correction). La lecture rend la journee apres les corrections, coupee aux minuits
  # de l'entreprise (Europe/Paris, UTC+1 en janvier). La regle de reception juge ensuite les pointages sur ce journal.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "tour-corr" de nature "tournage" et de cout horaire "60.5"
    And l'entreprise a declare le poste de travail "fraiseuse-corr" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "rectifieuse-corr" de nature "rectification" et de cout horaire "70.5"
    And l'entreprise a declare l'operateur "dupont-corr" habilite sur "tour-corr" et "fraiseuse-corr" avec un taux horaire de "22.5"

  Scenario: La journee d'un operateur sans activite donne ses minuits et ses choix
    Given il est "2046-01-10T12:00:00Z"
    When je consulte la journee de "dupont-corr" du "2046-01-10"
    Then la reponse a le statut http 200
    And la journee est celle de "dupont-corr" le "2046-01-10" a l'evaluation "2046-01-10T12:00:00Z"
    And la journee porte les bornes
      | debut          | 2046-01-09T23:00:00Z |
      | fin            | 2046-01-10T23:00:00Z |
      | finDuLendemain | 2046-01-11T23:00:00Z |
    And la duree operationnelle de la journee est "PT0S"
    And la journee ne porte aucune activite
    And la journee propose les postes
      | poste          | nature   |
      | fraiseuse-corr | fraisage |
      | tour-corr      | tournage |

  Scenario: Les activites du jour portent leur suivi, leur poste, leur etat et leur duree
    Given il est "2046-01-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 1001"
      | categorie | OF       |
      | reference | CORR1001 |
    And j'ai engage l'element "Corr 1001" en atelier
    And il est "2046-01-10T08:00:00Z"
    And j'ai pointe sur "Corr 1001"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-10T10:00:00Z"
    And j'ai pointe sur "Corr 1001"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And j'ai pointe sur "Corr 1001"
      | type      | DEBUT          |
      | operateur | dupont-corr    |
      | poste     | fraiseuse-corr |
    And il est "2046-01-10T12:00:00Z"
    When je consulte la journee de "dupont-corr" du "2046-01-10"
    Then la reponse a le statut http 200
    And la journee porte les activites
      | debut                | fin                  | poste          | nature   | etat     | dureeSurLeJour | autresJours |
      | 2046-01-10T08:00:00Z | 2046-01-10T10:00:00Z | tour-corr      | tournage | TERMINEE | PT2H           |             |
      | 2046-01-10T10:00:00Z |                      | fraiseuse-corr | fraisage | EN_COURS |                |             |
    And la duree operationnelle de la journee est "PT2H"
    And l'activite de la journee commencee a "2046-01-10T08:00:00Z" est sur le suivi de "Corr 1001"
    And l'activite de la journee commencee a "2046-01-10T08:00:00Z" porte l'element "Corr 1001" de reference "CORR1001"
    And la journee propose le suivi de "Corr 1001" engage le "2046-01-10T06:00:00Z"

  Scenario: Une fin automatique compte jusqu'a son echeance et signale son etat
    Given il est "2046-01-10T00:30:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 1002"
      | categorie | OF       |
      | reference | CORR1002 |
    And j'ai engage l'element "Corr 1002" en atelier
    And il est "2046-01-10T01:00:00Z"
    And j'ai pointe sur "Corr 1002"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-10T20:00:00Z"
    When je consulte la journee de "dupont-corr" du "2046-01-10"
    Then la journee porte les activites
      | debut                | fin                  | poste     | etat                     | dureeSurLeJour | autresJours |
      | 2046-01-10T01:00:00Z | 2046-01-10T14:00:00Z | tour-corr | TERMINEE_AUTOMATIQUEMENT | PT13H          |             |
    And la duree operationnelle de la journee est "PT13H"
    When je consulte la journee de "dupont-corr" du "2046-01-10" a l'evaluation "2046-01-10T13:59:59Z"
    Then la journee porte les activites
      | debut                | fin | poste     | etat     | dureeSurLeJour | autresJours |
      | 2046-01-10T01:00:00Z |     | tour-corr | EN_COURS |                |             |
    And la duree operationnelle de la journee est "PT0S"

  Scenario: Une activite a cheval sur minuit est coupee aux minuits de l'entreprise
    Given il est "2046-01-10T12:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 1003"
      | categorie | OF       |
      | reference | CORR1003 |
    And j'ai engage l'element "Corr 1003" en atelier
    And il est "2046-01-10T21:00:00Z"
    And j'ai pointe sur "Corr 1003"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-11T03:00:00Z"
    And j'ai pointe sur "Corr 1003"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    When je consulte la journee de "dupont-corr" du "2046-01-10"
    Then la journee porte les activites
      | debut                | fin                  | etat     | dureeSurLeJour | autresJours     |
      | 2046-01-10T21:00:00Z | 2046-01-11T03:00:00Z | TERMINEE | PT2H           | 2046-01-11=PT4H |
    And la duree operationnelle de la journee est "PT2H"
    When je consulte la journee de "dupont-corr" du "2046-01-11"
    Then la journee porte les activites
      | debut                | fin                  | etat     | dureeSurLeJour | autresJours     |
      | 2046-01-10T21:00:00Z | 2046-01-11T03:00:00Z | TERMINEE | PT4H           | 2046-01-10=PT2H |
    And la duree operationnelle de la journee est "PT4H"

  Scenario: Les suivis presents distinguent l'engagement et la cloture
    Given il est "2046-01-13T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 1004"
      | categorie | OF       |
      | reference | CORR1004 |
    And j'ai engage l'element "Corr 1004" en atelier
    And l'entreprise a cree l'element de fabrication "Corr 1005"
      | categorie | OF       |
      | reference | CORR1005 |
    And j'ai engage l'element "Corr 1005" en atelier
    And il est "2046-01-13T08:00:00Z"
    And j'ai cloture "Corr 1005"
      | dateDeSurvenue | 2046-01-13T08:00:00Z |
    And il est "2046-01-14T12:00:00Z"
    When je consulte la journee de "dupont-corr" du "2046-01-13"
    Then la journee propose le suivi de "Corr 1004" engage le "2046-01-13T06:00:00Z"
    And la journee propose le suivi de "Corr 1005" engage le "2046-01-13T06:00:00Z" et cloture le "2046-01-13T08:00:00Z"
    When je consulte la journee de "dupont-corr" du "2046-01-14"
    Then la journee propose le suivi de "Corr 1004" engage le "2046-01-13T06:00:00Z"
    And la journee ne propose pas le suivi de "Corr 1005"
    When je consulte la journee de "dupont-corr" du "2046-01-12"
    Then la journee ne propose pas le suivi de "Corr 1004"
    And la journee ne propose pas le suivi de "Corr 1005"

  Scenario: Un operateur inconnu ne se lit pas
    When je consulte la journee de l'operateur inconnu du "2046-01-10"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:operateur-introuvable"

  Scenario: Un jour mal ecrit ou une evaluation mal ecrite se refuse
    When je consulte la journee de "dupont-corr" du "10-01-2046"
    Then la reponse a le statut http 400
    When je consulte la journee de "dupont-corr" du "2046-01-10" a l'evaluation "pas-un-instant"
    Then la reponse a le statut http 400

  Scenario: Seul le gestionnaire lit et corrige
    Given il est "2046-01-15T12:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 1006"
      | categorie | OF       |
      | reference | CORR1006 |
    And j'ai engage l'element "Corr 1006" en atelier
    And I am logged in as "user" with role "USER"
    When je consulte la journee de "dupont-corr" du "2046-01-15"
    Then la reponse a le statut http 403
    When j'ajoute sur "Corr 1006" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-01-15T08:00:00Z |
      | fin       | 2046-01-15T10:00:00Z |
    Then la reponse a le statut http 403
    When je retire une activite sur le suivi inconnu
    Then la reponse a le statut http 403

  Scenario: Ajouter une activite oubliee l'ecrit au journal et la compte dans la journee
    Given il est "2046-01-16T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2001"
      | categorie | OF       |
      | reference | CORR2001 |
    And j'ai engage l'element "Corr 2001" en atelier
    And il est "2046-01-16T18:00:00Z"
    When j'ajoute sur "Corr 2001" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-01-16T08:00:00Z |
      | fin       | 2046-01-16T11:30:00Z |
    Then la reponse a le statut http 201
    When je consulte "Corr 2001"
    Then le journal du suivi ne contient que les types
      | DEBUT |
      | FIN   |
    And l'evenement 0 du suivi est une regularisation de "dupont-corr" saisie par "gestionnaire"
    And l'evenement 0 du suivi a survenu a "2046-01-16T08:00:00Z" et a ete saisi a "2046-01-16T18:00:00Z" par "gestionnaire"
    And l'evenement 1 du suivi a survenu a "2046-01-16T11:30:00Z" et a ete saisi a "2046-01-16T18:00:00Z" par "gestionnaire"
    And l'evenement 1 du suivi n'ouvre aucune activite
    And l'evenement 0 du suivi a la nature "tournage"
    And l'evenement 0 du suivi a le cout horaire "60.5"
    And l'evenement 0 du suivi a le taux horaire "22.5"
    When je consulte la journee de "dupont-corr" du "2046-01-16"
    Then la journee porte les activites
      | debut                | fin                  | poste     | etat     | dureeSurLeJour |
      | 2046-01-16T08:00:00Z | 2046-01-16T11:30:00Z | tour-corr | TERMINEE | PT3H30M        |
    And la duree operationnelle de la journee est "PT3H30M"
    When je liste les fins automatiques de "Corr 2001"
    Then la liste des fins automatiques compte 0 ligne

  Scenario: Ajouter une non conformite sans poste
    Given il est "2046-01-17T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2002"
      | categorie | OF       |
      | reference | CORR2002 |
    And j'ai engage l'element "Corr 2002" en atelier
    And il est "2046-01-17T18:00:00Z"
    When j'ajoute sur "Corr 2002" une activite
      | operateur | dupont-corr          |
      | categorie | NON_CONFORMITE       |
      | debut     | 2046-01-17T08:00:00Z |
      | fin       | 2046-01-17T09:00:00Z |
    Then la reponse a le statut http 201
    When je consulte "Corr 2002"
    Then le journal du suivi ne contient que les types
      | NON_CONFORMITE |
      | FIN            |
    When je consulte la journee de "dupont-corr" du "2046-01-17"
    Then la journee porte les activites
      | debut                | categorie      | poste | etat     | dureeSurLeJour |
      | 2046-01-17T08:00:00Z | NON_CONFORMITE |       | TERMINEE | PT1H           |

  Scenario: Ajouter une activite qui finit le lendemain la compte sur chaque jour
    Given il est "2046-01-18T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2003"
      | categorie | OF       |
      | reference | CORR2003 |
    And j'ai engage l'element "Corr 2003" en atelier
    And il est "2046-01-19T12:00:00Z"
    When j'ajoute sur "Corr 2003" une activite
      | operateur | dupont-corr               |
      | poste     | tour-corr                 |
      | categorie | TRAVAIL                   |
      | debut     | 2046-01-18T22:00:00+01:00 |
      | fin       | 2046-01-19T04:30:00+01:00 |
    Then la reponse a le statut http 201
    When je consulte la journee de "dupont-corr" du "2046-01-18"
    Then la journee porte les activites
      | debut                | fin                  | etat     | dureeSurLeJour | autresJours        |
      | 2046-01-18T21:00:00Z | 2046-01-19T03:30:00Z | TERMINEE | PT2H           | 2046-01-19=PT4H30M |
    When je relis la synthese des heures de "dupont-corr" pour la semaine 3 de 2046
    Then le jour "2046-01-18" a une duree operationnelle de "PT2H"
    And le jour "2046-01-19" a une duree operationnelle de "PT4H30M"

  Scenario: Un renvoi de la meme saisie ne s'ecrit pas une seconde fois
    Given il est "2046-01-20T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2004"
      | categorie | OF       |
      | reference | CORR2004 |
    And j'ai engage l'element "Corr 2004" en atelier
    And il est "2046-01-20T18:00:00Z"
    When j'ajoute sur "Corr 2004" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-01-20T08:00:00Z |
      | fin       | 2046-01-20T10:00:00Z |
    Then la reponse a le statut http 201
    When je renvoie la derniere correction
    Then la reponse a le statut http 200
    When je consulte "Corr 2004"
    Then le journal du suivi contient 2 evenements
    When je consulte la journee de "dupont-corr" du "2046-01-20"
    Then la duree operationnelle de la journee est "PT2H"

  Scenario Outline: Un ajout invalide est refuse avec son code
    Given il est "2046-01-21T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "<element>"
      | categorie | OF          |
      | reference | <reference> |
    And j'ai engage l'element "<element>" en atelier
    And il est "2046-01-21T18:00:00Z"
    And j'ai ajoute sur "<element>" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-01-21T09:00:00Z |
      | fin       | 2046-01-21T11:00:00Z |
    When j'ajoute sur "<element>" une activite
      | operateur | dupont-corr |
      | poste     | <poste>     |
      | categorie | TRAVAIL     |
      | debut     | <debut>     |
      | fin       | <fin>       |
    Then la reponse a le statut http <statut>
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:<code>"

    Examples:
      | element   | reference | poste            | debut                | fin                  | statut | code                               |
      | Corr 2101 | CORR2101  | tour-corr        | 2046-01-21T10:00:00Z | 2046-01-21T12:00:00Z | 409    | chevauchement                      |
      | Corr 2102 | CORR2102  | tour-corr        | 2046-01-21T08:00:00Z | 2046-01-21T09:30:00Z | 409    | chevauchement                      |
      | Corr 2103 | CORR2103  | tour-corr        | 2046-01-21T12:00:00Z | 2046-01-21T12:00:00Z | 409    | fin-avant-debut                    |
      | Corr 2104 | CORR2104  | tour-corr        | 2046-01-21T13:00:00Z | 2046-01-21T12:00:00Z | 409    | fin-avant-debut                    |
      | Corr 2105 | CORR2105  | tour-corr        | 2046-01-21T12:00:00Z | 2046-01-21T18:00:01Z | 400    | date-de-survenue-future            |
      | Corr 2106 | CORR2106  | tour-corr        | 2046-01-21T05:59:59Z | 2046-01-21T06:30:00Z | 409    | evenement-anterieur-a-l-engagement |
      | Corr 2107 | CORR2107  | rectifieuse-corr | 2046-01-21T12:00:00Z | 2046-01-21T13:00:00Z | 409    | operateur-non-habilite             |

  Scenario: Un ajout sur un poste, un operateur ou un suivi inconnus est refuse
    Given il est "2046-01-22T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2201"
      | categorie | OF       |
      | reference | CORR2201 |
    And j'ai engage l'element "Corr 2201" en atelier
    And il est "2046-01-22T18:00:00Z"
    When j'ajoute sur "Corr 2201" une activite
      | operateur | dupont-corr                          |
      | poste     | 00000000-0000-0000-0000-000000002201 |
      | categorie | TRAVAIL                              |
      | debut     | 2046-01-22T08:00:00Z                 |
      | fin       | 2046-01-22T10:00:00Z                 |
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:poste-de-travail-introuvable"
    When j'ajoute sur "Corr 2201" une activite
      | operateur | 00000000-0000-0000-0000-000000002202 |
      | poste     | tour-corr                            |
      | categorie | TRAVAIL                              |
      | debut     | 2046-01-22T08:00:00Z                 |
      | fin       | 2046-01-22T10:00:00Z                 |
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:operateur-introuvable"
    When je retire une activite sur le suivi inconnu
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:suivi-d-atelier-introuvable"

  Scenario: Un ajout dont la fin suit la cloture est refuse
    Given il est "2046-01-23T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2301"
      | categorie | OF       |
      | reference | CORR2301 |
    And j'ai engage l'element "Corr 2301" en atelier
    And il est "2046-01-23T12:00:00Z"
    And j'ai cloture "Corr 2301"
      | dateDeSurvenue | 2046-01-23T12:00:00Z |
    And il est "2046-01-23T18:00:00Z"
    When j'ajoute sur "Corr 2301" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-01-23T11:00:00Z |
      | fin       | 2046-01-23T12:00:01Z |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:evenement-posterieur-a-la-cloture"
    When j'ajoute sur "Corr 2301" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-01-23T11:00:00Z |
      | fin       | 2046-01-23T12:00:00Z |
    Then la reponse a le statut http 201

  Scenario: Un corps incomplet ou un faux pointage se refuse
    Given il est "2046-01-24T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 2401"
      | categorie | OF       |
      | reference | CORR2401 |
    And j'ai engage l'element "Corr 2401" en atelier
    When j'envoie une correction sans corps sur "Corr 2401"
    Then la reponse a le statut http 400
    When je pointe un "RETRAIT" sur "Corr 2401"
    Then la reponse a le statut http 400

  Scenario: Changer le poste d'une activite recopie la nature et le cout du nouveau poste
    Given il est "2046-01-25T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 3001"
      | categorie | OF       |
      | reference | CORR3001 |
    And j'ai engage l'element "Corr 3001" en atelier
    And il est "2046-01-25T08:00:00Z"
    And j'ai pointe sur "Corr 3001"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-25T10:00:00Z"
    And j'ai pointe sur "Corr 3001"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-26T09:00:00Z"
    When je change sur "Corr 3001" le poste de l'activite de "dupont-corr" commencee a "2046-01-25T08:00:00Z"
      | poste | fraiseuse-corr |
    Then la reponse a le statut http 201
    When je consulte "Corr 3001"
    Then le journal du suivi se termine par les types
      | CHANGEMENT_DE_POSTE |
    And le dernier evenement du journal du suivi est un fait du gestionnaire de type "CHANGEMENT_DE_POSTE" date de "2046-01-26T09:00:00Z"
    And le fait "CHANGEMENT_DE_POSTE" du journal du suivi a la nature "fraisage" et le cout horaire "45.5"
    When je consulte la journee de "dupont-corr" du "2046-01-25"
    Then la journee porte les activites
      | debut                | fin                  | poste          | nature   | etat     | dureeSurLeJour |
      | 2046-01-25T08:00:00Z | 2046-01-25T10:00:00Z | fraiseuse-corr | fraisage | TERMINEE | PT2H           |
    And la duree operationnelle de la journee est "PT2H"

  Scenario: Le poste d'une fin automatique change sans lui retirer son anomalie
    Given il est "2046-01-27T00:30:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 3002"
      | categorie | OF       |
      | reference | CORR3002 |
    And j'ai engage l'element "Corr 3002" en atelier
    And il est "2046-01-27T01:00:00Z"
    And j'ai pointe sur "Corr 3002"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-27T20:00:00Z"
    When je change sur "Corr 3002" le poste de l'activite de "dupont-corr" commencee a "2046-01-27T01:00:00Z"
      | poste | fraiseuse-corr |
    Then la reponse a le statut http 201
    When je consulte la journee de "dupont-corr" du "2046-01-27"
    Then la journee porte les activites
      | debut                | fin                  | poste          | etat                     | dureeSurLeJour |
      | 2046-01-27T01:00:00Z | 2046-01-27T14:00:00Z | fraiseuse-corr | TERMINEE_AUTOMATIQUEMENT | PT13H          |
    When je liste les fins automatiques de "Corr 3002"
    Then la liste des fins automatiques compte 1 ligne
    And l'adresse de la premiere ligne de la liste des fins automatiques ouvre le dossier de la meme activite

  Scenario Outline: Un changement de poste invalide est refuse avec son code
    Given il est "2046-01-28T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "<element>"
      | categorie | OF          |
      | reference | <reference> |
    And j'ai engage l'element "<element>" en atelier
    And il est "2046-01-28T08:00:00Z"
    And j'ai pointe sur "<element>"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-28T09:00:00Z"
    And j'ai pointe sur "<element>"
      | type      | DEBUT          |
      | operateur | dupont-corr    |
      | poste     | fraiseuse-corr |
    And il est "2046-01-28T09:30:00Z"
    And j'ai pointe sur "<element>"
      | type      | FIN            |
      | operateur | dupont-corr    |
      | poste     | fraiseuse-corr |
    And il est "2046-01-28T10:00:00Z"
    And j'ai pointe sur "<element>"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-28T12:00:00Z"
    When je change sur "<element>" le poste de l'activite de "dupont-corr" commencee a "2046-01-28T08:00:00Z"
      | poste | <poste> |
    Then la reponse a le statut http <statut>
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:<code>"

    Examples:
      | element   | reference | poste                                | statut | code                         |
      | Corr 3101 | CORR3101  | tour-corr                            | 409    | meme-poste                   |
      | Corr 3102 | CORR3102  | fraiseuse-corr                       | 409    | chevauchement                |
      | Corr 3103 | CORR3103  | rectifieuse-corr                     | 409    | operateur-non-habilite       |
      | Corr 3104 | CORR3104  | 00000000-0000-0000-0000-000000003104 | 404    | poste-de-travail-introuvable |

  Scenario: Une activite en cours ou inconnue ne se corrige pas
    Given il est "2046-01-29T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 3201"
      | categorie | OF       |
      | reference | CORR3201 |
    And j'ai engage l'element "Corr 3201" en atelier
    And il est "2046-01-29T08:00:00Z"
    And j'ai pointe sur "Corr 3201"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-29T09:00:00Z"
    When je change sur "Corr 3201" le poste de l'activite de "dupont-corr" commencee a "2046-01-29T08:00:00Z"
      | poste | fraiseuse-corr |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-en-cours"
    When je retire sur "Corr 3201" l'activite de "dupont-corr" commencee a "2046-01-29T08:00:00Z"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-en-cours"
    When je change le poste de l'activite inconnue sur "Corr 3201"
      | poste | fraiseuse-corr |
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-visee-introuvable"
    When je retire l'activite inconnue sur "Corr 3201"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-visee-introuvable"

  Scenario: Retirer une activite l'ecarte de la journee et du releve des heures
    Given il est "2046-01-10T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 4001"
      | categorie | OF       |
      | reference | CORR4001 |
    And j'ai engage l'element "Corr 4001" en atelier
    And il est "2046-01-10T08:00:00Z"
    And j'ai pointe sur "Corr 4001"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-10T10:00:00Z"
    And j'ai pointe sur "Corr 4001"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-11T09:00:00Z"
    When je relis la synthese des heures de "dupont-corr" pour la semaine 2 de 2046
    Then le jour "2046-01-10" a une duree operationnelle de "PT2H"
    When je retire sur "Corr 4001" l'activite de "dupont-corr" commencee a "2046-01-10T08:00:00Z"
    Then la reponse a le statut http 201
    When je consulte "Corr 4001"
    Then le journal du suivi se termine par les types
      | RETRAIT |
    And le dernier evenement du journal du suivi est un fait du gestionnaire de type "RETRAIT" date de "2046-01-11T09:00:00Z"
    And le journal du suivi contient 3 evenements
    When je consulte la journee de "dupont-corr" du "2046-01-10"
    Then la journee ne porte aucune activite
    And la duree operationnelle de la journee est "PT0S"
    When je relis la synthese des heures de "dupont-corr" pour la semaine 2 de 2046
    Then le jour "2046-01-10" a une duree operationnelle de "PT0S"
    And la duree operationnelle totale de la semaine est "PT0S"
    # Le journal brut garde les gestes pointes, jamais le fait de correction.
    And les pointages du "2046-01-10" sont
      | type  | dateDeSurvenue       |
      | DEBUT | 2046-01-10T08:00:00Z |
      | FIN   | 2046-01-10T10:00:00Z |

  Scenario: Retirer une fin automatique en fait disparaitre l'anomalie
    Given il est "2046-01-30T00:30:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 4002"
      | categorie | OF       |
      | reference | CORR4002 |
    And j'ai engage l'element "Corr 4002" en atelier
    And il est "2046-01-30T01:00:00Z"
    And j'ai pointe sur "Corr 4002"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-01-30T20:00:00Z"
    When je liste les fins automatiques de "Corr 4002"
    Then la liste des fins automatiques compte 1 ligne
    When je retire sur "Corr 4002" l'activite de "dupont-corr" commencee a "2046-01-30T01:00:00Z"
    Then la reponse a le statut http 201
    When je liste les fins automatiques de "Corr 4002"
    Then la liste des fins automatiques compte 0 ligne

  Scenario: Retirer deux fois la meme activite la trouve introuvable, la meme saisie est un renvoi
    Given il est "2046-02-01T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 4003"
      | categorie | OF       |
      | reference | CORR4003 |
    And j'ai engage l'element "Corr 4003" en atelier
    And il est "2046-02-01T08:00:00Z"
    And j'ai pointe sur "Corr 4003"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-01T10:00:00Z"
    And j'ai pointe sur "Corr 4003"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-02T09:00:00Z"
    When je retire sur "Corr 4003" l'activite de "dupont-corr" commencee a "2046-02-01T08:00:00Z"
    Then la reponse a le statut http 201
    When je renvoie la derniere correction
    Then la reponse a le statut http 200
    When je consulte "Corr 4003"
    Then le journal du suivi contient 3 evenements
    When je retire sur "Corr 4003" l'activite de "dupont-corr" commencee a "2046-02-01T08:00:00Z"
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:activite-visee-introuvable"

  Scenario: Une activite d'un suivi cloture ne se corrige pas, mais un retrait se fait sans habilitation
    Given il est "2046-02-03T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 4004"
      | categorie | OF       |
      | reference | CORR4004 |
    And j'ai engage l'element "Corr 4004" en atelier
    And il est "2046-02-03T08:00:00Z"
    And j'ai pointe sur "Corr 4004"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-03T10:00:00Z"
    And j'ai pointe sur "Corr 4004"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-03T11:00:00Z"
    And j'ai cloture "Corr 4004"
      | dateDeSurvenue | 2046-02-03T11:00:00Z |
    And il est "2046-02-03T12:00:00Z"
    When je retire sur "Corr 4004" l'activite de "dupont-corr" commencee a "2046-02-03T08:00:00Z"
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:evenement-posterieur-a-la-cloture"
    When je change sur "Corr 4004" le poste de l'activite de "dupont-corr" commencee a "2046-02-03T08:00:00Z"
      | poste | fraiseuse-corr |
    Then la reponse a le statut http 409
    And la reponse porte le code d'erreur "urn:glm:erreur:atelier:evenement-posterieur-a-la-cloture"
    When je rouvre "Corr 4004"
    And l'operateur "dupont-corr" n'est plus habilite sur "tour-corr"
    And je retire sur "Corr 4004" l'activite de "dupont-corr" commencee a "2046-02-03T08:00:00Z"
    Then la reponse a le statut http 201

  Scenario: Apres un ajout, le pupitre ne peut plus pointer dans la plage ajoutee
    Given il est "2046-02-04T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 5001"
      | categorie | OF       |
      | reference | CORR5001 |
    And j'ai engage l'element "Corr 5001" en atelier
    And il est "2046-02-04T18:00:00Z"
    And j'ai ajoute sur "Corr 5001" une activite
      | operateur | dupont-corr          |
      | poste     | tour-corr            |
      | categorie | TRAVAIL              |
      | debut     | 2046-02-04T08:00:00Z |
      | fin       | 2046-02-04T12:00:00Z |
    When l'operateur "dupont-corr" pointe DEBUT sur "Corr 5001" au poste "tour-corr" a "2046-02-04T10:00:00Z" et le serveur le recoit a "2046-02-04T18:30:00Z"
    Then le pointage est ignore
    And la table d'audit des pointages ignores de "Corr 5001" contient
      | raison    |
      | ANTERIEUR |
    When l'operateur "dupont-corr" pointe DEBUT sur "Corr 5001" au poste "tour-corr" a "2046-02-04T13:00:00Z" et le serveur le recoit a "2046-02-04T18:40:00Z"
    Then le pointage est accepte

  Scenario: Apres un retrait, le pupitre peut pointer dans la plage retiree
    Given il est "2046-02-05T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 5002"
      | categorie | OF       |
      | reference | CORR5002 |
    And j'ai engage l'element "Corr 5002" en atelier
    And il est "2046-02-05T08:00:00Z"
    And j'ai pointe sur "Corr 5002"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-05T12:00:00Z"
    And j'ai pointe sur "Corr 5002"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    When l'operateur "dupont-corr" pointe DEBUT sur "Corr 5002" au poste "tour-corr" a "2046-02-05T10:00:00Z" et le serveur le recoit a "2046-02-05T18:00:00Z"
    Then le pointage est ignore
    Given j'ai retire sur "Corr 5002" l'activite de "dupont-corr" commencee a "2046-02-05T08:00:00Z"
    When l'operateur "dupont-corr" pointe DEBUT sur "Corr 5002" au poste "tour-corr" a "2046-02-05T10:00:00Z" et le serveur le recoit a "2046-02-05T18:30:00Z"
    Then le pointage est accepte

  Scenario: Apres un changement de poste, les faits de l'activite comptent sur le nouveau poste
    Given il est "2046-02-06T06:00:00Z"
    And l'entreprise a cree l'element de fabrication "Corr 5003"
      | categorie | OF       |
      | reference | CORR5003 |
    And j'ai engage l'element "Corr 5003" en atelier
    And il est "2046-02-06T08:00:00Z"
    And j'ai pointe sur "Corr 5003"
      | type      | DEBUT       |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-06T12:00:00Z"
    And j'ai pointe sur "Corr 5003"
      | type      | FIN         |
      | operateur | dupont-corr |
      | poste     | tour-corr   |
    And il est "2046-02-06T18:00:00Z"
    And j'ai change sur "Corr 5003" le poste de l'activite de "dupont-corr" commencee a "2046-02-06T08:00:00Z"
      | poste | fraiseuse-corr |
    When l'operateur "dupont-corr" pointe DEBUT sur "Corr 5003" au poste "fraiseuse-corr" a "2046-02-06T10:00:00Z" et le serveur le recoit a "2046-02-06T18:30:00Z"
    Then le pointage est ignore
    When l'operateur "dupont-corr" pointe DEBUT sur "Corr 5003" au poste "tour-corr" a "2046-02-06T10:00:00Z" et le serveur le recoit a "2046-02-06T18:40:00Z"
    Then le pointage est accepte
