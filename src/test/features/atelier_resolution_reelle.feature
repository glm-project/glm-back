Feature: Resolution reelle des conflits
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-resolution" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare l'operateur "dupont-resolution" habilite sur "fraiseuse-resolution"

  Scenario: Resolution reelle d'une fin remplacee avec neuf decimales et decalage conserve
    Given il est "2044-01-06T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4401"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4401             |
    And j'ai engage l'element "Resolution 4401" en atelier
    And il est "2044-01-06T08:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4401"
      | id        | 00000000-0000-0000-0000-000000004401 |
      | type      | DEBUT                |
      | intention | OUVERTURE            |
      | operateur | dupont-resolution    |
      | poste     | fraiseuse-resolution |
    And il est "2044-01-06T12:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4401"
      | id        | 00000000-0000-0000-0000-000000004402 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000004401 |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-06T17:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4401"
      | id        | 00000000-0000-0000-0000-000000004403 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000004401 |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-06T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4401" ancre 0
      | kind      | CORRECTION                         |
      | pointage  | 2                                  |
      | motif     | La fin doit terminer la NC         |
      | type      | FIN                                |
      | intention | FIN                                |
      | cible     | 1                                  |
      | instant   | 2044-01-06T19:00:00.123456789+02:00 |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT5H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4401" ont les durees
      | PT4H |
      | PT5H |
