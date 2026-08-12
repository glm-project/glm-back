package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.Operateur;
import com.glm.glmback.atelier.domain.PosteDeTravail;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Optional;

@Schema(
  description = """
  Une saisie oubliee, rattrapee par le gestionnaire a l'heure ou elle a reellement eu lieu.

  L'evenement produit portera `dateDeSurvenue` (fournie ici) et `dateDEnregistrement` (l'instant courant) : c'est cet
  ecart, et lui seul, qui signale une regularisation.
  """
)
record RestRegularisation(
  @Schema(description = "Nature du pointage rattrape.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TypeDEvenementDAtelier type,

  @Schema(description = "Operateur dont le temps est affecte.", example = "dupont", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Size(max = 100)
  String operateur,

  @Schema(description = "Poste de travail, facultatif.", example = "fraiseuse-1") @Size(max = 100) String poste,

  @Schema(description = "Heure metier a laquelle le fait a reellement eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  Instant dateDeSurvenue
) {
  RegularisationAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(new Operateur(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravail::new))
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);
  }
}
