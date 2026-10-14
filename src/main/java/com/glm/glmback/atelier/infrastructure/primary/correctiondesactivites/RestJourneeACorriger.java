package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.correctiondesactivites.JourneeACorriger;
import com.glm.glmback.atelier.infrastructure.primary.RestOperateur;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(
  description = """
  La journee d'un operateur telle que le gestionnaire la corrige : les minuits du jour, les activites qui le touchent, leur
  duree operationnelle et de quoi ajouter une activite.

  Les activites sont lues apres les corrections deja faites : une activite retiree n'y est plus, une activite dont le poste
  a change y est sur son nouveau poste. La duree operationnelle est la somme des portions du jour des activites terminees,
  automatiquement comprises, sans union des intervalles ; une activite en cours ne compte pas. Les instants se lisent en UTC
  comme partout dans l'API ; ceux qu'on envoie peuvent porter un decalage. Les durees suivent ISO-8601.
  """
)
record RestJourneeACorriger(
  @Schema(description = "L'operateur, resolu au referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) RestOperateur operateur,
  @Schema(
    description = "Le jour calendaire lu, dans le fuseau de l'entreprise.",
    example = "2026-10-05",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  LocalDate jour,
  @Schema(description = "Les minuits du jour, calcules dans le fuseau de l'entreprise.", requiredMode = Schema.RequiredMode.REQUIRED)
  RestBornesDuJour bornes,
  @Schema(
    description = "La duree operationnelle du jour, sous le nom qu'emploie la synthese des heures.",
    type = "string",
    example = "PT7H30M",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeOperationnelle,
  @Schema(description = "Les activites qui touchent le jour, par debut.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestActiviteDeLaJournee> activites,
  @Schema(description = "De quoi ajouter une activite ce jour-la.", requiredMode = Schema.RequiredMode.REQUIRED) RestChoixDAjout choix,
  @Schema(
    description = "Instant effectivement utilise pour juger si une activite sans fin est en cours ou terminee automatiquement.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant evaluation
) {
  static RestJourneeACorriger from(JourneeACorriger journee) {
    OperateurConnu operateur = journee.operateur();

    return new RestJourneeACorriger(
      new RestOperateur(operateur.id().uuid(), operateur.nom().value(), operateur.prenom().value()),
      journee.jour(),
      RestBornesDuJour.from(journee.bornes()),
      journee.dureeOperationnelle(),
      journee
        .activites()
        .stream()
        .map(activite -> RestActiviteDeLaJournee.from(activite, journee))
        .toList(),
      RestChoixDAjout.from(journee),
      journee.evaluation()
    );
  }
}
