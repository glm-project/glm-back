package com.glm.glmback.atelier.application;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Le contenu stable d'un geste du pupitre, independant de l'etat courant des agregats.
 *
 * <p>
 * Le suivi est celui d'un pointage d'element ; la presence n'en a pas.
 * </p>
 */
public record EmpreinteDEvenement(
  NatureDeGesteDuPupitre nature,
  Optional<UUID> suivi,
  UUID operateur,
  String type,
  Optional<UUID> poste,
  Optional<Instant> dateDeSurvenue
) {
  public EmpreinteDEvenement {
    Assert.notNull("nature", nature);
    Assert.notNull("suivi", suivi);
    Assert.notNull("operateur", operateur);
    Assert.notBlank("type", type);
    Assert.notNull("poste", poste);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  private EmpreinteDEvenement(Builder builder) {
    this(builder.nature, builder.suivi, builder.operateur, builder.type, builder.poste, builder.dateDeSurvenue);
  }

  public static EmpreinteDEvenementNatureBuilder builder() {
    return new Builder();
  }

  public interface EmpreinteDEvenementNatureBuilder {
    EmpreinteDEvenementSuiviBuilder nature(NatureDeGesteDuPupitre nature);
  }

  public interface EmpreinteDEvenementSuiviBuilder {
    EmpreinteDEvenementOperateurBuilder suivi(Optional<UUID> suivi);
  }

  public interface EmpreinteDEvenementOperateurBuilder {
    EmpreinteDEvenementTypeBuilder operateur(UUID operateur);
  }

  public interface EmpreinteDEvenementTypeBuilder {
    EmpreinteDEvenementPosteBuilder type(String type);
  }

  public interface EmpreinteDEvenementPosteBuilder {
    EmpreinteDEvenementDateDeSurvenueBuilder poste(Optional<UUID> poste);
  }

  public interface EmpreinteDEvenementDateDeSurvenueBuilder {
    EmpreinteDEvenement dateDeSurvenue(Optional<Instant> dateDeSurvenue);
  }

  private static final class Builder
    implements
      EmpreinteDEvenementNatureBuilder,
      EmpreinteDEvenementSuiviBuilder,
      EmpreinteDEvenementOperateurBuilder,
      EmpreinteDEvenementTypeBuilder,
      EmpreinteDEvenementPosteBuilder,
      EmpreinteDEvenementDateDeSurvenueBuilder
  {

    private NatureDeGesteDuPupitre nature;
    private Optional<UUID> suivi;
    private UUID operateur;
    private String type;
    private Optional<UUID> poste;
    private Optional<Instant> dateDeSurvenue;

    @Override
    public EmpreinteDEvenementSuiviBuilder nature(NatureDeGesteDuPupitre nature) {
      this.nature = nature;
      return this;
    }

    @Override
    public EmpreinteDEvenementOperateurBuilder suivi(Optional<UUID> suivi) {
      this.suivi = suivi;
      return this;
    }

    @Override
    public EmpreinteDEvenementTypeBuilder operateur(UUID operateur) {
      this.operateur = operateur;
      return this;
    }

    @Override
    public EmpreinteDEvenementPosteBuilder type(String type) {
      this.type = type;
      return this;
    }

    @Override
    public EmpreinteDEvenementDateDeSurvenueBuilder poste(Optional<UUID> poste) {
      this.poste = poste;
      return this;
    }

    @Override
    public EmpreinteDEvenement dateDeSurvenue(Optional<Instant> dateDeSurvenue) {
      this.dateDeSurvenue = dateDeSurvenue;
      return new EmpreinteDEvenement(this);
    }
  }
}
