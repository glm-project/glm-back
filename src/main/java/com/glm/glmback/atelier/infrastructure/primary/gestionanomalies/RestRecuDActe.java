package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.RecuDActe;
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
    var proposition = recu.proposition();
    return new RestRecuDActe(
      proposition.commande(),
      new RestAdresseDossierConflit(proposition.adresse().suivi().uuid(), proposition.adresse().pointage().uuid()),
      RestActeDeResolution.from(proposition.acte()),
      proposition.revision().value(),
      recu.revisionEnregistree().value(),
      recu.enregistreLe(),
      proposition.evenement().map(EvenementDAtelierId::uuid).orElse(null),
      recu.evenementsTouches().stream().map(EvenementDAtelierId::uuid).toList()
    );
  }
}
