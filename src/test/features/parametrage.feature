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
