package com.glm.glmback.atelier.infrastructure.primary;

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
  Le suivi en atelier d'un element engage.

  Son etat, ses activites en cours et son temps ne sont **jamais stockes** : tout est deduit du journal a chaque
  lecture. C'est ce qui permet a une saisie rattrapee de compter a l'heure ou elle a reellement eu lieu.
  """
)
record RestSuiviDAtelier(
  @Schema(description = "Identifiant du suivi. C'est lui, et non celui de l'element, que portent les URLs.") UUID id,
  @Schema(description = "Identifiant de l'element de fabrication engage.") UUID element,
  @Schema(description = "Nom de l'element, copie a l'engagement.", example = "OF-2026-000042") String nom,
  @Schema(description = "Type de l'element, copie a l'engagement.") TypeDElementEngage type,
  @Schema(description = "Utilisateur ayant engage l'element.", example = "gestionnaire.impeccmold") String engagePar,
  @Schema(description = "Instant de l'engagement.") Instant engageLe,
  @Schema(description = "EN_ATTENTE, EN_COURS, INTERROMPU ou CLOTURE. Deduit du journal.") EtatDAtelier etat,
  @Schema(description = "Utilisateur ayant cloture l'element, absent tant qu'il ne l'est pas.") String cloturePar,
  @Schema(description = "Instant metier de la cloture, absent tant que l'element n'est pas cloture.") Instant clotureLe,
  @Schema(description = "Le journal complet, annules compris, du plus ancien au plus recent.") List<RestEvenementDAtelier> journal,
  @Schema(description = "Les activites ouvertes a cet instant.") List<RestActiviteEnCours> activitesEnCours
) {
  static RestSuiviDAtelier from(SuiviDAtelier suivi, AnnuaireDAtelier annuaire) {
    return new RestSuiviDAtelier(
      suivi.id().uuid(),
      suivi.element().id().uuid(),
      suivi.element().nom().value(),
      suivi.element().type(),
      suivi.engagement().auteur().value(),
      suivi.engagement().date(),
      suivi.etat(),
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
      suivi
        .activitesEnCours()
        .stream()
        .map(activite -> RestActiviteEnCours.from(activite, annuaire))
        .toList()
    );
  }
}
