package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une saisie rattrapee : elle porte l'operateur dont le temps est affecte, et l'auteur qui la saisit, qui ne sont pas
 * necessairement la meme personne.
 */
public record RegularisationAEnregistrer(
  SuiviDAtelierId suivi,
  TypeDEvenementDAtelier type,
  Operateur operateur,
  Optional<PosteDeTravail> poste,
  Auteur auteur,
  Instant dateDeSurvenue
) {
  public RegularisationAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("type", type);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  public static RegularisationAEnregistrerSuiviBuilder builder() {
    return suivi ->
      type ->
        operateur ->
          poste -> auteur -> dateDeSurvenue -> new RegularisationAEnregistrer(suivi, type, operateur, poste, auteur, dateDeSurvenue);
  }

  public interface RegularisationAEnregistrerSuiviBuilder {
    RegularisationAEnregistrerTypeBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface RegularisationAEnregistrerTypeBuilder {
    RegularisationAEnregistrerOperateurBuilder type(TypeDEvenementDAtelier type);
  }

  public interface RegularisationAEnregistrerOperateurBuilder {
    RegularisationAEnregistrerPosteBuilder operateur(Operateur operateur);
  }

  public interface RegularisationAEnregistrerPosteBuilder {
    RegularisationAEnregistrerAuteurBuilder poste(Optional<PosteDeTravail> poste);
  }

  public interface RegularisationAEnregistrerAuteurBuilder {
    RegularisationAEnregistrerDateDeSurvenueBuilder auteur(Auteur auteur);
  }

  public interface RegularisationAEnregistrerDateDeSurvenueBuilder {
    RegularisationAEnregistrer dateDeSurvenue(Instant dateDeSurvenue);
  }
}
