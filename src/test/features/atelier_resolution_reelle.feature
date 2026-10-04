Feature: Resolution reelle des conflits

  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'entreprise a declare le poste de travail "fraiseuse-resolution" de nature "fraisage" et de cout horaire "45.5"
    And l'entreprise a declare le poste de travail "rectifieuse-resolution" de nature "rectification" et de cout horaire "60"
    And l'entreprise a declare l'operateur "dupont-resolution" habilite sur "fraiseuse-resolution" et "rectifieuse-resolution" avec un taux horaire de "22"

  Scenario: Resolution reelle d'une fin remplacee avec neuf decimales et decalage conserve
    Given il est "2044-01-06T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4401"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4401              |
    And j'ai engage l'element "Resolution 4401" en atelier
    And il est "2044-01-06T08:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4401"
      | id        | 00000000-0000-0000-0000-000000004401 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
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
      | kind      | CORRECTION                          |
      | pointage  | 2                                   |
      | motif     | La fin doit terminer la NC          |
      | type      | FIN                                 |
      | intention | FIN                                 |
      | cible     | 1                                   |
      | instant   | 2044-01-06T19:00:00.123456789+02:00 |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT5H  |
    And les quatre lecteurs API expliquent ce conflit
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les quatre lecteurs API relevent la correction exacte
    And les periodes relues de "Resolution 4401" ont les durees
      | PT4H |
      | PT5H |

  Scenario: Resolution reelle par annulation de la transition donne neuf heures
    Given il est "2044-01-07T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4402"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4402              |
    And j'ai engage l'element "Resolution 4402" en atelier
    And il est "2044-01-07T08:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4402"
      | id        | 00000000-0000-0000-0000-000000044021 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-07T12:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4402"
      | id        | 00000000-0000-0000-0000-000000044022 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000044021 |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-07T17:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4402"
      | id        | 00000000-0000-0000-0000-000000044023 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000044021 |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-07T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4402" ancre 0
      | kind     | ANNULATION                |
      | pointage | 1                         |
      | motif    | Transition saisie en trop |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree |
      | TRAVAIL   | TERMINEE | PT9H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4402" ont les durees
      | PT9H |

  Scenario: Resolution reelle avec une fin NC arbitraire a dix sept heures une
    Given il est "2044-01-08T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4403"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4403              |
    And j'ai engage l'element "Resolution 4403" en atelier
    And il est "2044-01-08T08:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4403"
      | id        | 00000000-0000-0000-0000-000000044031 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-08T12:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4403"
      | id        | 00000000-0000-0000-0000-000000044032 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | cible     | 00000000-0000-0000-0000-000000044031 |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-08T17:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4403"
      | id        | 00000000-0000-0000-0000-000000044033 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | cible     | 00000000-0000-0000-0000-000000044031 |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-08T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4403" ancre 0
      | kind      | CORRECTION                             |
      | pointage  | 2                                      |
      | motif     | La fin de NC est a dix sept heures une |
      | type      | FIN                                    |
      | intention | FIN                                    |
      | cible     | 1                                      |
      | instant   | 2044-01-08T19:01:00.123456789+02:00    |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree  |
      | TRAVAIL        | TERMINEE | PT4H   |
      | NON_CONFORMITE | TERMINEE | PT5H1M |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4403" ont les durees
      | PT4H   |
      | PT5H1M |

  Scenario: Resolution reelle de la transition remplacee conserve la reprise
    Given il est "2044-01-09T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4404"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4404              |
    And j'ai engage l'element "Resolution 4404" en atelier
    And il est "2044-01-09T08:00:00Z"
    And j'ai pointe sur "Resolution 4404"
      | id        | 00000000-0000-0000-0000-000000044041 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-09T12:00:00Z"
    And j'ai pointe sur "Resolution 4404"
      | id        | 00000000-0000-0000-0000-000000044042 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044041 |
    And il est "2044-01-09T14:00:00Z"
    And j'ai pointe sur "Resolution 4404"
      | id        | 00000000-0000-0000-0000-000000044043 |
      | type      | DEBUT                                |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044041 |
    And il est "2044-01-09T17:00:00Z"
    And j'ai pointe sur "Resolution 4404"
      | id        | 00000000-0000-0000-0000-000000044044 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044043 |
    And il est "2044-01-09T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4404" ancre 0
      | kind      | CORRECTION               |
      | pointage  | 2                        |
      | motif     | La reprise termine la NC |
      | type      | DEBUT                    |
      | intention | TRANSITION               |
      | cible     | 1                        |
      | instant   | 2044-01-09T14:00:00Z     |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT2H  |
      | TRAVAIL        | TERMINEE | PT3H  |
    And la correction conserve l'identite de l'activite ouverte
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4404" ont les durees
      | PT4H |
      | PT2H |
      | PT3H |

  Scenario: Resolution reelle de meme categorie conserve les cibles de la transition
    Given il est "2044-01-10T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4405"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4405              |
    And j'ai engage l'element "Resolution 4405" en atelier
    And il est "2044-01-10T08:00:00Z"
    And j'ai pointe sur "Resolution 4405"
      | id        | 00000000-0000-0000-0000-000000044051 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-10T12:00:00Z"
    And j'ai pointe sur "Resolution 4405"
      | id        | 00000000-0000-0000-0000-000000044052 |
      | type      | DEBUT                                |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044051 |
    And il est "2044-01-10T17:00:00Z"
    And j'ai pointe sur "Resolution 4405"
      | id        | 00000000-0000-0000-0000-000000044053 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044052 |
    And il est "2044-01-10T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4405" ancre 0
      | kind      | CORRECTION                          |
      | pointage  | 1                                   |
      | motif     | Le travail etait une non conformite |
      | type      | NON_CONFORMITE                      |
      | intention | TRANSITION                          |
      | cible     | 0                                   |
      | instant   | 2044-01-10T12:00:00Z                |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT5H  |
    And la correction conserve l'identite de l'activite ouverte
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4405" ont les durees
      | PT4H |
      | PT5H |

  Scenario: Resolution reelle de la fin avant ouverture sans poste conserve neuf decimales
    Given il est "2044-01-11T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4406"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4406              |
    And j'ai engage l'element "Resolution 4406" en atelier
    And il est "2044-01-11T08:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4406"
      | id        | 00000000-0000-0000-0000-000000044061 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
    And il est "2044-01-11T09:00:00Z"
    And j'ai pointe sur "Resolution 4406"
      | id             | 00000000-0000-0000-0000-000000044062 |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | operateur      | dupont-resolution                    |
      | cible          | 00000000-0000-0000-0000-000000044061 |
      | dateDeSurvenue | 2044-01-11T07:00:00Z                 |
    And il est "2044-01-11T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4406" ancre 0
      | kind      | CORRECTION                        |
      | pointage  | 0                                 |
      | motif     | La fin survient a dix sept heures |
      | type      | FIN                               |
      | intention | FIN                               |
      | cible     | 1                                 |
      | instant   | 2044-01-11T17:00:00Z              |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree                |
      | TRAVAIL   | TERMINEE | PT8H59M59.876543211S |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4406" ont les durees
      | PT8H59M59.876543211S |

  Scenario: Resolution reelle de deux fins distinctes separees de deux secondes
    Given il est "2044-01-12T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4407"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4407              |
    And j'ai engage l'element "Resolution 4407" en atelier
    And il est "2044-01-12T08:00:00Z"
    And j'ai pointe sur "Resolution 4407"
      | id        | 00000000-0000-0000-0000-000000044071 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-12T17:00:00Z"
    And j'ai pointe sur "Resolution 4407"
      | id        | 00000000-0000-0000-0000-000000044072 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044071 |
    And il est "2044-01-12T17:00:02Z"
    And j'ai pointe sur "Resolution 4407"
      | id        | 00000000-0000-0000-0000-000000044073 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044071 |
    And il est "2044-01-12T18:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    When je consulte "Resolution 4407"
    Then le journal du suivi contient 3 evenements
    When je prepare la resolution du conflit de "Resolution 4407" ancre 0
      | kind     | ANNULATION                             |
      | pointage | 2                                      |
      | motif    | La deuxieme fin est une saisie en trop |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree |
      | TRAVAIL   | TERMINEE | PT9H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4407" ont les durees
      | PT9H |

  Scenario: Resolution reelle a treize heures reste en cours apres annulation
    Given il est "2044-01-13T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4408"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4408              |
    And j'ai engage l'element "Resolution 4408" en atelier
    And il est "2044-01-13T08:00:00Z"
    And j'ai pointe sur "Resolution 4408"
      | id        | 00000000-0000-0000-0000-000000044081 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-13T12:00:00Z"
    And j'ai pointe sur "Resolution 4408"
      | id        | 00000000-0000-0000-0000-000000044082 |
      | type      | DEBUT                                |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044081 |
    And il est "2044-01-13T13:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4408" ancre 0
      | kind     | ANNULATION                             |
      | pointage | 1                                      |
      | motif    | Le travail continue depuis huit heures |
    Then l'apercu donne les activites de resolution
      | categorie | etat     |
      | TRAVAIL   | EN_COURS |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And l'apercu ne fixe aucune fin ni duree definitive

    And les API lecteurs gardent le travail en cours sans duree finale
    Given il est "2044-01-13T20:59:59.999999999Z"
    Then les API lecteurs gardent le travail en cours sans duree finale
    Given il est "2044-01-13T21:00:00Z"
    Then les API lecteurs jugent le travail echu a vingt et une heures
    And la liste de ce suivi conserve 0 sequences en conflit

  Scenario: Resolution reelle de la fin orpheline apres annulation de louvrant
    Given il est "2044-01-14T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4409"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4409              |
    And j'ai engage l'element "Resolution 4409" en atelier
    And il est "2044-01-14T08:00:00Z"
    And j'ai pointe sur "Resolution 4409"
      | id        | 00000000-0000-0000-0000-000000044091 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
    And il est "2044-01-14T17:00:00Z"
    And j'ai pointe sur "Resolution 4409"
      | id        | 00000000-0000-0000-0000-000000044092 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | cible     | 00000000-0000-0000-0000-000000044091 |
    And il est "2044-01-14T18:00:00Z"
    And j'annule l'evenement 0 de "Resolution 4409"
      | motif | Ouverture erronee |
    When je prepare la resolution du conflit de "Resolution 4409" ancre 1
      | kind     | ANNULATION                        |
      | pointage | 1                                 |
      | motif    | La fin vise une ouverture annulee |
    Then l'apercu n'invente aucune activite
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And la lecture ne porte aucune periode pour "Resolution 4409"

  Scenario: Resolution reelle rattache la transition a B et conserve le trou
    Given il est "2044-01-15T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4410"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4410              |
    And j'ai engage l'element "Resolution 4410" en atelier
    And il est "2044-01-15T08:00:00Z"
    And j'ai pointe sur "Resolution 4410"
      | id        | 00000000-0000-0000-0000-000000044101 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-15T22:00:00Z"
    And j'ai pointe sur "Resolution 4410"
      | id        | 00000000-0000-0000-0000-000000044102 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-15T23:00:00Z"
    And j'ai pointe sur "Resolution 4410"
      | id        | 00000000-0000-0000-0000-000000044103 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044101 |
    And il est "2044-01-16T00:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4410" ancre 0
      | kind      | CORRECTION                                         |
      | pointage  | 2                                                  |
      | motif     | La transition vise la relance de vingt deux heures |
      | type      | NON_CONFORMITE                                     |
      | intention | TRANSITION                                         |
      | cible     | 1                                                  |
      | instant   | 2044-01-15T23:00:00Z                               |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     |
      | TRAVAIL        | ECHUE    |
      | TRAVAIL        | TERMINEE |
      | NON_CONFORMITE | EN_COURS |
    And la correction conserve l'identite de l'activite ouverte
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And l'apercu conserve le trou entre "2044-01-15T21:00:00Z" et "2044-01-15T22:00:00Z"

  Scenario: Resolution reelle preserve la fin regularisee apres annulation contradictoire
    Given il est "2044-01-16T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4411"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4411              |
    And j'ai engage l'element "Resolution 4411" en atelier
    And il est "2044-01-16T08:00:00Z"
    And j'ai pointe sur "Resolution 4411"
      | id        | 00000000-0000-0000-0000-000000044111 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-16T22:00:00Z"
    And j'ai pointe sur "Resolution 4411"
      | id        | 00000000-0000-0000-0000-000000044112 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044111 |
    And il est "2044-01-17T01:00:00Z"
    And je regularise sur "Resolution 4411" en visant l'activite de l'evenement 0
      | type           | FIN                  |
      | intention      | FIN                  |
      | operateur      | dupont-resolution    |
      | poste          | fraiseuse-resolution |
      | dateDeSurvenue | 2044-01-16T23:00:00Z |
    When je prepare la resolution du conflit de "Resolution 4411" ancre 0
      | kind     | ANNULATION                      |
      | pointage | 1                               |
      | motif    | La fin regularisee est correcte |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree |
      | TRAVAIL   | TERMINEE | PT15H |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4411" ont les durees
      | PT15H |

  Scenario: Resolution reelle corrige la fin regularisee apres relance vers B
    Given il est "2044-01-17T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4412"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4412              |
    And j'ai engage l'element "Resolution 4412" en atelier
    And il est "2044-01-17T08:00:00Z"
    And j'ai pointe sur "Resolution 4412"
      | id        | 00000000-0000-0000-0000-000000044121 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-17T22:00:00Z"
    And j'ai pointe sur "Resolution 4412"
      | id        | 00000000-0000-0000-0000-000000044122 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-18T01:00:00Z"
    And je regularise sur "Resolution 4412" en visant l'activite de l'evenement 0
      | type           | FIN                  |
      | intention      | FIN                  |
      | operateur      | dupont-resolution    |
      | poste          | fraiseuse-resolution |
      | dateDeSurvenue | 2044-01-17T23:00:00Z |
    When je prepare la resolution du conflit de "Resolution 4412" ancre 0
      | kind      | CORRECTION                            |
      | pointage  | 2                                     |
      | motif     | La fin regularisee termine la relance |
      | type      | FIN                                   |
      | intention | FIN                                   |
      | cible     | 1                                     |
      | instant   | 2044-01-17T23:00:00Z                  |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree |
      | TRAVAIL   | ECHUE    | PT13H |
      | TRAVAIL   | TERMINEE | PT1H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4412" ont les durees
      | PT13H |
      | PT1H  |

  Scenario: Resolution reelle en deux actes conserve la cloture et accepte un conflit intermediaire
    Given il est "2044-01-18T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4413"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4413              |
    And j'ai engage l'element "Resolution 4413" en atelier
    And il est "2044-01-18T08:00:00Z"
    And j'ai pointe sur "Resolution 4413"
      | id        | 00000000-0000-0000-0000-000000044131 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-18T17:00:00Z"
    And j'ai pointe sur "Resolution 4413"
      | id        | 00000000-0000-0000-0000-000000044132 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044131 |
    And il est "2044-01-18T18:00:00Z"
    And j'ai cloture "Resolution 4413"
      | dateDeSurvenue | 2044-01-18T18:00:00Z |
    And il est "2044-01-19T09:00:00Z"
    And je regularise sur "Resolution 4413" en visant l'activite de l'evenement 0
      | type           | NON_CONFORMITE       |
      | intention      | TRANSITION           |
      | operateur      | dupont-resolution    |
      | poste          | fraiseuse-resolution |
      | dateDeSurvenue | 2044-01-18T12:00:00Z |
    When je prepare la resolution du conflit de "Resolution 4413" ancre 0
      | kind      | REGULARISATION       |
      | pointage  | 1                    |
      | type      | DEBUT                |
      | intention | TRANSITION           |
      | cible     | 1                    |
      | instant   | 2044-01-18T14:00:00Z |
    Then l'apercu conserve 1 sequence en conflit
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 1 sequences en conflit
    And la cloture du suivi reste acquise apres cet acte
    When je prepare la resolution du conflit de "Resolution 4413" ancre 0
      | kind      | CORRECTION                                  |
      | pointage  | 3                                           |
      | motif     | La fin termine la reprise a quatorze heures |
      | type      | FIN                                         |
      | intention | FIN                                         |
      | cible     | 2                                           |
      | instant   | 2044-01-18T17:00:00Z                        |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT2H  |
      | TRAVAIL        | TERMINEE | PT3H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And la cloture du suivi reste acquise apres cet acte
    And les periodes relues de "Resolution 4413" ont les durees
      | PT4H |
      | PT2H |
      | PT3H |

  Scenario: Resolution reelle fin correctement ciblee sur NC
    Given il est "2044-01-20T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4420"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4420              |
    And j'ai engage l'element "Resolution 4420" en atelier
    And il est "2044-01-20T08:00:00Z"
    And j'ai pointe sur "Resolution 4420"
      | id        | 00000000-0000-0000-0000-000000044201 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-20T12:00:00Z"
    And j'ai pointe sur "Resolution 4420"
      | id        | 00000000-0000-0000-0000-000000044202 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044201 |
    And il est "2044-01-20T17:00:00Z"
    And j'ai pointe sur "Resolution 4420"
      | id        | 00000000-0000-0000-0000-000000044203 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044202 |
    And il est "2044-01-21T09:00:00Z"
    Then la liste des conflits de "Resolution 4420" reste vide
    When je consulte "Resolution 4420"
    Then le journal du suivi contient 3 evenements

  Scenario: Resolution reelle fin recue le lendemain survenue avant echeance
    Given il est "2044-01-21T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4421"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4421              |
    And j'ai engage l'element "Resolution 4421" en atelier
    And il est "2044-01-21T08:00:00Z"
    And j'ai pointe sur "Resolution 4421"
      | id        | 00000000-0000-0000-0000-000000044211 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-22T09:00:00Z"
    And j'ai pointe sur "Resolution 4421"
      | id             | 00000000-0000-0000-0000-000000044212 |
      | type           | FIN                                  |
      | intention      | FIN                                  |
      | operateur      | dupont-resolution                    |
      | poste          | fraiseuse-resolution                 |
      | cible          | 00000000-0000-0000-0000-000000044211 |
      | dateDeSurvenue | 2044-01-21T17:00:00Z                 |
    And il est "2044-01-22T09:00:00Z"
    Then la liste des conflits de "Resolution 4421" reste vide
    When je consulte "Resolution 4421"
    Then le journal du suivi contient 2 evenements

  Scenario: Resolution reelle transition depuis une cible seulement echue
    Given il est "2044-01-22T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4422"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4422              |
    And j'ai engage l'element "Resolution 4422" en atelier
    And il est "2044-01-22T08:00:00Z"
    And j'ai pointe sur "Resolution 4422"
      | id        | 00000000-0000-0000-0000-000000044221 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-22T23:00:00Z"
    And j'ai pointe sur "Resolution 4422"
      | id        | 00000000-0000-0000-0000-000000044222 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044221 |
    And il est "2044-01-23T09:00:00Z"
    Then la liste des conflits de "Resolution 4422" reste vide
    When je consulte "Resolution 4422"
    Then le journal du suivi contient 2 evenements

  Scenario: Resolution reelle fin automatique isolee sans fait synthetique
    Given il est "2044-01-23T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4423"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4423              |
    And j'ai engage l'element "Resolution 4423" en atelier
    And il est "2044-01-23T08:00:00Z"
    And j'ai pointe sur "Resolution 4423"
      | id        | 00000000-0000-0000-0000-000000044231 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-24T09:00:00Z"
    Then la liste des conflits de "Resolution 4423" reste vide
    When je consulte "Resolution 4423"
    Then le journal du suivi contient 1 evenements

  Scenario: Resolution reelle sur deux postes preserve les faits independants
    Given il est "2044-01-24T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4430"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4430              |
    And j'ai engage l'element "Resolution 4430" en atelier
    And il est "2044-01-24T08:00:00Z"
    And j'ai pointe sur "Resolution 4430"
      | id        | 00000000-0000-0000-0000-000000044301 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-24T08:00:00Z"
    And j'ai pointe sur "Resolution 4430"
      | id        | 00000000-0000-0000-0000-000000044302 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | rectifieuse-resolution               |
    And il est "2044-01-24T12:00:00Z"
    And j'ai pointe sur "Resolution 4430"
      | id        | 00000000-0000-0000-0000-000000044303 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044301 |
    And il est "2044-01-24T12:00:00Z"
    And j'ai pointe sur "Resolution 4430"
      | id        | 00000000-0000-0000-0000-000000044304 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | rectifieuse-resolution               |
      | cible     | 00000000-0000-0000-0000-000000044302 |
    And il est "2044-01-24T17:00:00Z"
    And j'ai pointe sur "Resolution 4430"
      | id        | 00000000-0000-0000-0000-000000044305 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044301 |
    And il est "2044-01-24T17:00:00Z"
    And j'ai pointe sur "Resolution 4430"
      | id        | 00000000-0000-0000-0000-000000044306 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | rectifieuse-resolution               |
      | cible     | 00000000-0000-0000-0000-000000044302 |
    And il est "2044-01-24T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4430" ancre 0
      | kind     | ANNULATION                                |
      | pointage | 2                                         |
      | motif    | Cette transition seulement est une erreur |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree |
      | TRAVAIL   | TERMINEE | PT9H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 1 sequences en conflit
    And les lignes restantes portent la revision commune
    And les faits independants de cet acte restent identiques
      | 1 |
      | 3 |
      | 5 |

  Scenario: Resolution reelle de deux sequences disjointes garde une revision commune
    Given il est "2044-01-26T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4431"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4431              |
    And j'ai engage l'element "Resolution 4431" en atelier
    And il est "2044-01-26T08:00:00Z"
    And j'ai pointe sur "Resolution 4431"
      | id        | 00000000-0000-0000-0000-000000044311 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-26T12:00:00Z"
    And j'ai pointe sur "Resolution 4431"
      | id        | 00000000-0000-0000-0000-000000044312 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044311 |
    And il est "2044-01-26T17:00:00Z"
    And j'ai pointe sur "Resolution 4431"
      | id        | 00000000-0000-0000-0000-000000044313 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044311 |
    And il est "2044-01-27T08:00:00Z"
    And j'ai pointe sur "Resolution 4431"
      | id        | 00000000-0000-0000-0000-000000044314 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-27T12:00:00Z"
    And j'ai pointe sur "Resolution 4431"
      | id        | 00000000-0000-0000-0000-000000044315 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044314 |
    And il est "2044-01-27T17:00:00Z"
    And j'ai pointe sur "Resolution 4431"
      | id        | 00000000-0000-0000-0000-000000044316 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044314 |
    And il est "2044-01-27T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4431" ancre 0
      | kind     | ANNULATION                                |
      | pointage | 1                                         |
      | motif    | Cette transition seulement est une erreur |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree |
      | TRAVAIL   | TERMINEE | PT9H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 1 sequences en conflit
    And les lignes restantes portent la revision commune
    And les faits independants de cet acte restent identiques
      | 3 |
      | 4 |
      | 5 |

  Scenario: Resolution reelle corrige louvrant deux fois sans changer son activite ni rediriger ses ancres
    Given il est "2044-01-28T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4432"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4432              |
    And j'ai engage l'element "Resolution 4432" en atelier
    And il est "2044-01-28T08:00:00Z"
    And j'ai pointe sur "Resolution 4432"
      | id        | 00000000-0000-0000-0000-000000044321 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-28T12:00:00Z"
    And j'ai pointe sur "Resolution 4432"
      | id        | 00000000-0000-0000-0000-000000044322 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044321 |
    And il est "2044-01-28T17:00:00Z"
    And j'ai pointe sur "Resolution 4432"
      | id        | 00000000-0000-0000-0000-000000044323 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044321 |
    And il est "2044-01-28T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4432" ancre 0
      | kind      | CORRECTION                   |
      | pointage  | 0                            |
      | motif     | Le debut etait a neuf heures |
      | type      | DEBUT                        |
      | intention | OUVERTURE                    |
      | instant   | 2044-01-28T09:00:00Z         |
    Then l'apercu conserve 1 sequence en conflit
    And la correction conserve l'identite de l'activite ouverte
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 1 sequences en conflit
    And l'ancre corrigee est explicitement annulee
    When je prepare la resolution du conflit de "Resolution 4432" ancre 1
      | kind      | CORRECTION                              |
      | pointage  | 1                                       |
      | motif     | Le debut exact etait huit heures trente |
      | type      | DEBUT                                   |
      | intention | OUVERTURE                               |
      | instant   | 2044-01-28T08:30:00Z                    |
    Then l'apercu conserve 1 sequence en conflit
    And la correction conserve l'identite de l'activite ouverte
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And l'ancre corrigee est explicitement annulee
    When je prepare la resolution du conflit de "Resolution 4432" ancre 1
      | kind      | CORRECTION                       |
      | pointage  | 4                                |
      | motif     | La fin termine la non conformite |
      | type      | FIN                              |
      | intention | FIN                              |
      | cible     | 3                                |
      | instant   | 2044-01-28T17:00:00Z             |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree   |
      | TRAVAIL        | TERMINEE | PT3H30M |
      | NON_CONFORMITE | TERMINEE | PT5H    |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4432" ont les durees
      | PT3H30M |
      | PT5H    |

  Scenario: Resolution reelle preserve une nanoseconde et distingue le rejeu UUID
    Given il est "2044-01-30T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4433"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4433              |
    And j'ai engage l'element "Resolution 4433" en atelier
    And il est "2044-01-30T08:00:00.123456789Z"
    And j'ai pointe sur "Resolution 4433"
      | id        | 00000000-0000-0000-0000-000000044331 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-30T08:00:00.123456790Z"
    And j'ai pointe sur "Resolution 4433"
      | id        | 00000000-0000-0000-0000-000000044332 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044331 |
    And il est "2044-01-30T08:00:00.123456791Z"
    And j'ai pointe sur "Resolution 4433"
      | id        | 00000000-0000-0000-0000-000000044333 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044331 |
    And il est "2044-01-30T18:00:00Z"
    When je rejoue le dernier geste du pupitre
    Then la reponse a le statut http 200
    When je consulte "Resolution 4433"
    Then le journal du suivi contient 3 evenements
    When je prepare la resolution du conflit de "Resolution 4433" ancre 0
      | kind     | ANNULATION                              |
      | pointage | 2                                       |
      | motif    | La seconde fin distincte est une erreur |
    Then l'apercu donne les activites de resolution
      | categorie | etat     | duree          |
      | TRAVAIL   | TERMINEE | PT0.000000001S |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4433" ont les durees
      | PT0.000000001S |
    And les API lecteurs conservent la duree nanoseconde dans la semaine 4

  Scenario: Resolution reelle interprete la NC recue apres la fin a son heure metier
    Given il est "2044-01-29T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4434"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4434              |
    And j'ai engage l'element "Resolution 4434" en atelier
    And il est "2044-01-29T08:00:00Z"
    And j'ai pointe sur "Resolution 4434"
      | id        | 00000000-0000-0000-0000-000000044341 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-29T17:00:00Z"
    And j'ai pointe sur "Resolution 4434"
      | id        | 00000000-0000-0000-0000-000000044342 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044341 |
    And il est "2044-01-29T23:00:00Z"
    And j'ai pointe sur "Resolution 4434"
      | id             | 00000000-0000-0000-0000-000000044343 |
      | type           | NON_CONFORMITE                       |
      | intention      | TRANSITION                           |
      | operateur      | dupont-resolution                    |
      | poste          | fraiseuse-resolution                 |
      | cible          | 00000000-0000-0000-0000-000000044341 |
      | dateDeSurvenue | 2044-01-29T12:00:00Z                 |
    And il est "2044-01-29T23:30:00Z"
    When je prepare la resolution du conflit de "Resolution 4434" ancre 0
      | kind      | CORRECTION                               |
      | pointage  | 2                                        |
      | motif     | La fin cible la NC meme recue hors ordre |
      | type      | FIN                                      |
      | intention | FIN                                      |
      | cible     | 1                                        |
      | instant   | 2044-01-29T17:00:00Z                     |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT5H  |
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4434" ont les durees
      | PT4H |
      | PT5H |

  Scenario: Resolution reelle refuse les autres droits acteurs et tenants avant confirmation et au rejeu
    Given il est "2044-01-31T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4440"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4440              |
    And j'ai engage l'element "Resolution 4440" en atelier
    And il est "2044-01-31T08:00:00Z"
    And j'ai pointe sur "Resolution 4440"
      | id        | 00000000-0000-0000-0000-000000044401 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-31T12:00:00Z"
    And j'ai pointe sur "Resolution 4440"
      | id        | 00000000-0000-0000-0000-000000044402 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044401 |
    And il est "2044-01-31T17:00:00Z"
    And j'ai pointe sur "Resolution 4440"
      | id        | 00000000-0000-0000-0000-000000044403 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044401 |
    And il est "2044-01-31T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4440" ancre 0
      | kind      | CORRECTION           |
      | pointage  | 2                    |
      | motif     | La fin termine la NC |
      | type      | FIN                  |
      | intention | FIN                  |
      | cible     | 1                    |
      | instant   | 2044-01-31T17:00:00Z |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT5H  |
    Given I am logged in as "gestionnaire" with role "USER"
    When je tente un nouvel apercu avec statut 403
    And je tente la confirmation de cet apercu avec statut 403
    And je tente la verification de cette commande avec statut 403
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" without tenant
    When je tente un nouvel apercu avec statut 403
    And je tente la confirmation de cet apercu avec statut 403
    And je tente la verification de cette commande avec statut 403
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je tente un nouvel apercu avec statut 404
    And je tente la confirmation de cet apercu avec statut 404
    And la commande reste non attestee pour cet autre tenant
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4440" ont les durees
      | PT4H |
      | PT5H |
    Given I am logged in as "gestionnaire" with role "USER"
    When je tente un nouvel apercu avec statut 403
    And je tente la confirmation de cet apercu avec statut 403
    And je tente la verification de cette commande avec statut 403
    Given I am logged in as "autre-gestionnaire" with role "GESTIONNAIRE"
    When je tente la confirmation de cet apercu avec statut 409
    And je tente la verification de cette commande avec statut 409
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" without tenant
    When je tente un nouvel apercu avec statut 403
    And je tente la confirmation de cet apercu avec statut 403
    And je tente la verification de cette commande avec statut 403
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "katilys"
    When je tente un nouvel apercu avec statut 404
    And je tente la confirmation de cet apercu avec statut 404
    And la commande reste non attestee pour cet autre tenant
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE"
    Then le recu canonique conserve les memes identites et activites
    And le suivi garde exactement les faits confirmes et leur revision

  Scenario: Resolution reelle refuse les motifs blancs et le futur minimal sans ecriture
    Given il est "2044-01-31T07:00:00Z"
    And l'entreprise a cree l'element de fabrication "Resolution 4441"
      | type      | ORDRE_DE_FABRICATION |
      | reference | RES4441              |
    And j'ai engage l'element "Resolution 4441" en atelier
    And il est "2044-01-31T08:00:00Z"
    And j'ai pointe sur "Resolution 4441"
      | id        | 00000000-0000-0000-0000-000000044411 |
      | type      | DEBUT                                |
      | intention | OUVERTURE                            |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
    And il est "2044-01-31T12:00:00Z"
    And j'ai pointe sur "Resolution 4441"
      | id        | 00000000-0000-0000-0000-000000044412 |
      | type      | NON_CONFORMITE                       |
      | intention | TRANSITION                           |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044411 |
    And il est "2044-01-31T17:00:00Z"
    And j'ai pointe sur "Resolution 4441"
      | id        | 00000000-0000-0000-0000-000000044413 |
      | type      | FIN                                  |
      | intention | FIN                                  |
      | operateur | dupont-resolution                    |
      | poste     | fraiseuse-resolution                 |
      | cible     | 00000000-0000-0000-0000-000000044411 |
    And il est "2044-01-31T18:00:00Z"
    When je prepare la resolution du conflit de "Resolution 4441" ancre 0
      | kind      | CORRECTION           |
      | pointage  | 2                    |
      | motif     | La fin termine la NC |
      | type      | FIN                  |
      | intention | FIN                  |
      | cible     | 1                    |
      | instant   | 2044-01-31T17:00:00Z |
    Then l'apercu donne les activites de resolution
      | categorie      | etat     | duree |
      | TRAVAIL        | TERMINEE | PT4H  |
      | NON_CONFORMITE | TERMINEE | PT5H  |
    And les apercus invalides restent refuses sans ecriture
    And l'apercu ne modifie ni les faits ni les projections ni la revision
    When je confirme cet apercu de resolution
    Then le recu canonique conserve les memes identites et activites
    And la liste de ce suivi conserve 0 sequences en conflit
    And les periodes relues de "Resolution 4441" ont les durees
      | PT4H |
      | PT5H |
