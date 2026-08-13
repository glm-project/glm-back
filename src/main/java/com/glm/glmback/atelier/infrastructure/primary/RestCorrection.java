package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  La correction d'une saisie fausse : une annulation et une regularisation jouees en un seul acte.

  L'evenement d'origine reste au journal, annule, et le remplacant y prend sa place a l'heure corrigee.
  """
)
record RestCorrection(
  @Schema(
    description = "Motif de l'annulation de la saisie fausse.",
    example = "Heure erronee",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  @Size(max = 255)
  String motif,

  @Schema(description = "Nature du pointage corrige.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TypeDEvenementDAtelier type,

  @Schema(description = "Identifiant de l'operateur dont le temps est affecte.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID operateur,

  @Schema(description = "Identifiant du poste de travail, facultatif.") UUID poste,

  @Schema(description = "Heure metier corrigee.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Instant dateDeSurvenue
) {
  CorrectionAEnregistrer toDomain(SuiviDAtelierId suivi, UUID evenement, Auteur auteur) {
    RegularisationAEnregistrer remplacement = RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(new OperateurId(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravailId::new))
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);

    return new CorrectionAEnregistrer(new EvenementDAtelierId(evenement), new MotifDAnnulation(motif), remplacement);
  }
}
