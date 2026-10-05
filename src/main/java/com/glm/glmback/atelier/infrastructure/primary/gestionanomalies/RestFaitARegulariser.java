package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
  description = "La fin que le gestionnaire regularise sur une activite terminee automatiquement. Aucune heure n'est proposee : il la saisit, et la reprend dans le fait de l'acte REGULARISATION."
)
record RestFaitARegulariser(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TypeDEvenementDAtelier type,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IntentionDePointage intention,
  @Schema(
    requiredMode = Schema.RequiredMode.REQUIRED,
    description = "L'activite d'origine a terminer, celle de activites[].activite, jamais l'identifiant de l'evenement ouvrant."
  )
  UUID activiteVisee,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  UUID poste
) {}
