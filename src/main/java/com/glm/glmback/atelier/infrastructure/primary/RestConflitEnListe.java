package com.glm.glmback.atelier.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.ConflitEnListe;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "RestConflitEnListe", description = "Une sequence en conflit issue des projections courantes, sans journal.")
final class RestConflitEnListe {

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final RestAdresseDossierConflit adresse;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final long revision;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final UUID elementId;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final String designation;

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
  private final Instant datePremierPointage;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final int nombrePointages;

  private RestConflitEnListe(ConflitEnListe ligne, AnnuaireDAtelier annuaire) {
    adresse = new RestAdresseDossierConflit(ligne.adresse().suivi().uuid(), ligne.adresse().pointage().uuid());
    revision = ligne.revision().value();
    elementId = ligne.element().id().uuid();
    designation = ligne.element().nom().value();
    operateurId = ligne.cle().operateur().uuid();
    operateur = RestOperateur.resolu(annuaire, ligne.cle().operateur());
    posteId = ligne
      .cle()
      .poste()
      .map(id -> id.uuid())
      .orElse(null);
    poste = RestPosteDeTravail.resolu(annuaire, ligne.cle().poste());
    datePremierPointage = ligne.repere().premierPointage();
    nombrePointages = ligne.repere().nombrePointages();
  }

  static RestConflitEnListe from(ConflitEnListe ligne, AnnuaireDAtelier annuaire) {
    return new RestConflitEnListe(ligne, annuaire);
  }
}
