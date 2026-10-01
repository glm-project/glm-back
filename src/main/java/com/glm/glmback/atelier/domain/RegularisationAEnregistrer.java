package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une saisie rattrapee : elle porte l'operateur dont le temps est affecte, et l'auteur qui la saisit, qui ne sont pas
 * necessairement la meme personne. Comme un pointage, elle dit son intention et, pour une transition ou une fin,
 * l'activite qu'elle vise.
 */
public record RegularisationAEnregistrer(
  SuiviDAtelierId suivi,
  TypeDEvenementDAtelier type,
  IntentionDePointage intention,
  Optional<ActiviteId> activiteVisee,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  Auteur auteur,
  Instant dateDeSurvenue
) {
  public RegularisationAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("type", type);
    Assert.notNull("intention", intention);
    Assert.notNull("activite visee", activiteVisee);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  public static RegularisationAEnregistrerSuiviBuilder builder() {
    return suivi ->
      type ->
        intention ->
          activiteVisee ->
            operateur ->
              poste ->
                auteur ->
                  dateDeSurvenue ->
                    new RegularisationAEnregistrer(suivi, type, intention, activiteVisee, operateur, poste, auteur, dateDeSurvenue);
  }

  public interface RegularisationAEnregistrerSuiviBuilder {
    RegularisationAEnregistrerTypeBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface RegularisationAEnregistrerTypeBuilder {
    RegularisationAEnregistrerIntentionBuilder type(TypeDEvenementDAtelier type);
  }

  public interface RegularisationAEnregistrerIntentionBuilder {
    RegularisationAEnregistrerActiviteViseeBuilder intention(IntentionDePointage intention);
  }

  public interface RegularisationAEnregistrerActiviteViseeBuilder {
    RegularisationAEnregistrerOperateurBuilder activiteVisee(Optional<ActiviteId> activiteVisee);
  }

  public interface RegularisationAEnregistrerOperateurBuilder {
    RegularisationAEnregistrerPosteBuilder operateur(OperateurId operateur);
  }

  public interface RegularisationAEnregistrerPosteBuilder {
    RegularisationAEnregistrerAuteurBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface RegularisationAEnregistrerAuteurBuilder {
    RegularisationAEnregistrerDateDeSurvenueBuilder auteur(Auteur auteur);
  }

  public interface RegularisationAEnregistrerDateDeSurvenueBuilder {
    RegularisationAEnregistrer dateDeSurvenue(Instant dateDeSurvenue);
  }
}
