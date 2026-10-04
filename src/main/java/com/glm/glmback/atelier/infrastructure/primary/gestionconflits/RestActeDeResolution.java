package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolution;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
  {
    @JsonSubTypes.Type(value = RestActeDeResolution.Annulation.class, name = "ANNULATION"),
    @JsonSubTypes.Type(value = RestActeDeResolution.Correction.class, name = "CORRECTION"),
    @JsonSubTypes.Type(value = RestActeDeResolution.Regularisation.class, name = "REGULARISATION"),
  }
)
@Schema(
  discriminatorProperty = "kind",
  discriminatorMapping = {
    @DiscriminatorMapping(value = "ANNULATION", schema = RestActeDeResolution.Annulation.class),
    @DiscriminatorMapping(value = "CORRECTION", schema = RestActeDeResolution.Correction.class),
    @DiscriminatorMapping(value = "REGULARISATION", schema = RestActeDeResolution.Regularisation.class),
  },
  oneOf = { RestActeDeResolution.Annulation.class, RestActeDeResolution.Correction.class, RestActeDeResolution.Regularisation.class }
)
sealed interface RestActeDeResolution {
  ActeDeResolution toDomain(SuiviDAtelierId suivi, Auteur auteur);

  static RestActeDeResolution from(ActeDeResolution acte) {
    if (acte instanceof ActeDeResolution.Correction correction) {
      return new Correction(
        correction.commande().evenement().uuid(),
        correction.commande().motif().value(),
        RestFaitDeResolution.from(correction.commande().remplacement(), correction.instant())
      );
    }
    if (acte instanceof ActeDeResolution.Annulation annulation) {
      return new Annulation(annulation.commande().evenement().uuid(), annulation.commande().motif().value());
    }
    var regularisation = (ActeDeResolution.Regularisation) acte;
    return new Regularisation(RestFaitDeResolution.from(regularisation.commande(), regularisation.instant()));
  }

  @Schema(name = "RestActeAnnulation")
  record Annulation(
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID pointage,
    @NotBlank @Size(max = 255) @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String motif
  ) implements RestActeDeResolution {
    @Override
    public ActeDeResolution toDomain(SuiviDAtelierId suivi, Auteur auteur) {
      return new ActeDeResolution.Annulation(
        new AnnulationAEnregistrer(suivi, new EvenementDAtelierId(pointage), auteur, new MotifDAnnulation(motif))
      );
    }
  }

  @Schema(name = "RestActeCorrection")
  record Correction(
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID pointage,
    @NotBlank @Size(max = 255) @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String motif,
    @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestFaitDeResolution fait
  ) implements RestActeDeResolution {
    @Override
    public ActeDeResolution toDomain(SuiviDAtelierId suivi, Auteur auteur) {
      return new ActeDeResolution.Correction(
        new CorrectionAEnregistrer(new EvenementDAtelierId(pointage), new MotifDAnnulation(motif), fait.toDomain(suivi, auteur)),
        fait.instant()
      );
    }
  }

  @Schema(name = "RestActeRegularisation")
  record Regularisation(
    @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestFaitDeResolution fait
  ) implements RestActeDeResolution {
    @Override
    public ActeDeResolution toDomain(SuiviDAtelierId suivi, Auteur auteur) {
      return new ActeDeResolution.Regularisation(fait.toDomain(suivi, auteur), fait.instant());
    }
  }
}
