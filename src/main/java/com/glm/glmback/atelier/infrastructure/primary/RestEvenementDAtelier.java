package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CoutHoraire;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.TauxHoraire;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Un evenement du journal d'un element engage.

  Comme pour la presence, l'horodatage est bitemporel. La pause et le depart de l'operateur **ne figurent jamais ici** :
  ce sont des faits de sa journee de travail, croises a la lecture.
  """
)
record RestEvenementDAtelier(
  @Schema(description = "Identifiant de l'evenement, a reprendre pour l'annuler ou le corriger.") UUID id,
  @Schema(description = "Nature du pointage. Une reprise apres non conformite se pointe comme un DEBUT.") TypeDEvenementDAtelier type,
  @Schema(description = "Operateur dont le temps est affecte.") RestOperateur operateur,
  @Schema(description = "Poste de travail, toujours facultatif.") RestPosteDeTravail poste,
  @Schema(description = "Nature de l'operation, recopiee du poste a la saisie. Simple axe d'agregation.") String nature,
  @Schema(
    description = "Cout horaire du poste, copie a la saisie. Absent si le poste n'est pas valorise ou si aucun poste n'est fourni.",
    example = "45.50"
  )
  BigDecimal coutHoraire,
  @Schema(description = "Taux horaire de l'operateur, copie a la saisie. Absent si l'operateur n'est pas valorise.", example = "22.00")
  BigDecimal tauxHoraire,
  @Schema(description = "Utilisateur ayant saisi l'evenement.", example = "dupont") String auteur,
  @Schema(description = "Heure metier a laquelle le fait a eu lieu.") Instant dateDeSurvenue,
  @Schema(description = "Heure a laquelle la saisie a ete enregistree.") Instant dateDEnregistrement,
  @Schema(description = "Vrai lorsque la saisie a ete faite apres coup.") boolean estUneRegularisation,
  @Schema(description = "Presente lorsque l'evenement a ete annule. L'evenement reste au journal.") RestAnnulation annulation
) {
  static RestEvenementDAtelier from(EvenementDAtelier evenement, AnnuaireDAtelier annuaire) {
    return new RestEvenementDAtelier(
      evenement.id().uuid(),
      evenement.type(),
      RestOperateur.resolu(annuaire, evenement.operateur()),
      RestPosteDeTravail.resolu(annuaire, evenement.poste()),
      evenement.nature().map(NatureDOperation::value).orElse(null),
      evenement.coutHoraire().map(CoutHoraire::value).orElse(null),
      evenement.tauxHoraire().map(TauxHoraire::value).orElse(null),
      evenement.auteur().value(),
      evenement.dateDeSurvenue(),
      evenement.dateDEnregistrement(),
      evenement.estUneRegularisation(),
      evenement.annulation().map(RestAnnulation::from).orElse(null)
    );
  }
}
