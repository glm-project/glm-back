package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.RecuDActe;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Preuve durable d un acte enregistre. Sa revision est historique ; le dossier joint est relu a l instant courant.")
record RestRecuDActe(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID commande,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierConflit adresse,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestActeDeResolution acte,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revisionDeDepart,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revisionEnregistree,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant enregistreLe,
  UUID evenementCree,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UUID> evenementsTouches
) {
  static RestRecuDActe from(RecuDActe recu) {
    var preuve = recu.preuve();
    return new RestRecuDActe(
      preuve.commande(),
      new RestAdresseDossierConflit(preuve.adresse().suivi().uuid(), preuve.adresse().pointage().uuid()),
      RestActeDeResolution.from(preuve.acte()),
      preuve.revision().value(),
      recu.revisionEnregistree().value(),
      recu.enregistreLe(),
      preuve.evenement().map(EvenementDAtelierId::uuid).orElse(null),
      recu.evenementsTouches().stream().map(EvenementDAtelierId::uuid).toList()
    );
  }
}
