package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.util.Optional;
import java.time.Instant;

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
