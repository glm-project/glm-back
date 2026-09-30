package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.JourDeSynthese;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Un des sept jours du calendrier de l'entreprise, son journal brut et son temps operationnel.")
record RestJourDeSynthese(
  @Schema(description = "Date dans le fuseau de l'entreprise.", example = "2026-05-11") LocalDate jour,
  @Schema(description = "Tous les pointages actifs de l'operateur dates de ce jour, meme sans activite interpretable.")
  List<RestPointage> pointages,
  @Schema(
    description = "Total de travail et NC du jour, y compris automatiquement. Incomplet sans valeur si une activite a resoudre y contribue ; une activite en cours ne compte rien.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  RestDureeDeSynthese dureeOperationnelle
) {
  static RestJourDeSynthese from(JourDeSynthese jour) {
    return new RestJourDeSynthese(
      jour.jour(),
      jour.pointages().stream().map(RestPointage::from).toList(),
      RestDureeDeSynthese.from(jour.dureeOperationnelle())
    );
  }
}
