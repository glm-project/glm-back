Feature: La duree max d'une activite fixee par le gestionnaire gouverne l'atelier et le pupitre

  # La duree max d'une activite est un reglage de l'entreprise. Ces scenarios vivent dans l'entreprise
  # "duree_parametree", dont aucune autre feature ne lit les reglages : chaque scenario y fixe sa propre duree avant tout
  # geste, parce que les reglages d'une entreprise survivent a un scenario.
  Background:
    Given I am logged in as "gestionnaire" with role "GESTIONNAIRE" for tenant "duree_parametree"

  Scenario: Le referentiel du pupitre porte la duree que le gestionnaire a fixee
    Given j'ai fixe la duree max d'une activite a "PT8H"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then la reponse a le statut http 200
    And le referentiel du pupitre porte la duree maximale d'activite "PT8H"

  Scenario: Le referentiel suit un changement de la duree sans attendre
    Given j'ai fixe la duree max d'une activite a "PT10H"
    When je lis le referentiel du pupitre a "2026-05-11T07:00:00Z"
    Then le referentiel du pupitre porte la duree maximale d'activite "PT10H"
    Given j'ai fixe la duree max d'une activite a "PT8H30M"
    When je lis le referentiel du pupitre a "2026-05-11T07:05:00Z"
    Then le referentiel du pupitre porte la duree maximale d'activite "PT8H30M"
