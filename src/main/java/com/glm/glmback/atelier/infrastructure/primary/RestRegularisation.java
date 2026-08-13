package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  Une saisie oubliee, rattrapee par le gestionnaire a l'heure ou elle a reellement eu lieu.

  L'evenement produit portera `dateDeSurvenue` (fournie ici) et `dateDEnregistrement` (l'instant courant) : c'est cet
  ecart, et lui seul, qui signale une regularisation.
  """
)
record RestRegularisation(
  @Schema(description = "Nature du pointage rattrape.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TypeDEvenementDAtelier type,

  @Schema(description = "Identifiant de l'operateur dont le temps est affecte.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID operateur,

  @Schema(description = "Identifiant du poste de travail, facultatif.") UUID poste,

  @Schema(description = "Heure metier a laquelle le fait a reellement eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  Instant dateDeSurvenue
) {
  RegularisationAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(new OperateurId(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravailId::new))
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);
  }
}
