package com.glm.glmback.atelier.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.TypeDElementEngage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  Une ligne du tableau d'atelier, sans le journal des evenements.
  L'etat et les activites en cours restent deduits du journal a chaque lecture.
  Le journal complet, annules compris, se consulte via GET /api/atelier/suivis/{id}.
  """
)
final class RestSyntheseDeSuiviDAtelier {

  @JsonProperty
  @Schema(description = "Identifiant du suivi. C'est lui, et non celui de l'element, que portent les URLs.")
  private final UUID id;

  @JsonProperty
  @Schema(description = "Identifiant de l'element de fabrication engage.")
  private final UUID element;

  @JsonProperty
  @Schema(description = "Nom de l'element, copie a l'engagement.", example = "OF-2026-000042")
  private final String nom;

  @JsonProperty
  @Schema(description = "Type de l'element, copie a l'engagement.")
  private final TypeDElementEngage type;

  @JsonProperty
  @Schema(description = "Utilisateur ayant engage l'element.", example = "gestionnaire.impeccmold")
  private final String engagePar;

  @JsonProperty
  @Schema(description = "Instant de l'engagement.")
  private final Instant engageLe;

  @JsonProperty
  @Schema(description = "EN_ATTENTE, EN_COURS, INTERROMPU ou CLOTURE. Deduit du journal.")
  private final EtatDAtelier etat;

  @JsonProperty
  @Schema(description = "Utilisateur ayant cloture l'element, absent tant qu'il ne l'est pas.")
  private final String cloturePar;

  @JsonProperty
  @Schema(description = "Instant metier de la cloture, absent tant que l'element n'est pas cloture.")
  private final Instant clotureLe;

  @JsonProperty
  @Schema(description = "Les activites ouvertes a cet instant.")
  private final List<RestActiviteEnCours> activitesEnCours;

  private RestSyntheseDeSuiviDAtelier(SuiviDAtelier suivi, AnnuaireDAtelier annuaire) {
    id = suivi.id().uuid();
    element = suivi.element().id().uuid();
    nom = suivi.element().nom().value();
    type = suivi.element().type();
    engagePar = suivi.engagement().auteur().value();
    engageLe = suivi.engagement().date();
    etat = suivi.etat();
    cloturePar = suivi
      .cloture()
      .map(cloture -> cloture.auteur().value())
      .orElse(null);
    clotureLe = suivi.cloture().map(Cloture::dateDeSurvenue).orElse(null);
    activitesEnCours = suivi
      .activitesEnCours()
      .stream()
      .map(activite -> RestActiviteEnCours.from(activite, annuaire))
      .toList();
  }

  static RestSyntheseDeSuiviDAtelier from(SuiviDAtelier suivi, AnnuaireDAtelier annuaire) {
    return new RestSyntheseDeSuiviDAtelier(suivi, annuaire);
  }
}
