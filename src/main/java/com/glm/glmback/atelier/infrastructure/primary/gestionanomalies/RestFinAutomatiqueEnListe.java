package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.FinAutomatiqueEnListe;
import com.glm.glmback.atelier.infrastructure.primary.RestOperateur;
import com.glm.glmback.atelier.infrastructure.primary.RestPosteDeTravail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestFinAutomatiqueEnListe",
  description = "Une activite terminee a son echeance faute de fin reelle, issue des projections courantes, sans journal. L'adresse est celle de l'ouvrant actif ; activite est l'identite d'origine que visent les actes."
)
final class RestFinAutomatiqueEnListe implements RestAnomalieEnListe {

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final RestAdresseDossierAnomalie adresse;

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
  private final UUID activite;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final Instant debut;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final Instant echeance;

  private RestFinAutomatiqueEnListe(FinAutomatiqueEnListe ligne, AnnuaireDAtelier annuaire) {
    adresse = new RestAdresseDossierAnomalie(ligne.adresse().suivi().uuid(), ligne.adresse().pointage().uuid());
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
    activite = ligne.activite().uuid();
    debut = ligne.debut();
    echeance = ligne.echeance();
  }

  static RestFinAutomatiqueEnListe from(FinAutomatiqueEnListe ligne, AnnuaireDAtelier annuaire) {
    return new RestFinAutomatiqueEnListe(ligne, annuaire);
  }
}
