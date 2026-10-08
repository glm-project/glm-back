package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  Le suivi en atelier d'un element engage.

  Son etat, ses activites en cours et son temps ne sont **jamais stockes** : tout est deduit du journal a chaque
  lecture. C'est ce qui permet a une saisie rattrapee de compter a l'heure ou elle a reellement eu lieu.
  """
)
public record RestSuiviDAtelier(
  @Schema(
    description = "Identifiant du suivi. C'est lui, et non celui de l'element, que portent les URLs.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID id,
  @Schema(description = "Identifiant de l'element de fabrication engage.", requiredMode = Schema.RequiredMode.REQUIRED) UUID element,
  @Schema(description = "Nom de l'element, copie a l'engagement.", example = "OF-2026-000042", requiredMode = Schema.RequiredMode.REQUIRED)
  String nom,
  @Schema(description = "Categorie de l'element, copiee a l'engagement.", requiredMode = Schema.RequiredMode.REQUIRED) String type,
  @Schema(
    description = "Utilisateur ayant engage l'element.",
    example = "gestionnaire.impeccmold",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String engagePar,
  @Schema(description = "Instant de l'engagement.", requiredMode = Schema.RequiredMode.REQUIRED) Instant engageLe,
  @Schema(
    description = "EN_ATTENTE, EN_COURS, INTERROMPU ou CLOTURE. Deduit du journal a l'instant de la lecture.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  EtatDAtelier etat,
  @Schema(description = "Utilisateur ayant cloture l'element, absent tant qu'il ne l'est pas.") String cloturePar,
  @Schema(description = "Instant metier de la cloture, absent tant que l'element n'est pas cloture.") Instant clotureLe,
  @Schema(description = "Le journal complet, annules compris, du plus ancien au plus recent.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestEvenementDAtelier> journal,
  @Schema(
    description = """
    Les activites en cours a l'instant de la lecture ; une activite dont l'echeance est atteinte n'y figure plus, ni une
    activite a resoudre.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestActiviteEnCours> activitesEnCours,
  @Schema(
    description = """
    Les sequences en conflit du journal, vide quand ses faits sont coherents. Elles ne dependent pas de l'instant de la
    lecture, et ne changent pas l'etat, juge sur les seules activites interpretables.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestSequenceEnConflit> conflits
) {
  public static RestSuiviDAtelier from(LectureDuSuivi lecture, AnnuaireDAtelier annuaire) {
    SuiviDAtelier suivi = lecture.suivi();

    return new RestSuiviDAtelier(
      suivi.id().uuid(),
      suivi.element().id().uuid(),
      suivi.element().nom().value(),
      suivi.element().categorie().value(),
      suivi.engagement().auteur().value(),
      suivi.engagement().date(),
      lecture.etat(),
      suivi
        .cloture()
        .map(cloture -> cloture.auteur().value())
        .orElse(null),
      suivi.cloture().map(Cloture::dateDeSurvenue).orElse(null),
      suivi
        .journal()
        .evenements()
        .stream()
        .map(evenement -> RestEvenementDAtelier.from(evenement, annuaire))
        .toList(),
      lecture
        .activitesEnCours()
        .stream()
        .map(activite -> RestActiviteEnCours.from(activite, annuaire))
        .toList(),
      lecture
        .conflits()
        .stream()
        .map(sequence -> RestSequenceEnConflit.from(sequence, annuaire))
        .toList()
    );
  }
}
