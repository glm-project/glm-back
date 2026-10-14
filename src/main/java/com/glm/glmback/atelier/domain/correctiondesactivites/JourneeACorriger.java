package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * La journee d'un operateur telle que le gestionnaire la corrige : les minuits du jour, les activites qui le touchent,
 * leur duree operationnelle, et de quoi en ajouter une.
 *
 * <p>
 * La duree operationnelle est la somme des portions du jour des activites terminees, reelles ou automatiques, sans union
 * des intervalles : deux activites menees de front comptent chacune. Une activite en cours ne compte pas. Ce n'est pas un
 * agregat : c'est la lecture composee de la page, calculee a un instant d'evaluation que l'appelant fournit.
 * </p>
 */
public record JourneeACorriger(
  OperateurConnu operateur,
  LocalDate jour,
  BornesDuJour bornes,
  Duration dureeOperationnelle,
  List<ActiviteDeLaJournee> activites,
  ChoixDAjout choix,
  List<FicheDElement> fiches,
  Instant evaluation
) {
  public JourneeACorriger {
    Assert.notNull("operateur", operateur);
    Assert.notNull("jour", jour);
    Assert.notNull("bornes", bornes);
    Assert.notNull("duree operationnelle", dureeOperationnelle);
    Assert.notNull("activites", activites);
    Assert.notNull("choix", choix);
    Assert.notNull("fiches", fiches);
    Assert.notNull("evaluation", evaluation);

    activites = List.copyOf(activites);
    fiches = List.copyOf(fiches);
  }

  public static JourneeACorrigerOperateurBuilder builder() {
    return operateur ->
      jour ->
        bornes ->
          dureeOperationnelle ->
            activites ->
              choix ->
                fiches ->
                  evaluation -> new JourneeACorriger(operateur, jour, bornes, dureeOperationnelle, activites, choix, fiches, evaluation);
  }

  /**
   * La fiche relue au referentiel de cet element, absente quand l'element a ete supprime.
   */
  public Optional<FicheDElement> fiche(ElementEngageId element) {
    return fiches
      .stream()
      .filter(fiche -> fiche.id().equals(element))
      .findFirst();
  }

  public interface JourneeACorrigerOperateurBuilder {
    JourneeACorrigerJourBuilder operateur(OperateurConnu operateur);
  }

  public interface JourneeACorrigerJourBuilder {
    JourneeACorrigerBornesBuilder jour(LocalDate jour);
  }

  public interface JourneeACorrigerBornesBuilder {
    JourneeACorrigerDureeOperationnelleBuilder bornes(BornesDuJour bornes);
  }

  public interface JourneeACorrigerDureeOperationnelleBuilder {
    JourneeACorrigerActivitesBuilder dureeOperationnelle(Duration dureeOperationnelle);
  }

  public interface JourneeACorrigerActivitesBuilder {
    JourneeACorrigerChoixBuilder activites(List<ActiviteDeLaJournee> activites);
  }

  public interface JourneeACorrigerChoixBuilder {
    JourneeACorrigerFichesBuilder choix(ChoixDAjout choix);
  }

  public interface JourneeACorrigerFichesBuilder {
    JourneeACorrigerEvaluationBuilder fiches(List<FicheDElement> fiches);
  }

  public interface JourneeACorrigerEvaluationBuilder {
    JourneeACorriger evaluation(Instant evaluation);
  }
}
