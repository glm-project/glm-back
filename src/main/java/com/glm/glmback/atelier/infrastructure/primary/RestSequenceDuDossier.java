package com.glm.glmback.atelier.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.PerimetreDeDossier;
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

  private RestSequenceDuDossier(PerimetreDeDossier sequence, LectureDuSuivi lecture, AnnuaireDAtelier annuaire) {
    operateurId = sequence.cle().operateur().uuid();
    operateur = RestOperateur.resolu(annuaire, sequence.cle().operateur());
    posteId = sequence.cle().poste().map(PosteDeTravailId::uuid).orElse(null);
    poste = RestPosteDeTravail.resolu(annuaire, sequence.cle().poste());
    activites = sequence.activites().stream().map(ActiviteId::uuid).toList();
    pointages = sequence.pointages().stream().map(EvenementDAtelierId::uuid).toList();
    datePremierPointage = lecture.suivi().journal().evenement(sequence.pointages().getFirst()).orElseThrow().dateDeSurvenue();
    nombrePointages = sequence.pointages().size();
  }

  static RestSequenceDuDossier from(LectureDossierConflit dossier, AnnuaireDAtelier annuaire) {
    return dossier
      .sequence()
      .map(sequence ->
        new RestSequenceDuDossier(
          new PerimetreDeDossier(sequence.cle(), sequence.activites(), sequence.pointages()),
          dossier.lecture(),
          annuaire
        )
      )
      .orElse(null);
  }
}
