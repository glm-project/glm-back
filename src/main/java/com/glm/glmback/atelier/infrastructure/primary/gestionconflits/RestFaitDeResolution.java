package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.infrastructure.primary.CoherenceDuGeste;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

record RestFaitDeResolution(
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TypeDEvenementDAtelier type,
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IntentionDePointage intention,
  UUID activiteVisee,
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  UUID poste,
  @NotBlank
  @Schema(
    requiredMode = Schema.RequiredMode.REQUIRED,
    format = "date-time",
    description = "Instant saisi, conserve avec son decalage et ses decimales."
  )
  String instant
) {
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = CoherenceDuGeste.INTENTION_ADMISE)
  boolean isIntentionAdmiseParLeType() {
    return CoherenceDuGeste.intentionAdmise(type, intention);
  }

  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = CoherenceDuGeste.CIBLE_CONFORME)
  boolean isCibleConformeALIntention() {
    return CoherenceDuGeste.cibleConforme(intention, activiteVisee);
  }

  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "l'instant du fait doit respecter le format ISO-8601")
  boolean isInstantValide() {
    if (instant == null || instant.isBlank()) {
      return true;
    }
    try {
      Instant.parse(instant);
      return true;
    } catch (DateTimeParseException exception) {
      return false;
    }
  }

  static RestFaitDeResolution from(RegularisationAEnregistrer commande, String instant) {
    return new RestFaitDeResolution(
      commande.type(),
      commande.intention(),
      commande.activiteVisee().map(ActiviteId::uuid).orElse(null),
      commande.operateur().uuid(),
      commande.poste().map(PosteDeTravailId::uuid).orElse(null),
      instant
    );
  }

  RegularisationAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .intention(intention)
      .activiteVisee(Optional.ofNullable(activiteVisee).map(ActiviteId::new))
      .operateur(new OperateurId(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravailId::new))
      .auteur(auteur)
      .dateDeSurvenue(Instant.parse(instant));
  }
}
