package com.glm.glmback.atelier.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "La sequence autoritaire contenant l'ancre demandee, avec sa cle brute et les seuls faits de son perimetre.")
final class RestSequenceDuDossier {

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final UUID operateurId;

  @JsonProperty
  private final RestOperateur operateur;

  @JsonProperty
  private final UUID posteId;

  @JsonProperty
  private final RestPosteDeTravail poste;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final List<UUID> activites;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final List<UUID> pointages;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final Instant datePremierPointage;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final int nombrePointages;

  private RestSequenceDuDossier(LectureDossierConflit dossier, AnnuaireDAtelier annuaire) {
    var sequence = dossier.sequence().orElseThrow();
    operateurId = sequence.operateur().uuid();
    operateur = RestOperateur.resolu(annuaire, sequence.operateur());
    posteId = sequence.poste().map(PosteDeTravailId::uuid).orElse(null);
    poste = RestPosteDeTravail.resolu(annuaire, sequence.poste());
    activites = sequence.activites().stream().map(ActiviteId::uuid).toList();
    pointages = sequence.pointages().stream().map(EvenementDAtelierId::uuid).toList();
    datePremierPointage = dossier.lecture().suivi().journal().evenement(sequence.pointages().getFirst()).orElseThrow().dateDeSurvenue();
    nombrePointages = sequence.pointages().size();
  }

  static RestSequenceDuDossier from(LectureDossierConflit dossier, AnnuaireDAtelier annuaire) {
    return dossier
      .sequence()
      .map(sequence -> new RestSequenceDuDossier(dossier, annuaire))
      .orElse(null);
  }
}
