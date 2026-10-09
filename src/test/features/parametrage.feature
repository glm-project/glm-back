Feature: Parametrage de l'entreprise

  # Un seul jeu de reglages par entreprise : les scenarios qui le modifient le font dans l'entreprise
  # "parametrage_fixture", pour ne pas changer les reglages des autres features. "parametrage_vierge" n'est jamais
  # modifiee : elle garde les valeurs par defaut.
  Scenario: Une entreprise neuve a une duree max d'activite de treize heures
    Given I am logged in as "user" with role "USER" for tenant "parametrage_vierge"
    When je lis le parametrage de l'entreprise
    Then la reponse a le statut http 200
    And la duree max d'une activite vaut "PT13H"

  Scenario: Lecture refusee a un administrateur technique
    Given I am logged in as "admin" with role "ADMIN" for tenant "parametrage_vierge"
    When je lis le parametrage de l'entreprise
    Then la reponse a le statut http 403

  Scenario: Le gestionnaire fixe la duree max d'une activite
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    When je fixe la duree max d'une activite a "PT10H"
    Then la reponse a le statut http 200
    And la duree max d'une activite vaut "PT10H"
    When je lis le parametrage de l'entreprise
    Then la duree max d'une activite vaut "PT10H"

  Scenario: Un utilisateur lit la duree max fixee par le gestionnaire
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    And j'ai fixe la duree max d'une activite a "PT8H30M"
    And I am logged in as "user" with role "USER" for tenant "parametrage_fixture"
    When je lis le parametrage de l'entreprise
    Then la reponse a le statut http 200
    And la duree max d'une activite vaut "PT8H30M"

  Scenario Outline: Duree max refusee hors des bornes ou illisible
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    When je fixe la duree max d'une activite a "<duree>"
    Then la reponse a le statut http 400

    Examples:
      | duree   |
      | PT59M   |
      | PT24H1S |
      | treize  |

  Scenario: Duree max acceptee sur ses bornes
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    When je fixe la duree max d'une activite a "PT1H"
    Then la reponse a le statut http 200
    When je fixe la duree max d'une activite a "PT24H"
    Then la reponse a le statut http 200

  Scenario: Modification refusee a un utilisateur simple
    Given I am logged in as "user" with role "USER" for tenant "parametrage_fixture"
    When je fixe la duree max d'une activite a "PT10H"
    Then la reponse a le statut http 403

  Scenario Outline: Le gestionnaire depose un logo PNG ou JPEG de 50 x 50
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    When je depose comme logo "<fichier>"
    Then la reponse a le statut http 200
    And le logo depose a une version

    Examples:
      | fichier            |
      | un PNG de 50 x 50  |
      | un JPEG de 50 x 50 |

  Scenario Outline: Logo refuse
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    When je depose comme logo "<fichier>"
    Then la reponse a le statut http 400
    And la reponse porte le code d'erreur "urn:glm:erreur:parametrage:logo-invalide"
    And le refus du logo dit "<raison>"

    Examples:
      | fichier                    | raison                                                |
      | un GIF de 50 x 50          | Le logo doit etre une image PNG ou JPEG (recu : gif)  |
      | un PNG de 120 x 80         | Le logo doit mesurer 50 x 50 pixels (recu : 120 x 80) |
      | un PNG de 50 x 50 de 25 Ko | Le logo pese 25600 octets, au plus 20480              |
      | un fichier texte           | Le fichier n'est pas une image lisible                |

  Scenario: Depot du logo refuse a un utilisateur simple
    Given I am logged in as "user" with role "USER" for tenant "parametrage_fixture"
    When je depose comme logo "un PNG de 50 x 50"
    Then la reponse a le statut http 403

  Scenario: Une entreprise neuve n'a pas de logo
    Given I am logged in as "user" with role "USER" for tenant "parametrage_vierge"
    When je lis le parametrage de l'entreprise
    Then la reponse a le statut http 200
    And le parametrage n'a pas de logo

  Scenario: Le parametrage donne la version du logo depose
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    When je depose comme logo "un PNG de 50 x 50"
    Then le parametrage porte la version du logo depose

  Scenario Outline: Le logo se lit a sa version, garde en cache
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    And j'ai depose comme logo "<fichier>"
    And I am logged in as "user" with role "USER" for tenant "parametrage_fixture"
    When je lis le logo a sa version
    Then la reponse a le statut http 200
    And le logo est servi en "<type>" et garde en cache

    Examples:
      | fichier            | type       |
      | un PNG de 50 x 50  | image/png  |
      | un JPEG de 50 x 50 | image/jpeg |

  Scenario: Un nouveau logo change d'adresse, l'ancienne ne rend plus rien
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    And j'ai depose comme logo "un PNG de 50 x 50"
    And je retiens la version du logo
    And j'ai depose comme logo "un JPEG de 50 x 50"
    When je lis le logo a la version retenue
    Then la reponse a le statut http 404
    And la reponse porte le code d'erreur "urn:glm:erreur:parametrage:logo-introuvable"

  Scenario: Version de logo hors motif
    Given I am logged in as "user" with role "USER" for tenant "parametrage_fixture"
    When je lis le logo a la version "LOGO"
    Then la reponse a le statut http 400

  Scenario: Le gestionnaire retire le logo
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    And j'ai depose comme logo "un PNG de 50 x 50"
    And je retiens la version du logo
    When je retire le logo
    Then la reponse a le statut http 204
    When je lis le parametrage de l'entreprise
    Then le parametrage n'a pas de logo
    When je lis le logo a la version retenue
    Then la reponse a le statut http 404

  Scenario: Retirer un logo absent est sans effet
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "parametrage_fixture"
    And j'ai depose comme logo "un PNG de 50 x 50"
    And je retire le logo
    When je retire le logo
    Then la reponse a le statut http 204

  Scenario: Retrait du logo refuse a un utilisateur simple
    Given I am logged in as "user" with role "USER" for tenant "parametrage_fixture"
    When je retire le logo
    Then la reponse a le statut http 403
