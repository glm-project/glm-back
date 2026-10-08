package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.EtatDuSuivi;
import com.glm.glmback.pupitre.domain.ReferenceDElement;
import com.glm.glmback.pupitre.domain.SuiviDuPupitre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
  name = "RestSuiviDuPupitre",
  description = """
  Une tuile de l'ecran d'atelier : un element sur lequel on peut pointer, et ce qui s'y passe.

  Les elements clotures sont absents : ils n'acceptent plus de pointage. Le journal ne figure pas ici — il se
  consulte via GET /api/atelier/suivis/{id}.
  """
)
record RestSuiviDuPupitre(
  @Schema(
    description = "Identifiant du suivi. C'est lui, et non celui de l'element, que portent les URLs de pointage.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID id,
  @Schema(description = "Nom de l'element, copie a l'engagement.", example = "OF-2026-000042", requiredMode = Schema.RequiredMode.REQUIRED)
  String nom,
  @Schema(
    description = "Reference que l'entreprise donne elle-meme a l'element, relue a chaque lecture. Absente si elle n'en attribue pas.",
    example = "M-1187"
  )
  String reference,
  @Schema(description = "Categorie de l'element, copiee a l'engagement.", requiredMode = Schema.RequiredMode.REQUIRED) String categorie,
  @Schema(
    description = "EN_ATTENTE, EN_COURS ou INTERROMPU. Juge sur les seules activites interpretables a genereLe.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  EtatDuSuivi etat,
  @Schema(description = "Les activites ouvertes a cet instant.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestActiviteDuPupitre> activites,
  @Schema(description = "Sequences en conflit a resoudre par le gestionnaire.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestConflitDuPupitre> conflits
) {
  static RestSuiviDuPupitre from(SuiviDuPupitre suivi, Instant evaluation) {
    return new RestSuiviDuPupitre(
      suivi.id().uuid(),
      suivi.nom().value(),
      suivi.reference().map(ReferenceDElement::value).orElse(null),
      suivi.categorie().value(),
      suivi.etatA(evaluation),
      suivi.activitesEnCoursA(evaluation).stream().map(RestActiviteDuPupitre::from).toList(),
      suivi.conflits().stream().map(RestConflitDuPupitre::from).toList()
    );
  }
}
