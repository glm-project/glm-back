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
