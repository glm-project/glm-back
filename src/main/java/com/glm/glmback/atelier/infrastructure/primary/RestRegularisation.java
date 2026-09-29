package com.glm.glmback.atelier.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  Une saisie oubliee, rattrapee par le gestionnaire a l'heure ou elle a reellement eu lieu.

  L'evenement produit portera `dateDeSurvenue` (fournie ici) et `dateDEnregistrement` (l'instant courant), et restera
  une regularisation (`estUneRegularisation`) meme saisi a l'heure du fait : c'est l'acte qui la fait, jamais l'ecart
  entre les deux dates.
  """
)
record RestRegularisation(
  @Schema(description = "Nature du pointage rattrape.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TypeDEvenementDAtelier type,

  @Schema(
    description = """
    Ce que le geste fait d'une activite : OUVERTURE, TRANSITION ou FIN, comme pour un pointage. Seule une fin se pointe
    FIN. Aucune valeur par defaut.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  IntentionDePointage intention,

  @Schema(
    description = """
    L'activite que vise une transition ou une fin, designee par l'identifiant de son pointage ouvrant d'origine. Requise
    pour une transition ou une fin, absente pour une ouverture.
    """
  )
  UUID cible,

  @Schema(description = "Identifiant de l'operateur dont le temps est affecte.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID operateur,

  @Schema(description = "Identifiant du poste de travail, facultatif.") UUID poste,

  @Schema(description = "Heure metier a laquelle le fait a reellement eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  Instant dateDeSurvenue
) {
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = CoherenceDuGeste.INTENTION_ADMISE)
  boolean isIntentionAdmiseParLeType() {
    return CoherenceDuGeste.intentionAdmise(type, intention);
  }

  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = CoherenceDuGeste.CIBLE_CONFORME)
  boolean isCibleConformeALIntention() {
    return CoherenceDuGeste.cibleConforme(intention, cible);
  }

  RegularisationAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .intention(intention)
      .activiteVisee(Optional.ofNullable(cible).map(ActiviteId::new))
      .operateur(new OperateurId(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravailId::new))
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);
  }
}
