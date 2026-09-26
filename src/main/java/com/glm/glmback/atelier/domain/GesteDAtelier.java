package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record GesteDAtelier(
  SuiviDAtelierId suivi,
  OperateurId operateur,
  TypeDEvenementDAtelier type,
  Optional<PosteDeTravailId> poste,
  Optional<Instant> dateDeclaree
) implements GesteEnAttente {
  public GesteDAtelier {
    Assert.notNull("suivi", suivi);
    Assert.notNull("operateur", operateur);
    Assert.notNull("type", type);
    Assert.notNull("poste", poste);
    Assert.notNull("date declaree", dateDeclaree);
  }

  /**
   * Le geste, applique au nom du gestionnaire : une regularisation a la date retenue.
   */
  public RegularisationAEnregistrer regularisation(Auteur auteur, Instant dateDeSurvenue) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(operateur)
      .poste(poste)
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);
  }

  public static GesteDAtelierSuiviBuilder builder() {
    return suivi -> operateur -> type -> poste -> dateDeclaree -> new GesteDAtelier(suivi, operateur, type, poste, dateDeclaree);
  }

  public interface GesteDAtelierSuiviBuilder {
    GesteDAtelierOperateurBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface GesteDAtelierOperateurBuilder {
    GesteDAtelierTypeBuilder operateur(OperateurId operateur);
  }

  public interface GesteDAtelierTypeBuilder {
    GesteDAtelierPosteBuilder type(TypeDEvenementDAtelier type);
  }

  public interface GesteDAtelierPosteBuilder {
    GesteDAtelierDateDeclareeBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface GesteDAtelierDateDeclareeBuilder {
    GesteDAtelier dateDeclaree(Optional<Instant> dateDeclaree);
  }
}
