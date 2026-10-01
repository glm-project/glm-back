package com.glm.glmback.atelier.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(description = "Un pointage de l'operateur sur un element engage, date a l'instant present.")
record RestPointage(
  @Schema(description = "Identifiant durable du geste, genere par le pupitre.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID id,

  @Schema(
    description = "DEBUT, NON_CONFORMITE ou FIN. La reprise du travail apres une non conformite se pointe DEBUT, en transition.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  TypeDEvenementDAtelier type,

  @Schema(
    description = """
    Ce que le geste fait d'une activite : OUVERTURE en cree une nouvelle, y compris en non conformite ou a la reprise
    apres une pause ; TRANSITION remplace l'activite visee par une activite de l'autre categorie ; FIN termine l'activite
    visee. Seule une fin se pointe FIN. Aucune valeur par defaut.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  IntentionDePointage intention,

  @Schema(
    description = """
    L'activite que vise une transition ou une fin, designee par l'identifiant de son pointage ouvrant d'origine : celui
    que porte `activite` au journal. Requise pour une transition ou une fin, absente pour une ouverture. Elle doit etre
    une activite de ce suivi, du meme operateur et du meme poste.
    """
  )
  UUID cible,

  @Schema(description = "Identifiant de l'operateur dont le temps est affecte.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID operateur,

  @Schema(description = "Identifiant du poste de travail. Toujours facultatif : une entreprise sans parc machine le laisse vide.")
  UUID poste,

  @Schema(
    description = "Heure metier du geste. Absente, elle vaut l'instant de reception initial. La fournir ne fait pas du pointage une regularisation."
  )
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

  PointageAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(suivi)
      .type(type)
      .intention(intention)
      .activiteVisee(Optional.ofNullable(cible).map(ActiviteId::new))
      .operateur(new OperateurId(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravailId::new))
      .auteur(auteur)
      .dateDeSurvenue(Optional.ofNullable(dateDeSurvenue))
      .evenement(new EvenementDAtelierId(id));
  }
}
