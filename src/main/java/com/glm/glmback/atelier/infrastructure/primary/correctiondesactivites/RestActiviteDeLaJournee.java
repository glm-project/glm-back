package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.correctiondesactivites.ActiviteDeLaJournee;
import com.glm.glmback.atelier.domain.correctiondesactivites.EtatDActiviteDeLaJournee;
import com.glm.glmback.atelier.domain.correctiondesactivites.JourneeACorriger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  Une activite qui touche le jour lu, telle que le journal la lit apres les corrections du gestionnaire.

  Ses bornes sont entieres : une activite a cheval sur minuit garde son debut et sa fin d'origine, et ses portions sont
  `dureeSurLeJour` pour le jour lu et `autresJours` pour les autres. Une activite en cours n'a ni fin, ni duree. Elle n'est
  corrigeable (retrait, changement de poste) que terminee, normalement, automatiquement ou par une fin regularisee, et tant
  que son suivi n'est pas cloture : la cloture d'un suivi se lit dans `choix.suivis`.
  """
)
record RestActiviteDeLaJournee(
  @Schema(
    description = "Identite de l'activite : l'identifiant de son pointage ouvrant d'origine, qui ne change pas quand son poste change.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID id,
  @Schema(description = "Le suivi qui porte l'activite : celui des routes de correction.", requiredMode = Schema.RequiredMode.REQUIRED)
  UUID suivi,
  @Schema(description = "L'element engage dans ce suivi.", requiredMode = Schema.RequiredMode.REQUIRED) RestElementDeLaJournee element,
  @Schema(description = "Le poste courant de l'activite, apres un eventuel changement de poste. Absent si aucun poste n'a ete pointe.")
  RestPosteDeLaJournee poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE : le type de l'activite.", requiredMode = Schema.RequiredMode.REQUIRED)
  CategorieDActivite categorie,
  @Schema(description = "Debut de l'activite entiere.", requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(
    description = "Fin de l'activite entiere : sa fin reelle, ou son echeance pour une fin automatique. Absente tant qu'elle est en cours."
  )
  Instant fin,
  @Schema(
    description = """
    TERMINEE (fin reelle : pointee, regularisee, ajoutee ou posee par la cloture), TERMINEE_AUTOMATIQUEMENT (l'echeance est
    atteinte faute de fin reelle : l'anomalie que le gestionnaire regularise) ou EN_COURS.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  EtatDActiviteDeLaJournee etat,
  @Schema(
    description = "La portion comptee le jour lu, ISO-8601. Absente pour une activite en cours, qui ne compte aucune duree.",
    type = "string",
    example = "PT2H"
  )
  Duration dureeSurLeJour,
  @Schema(
    description = "Les portions comptees les autres jours, coupees aux minuits de l'entreprise. Vide pour une activite contenue dans le jour.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestPortionDeJour> autresJours
) {
  static RestActiviteDeLaJournee from(ActiviteDeLaJournee activite, JourneeACorriger journee) {
    return new RestActiviteDeLaJournee(
      activite.id().uuid(),
      activite.suivi().uuid(),
      RestElementDeLaJournee.from(activite.element(), journee.fiche(activite.element().id())),
      activite.poste().map(RestPosteDeLaJournee::from).orElse(null),
      activite.categorie(),
      activite.debut(),
      activite.fin().orElse(null),
      activite.etat(),
      activite.dureeSurLeJour().orElse(null),
      activite.autresJours().stream().map(RestPortionDeJour::from).toList()
    );
  }
}
