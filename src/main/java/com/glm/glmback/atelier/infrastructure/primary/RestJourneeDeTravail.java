package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  La journee de travail d'un operateur : une venue, bornee par une arrivee et un depart.

  Ce n'est **pas** un jour calendaire — le contexte ne connait ni fuseau horaire ni date. La pause n'est pas un
  evenement de presence : un operateur en pause reste present, et le pupitre pointe sa pause sur les elements.
  """
)
record RestJourneeDeTravail(
  @Schema(description = "Identifiant de la journee.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Operateur concerne, absent si sa fiche n'est plus resolue.") RestOperateur operateur,
  @Schema(description = "ABSENT ou PRESENT. Deduit du journal.", requiredMode = Schema.RequiredMode.REQUIRED) EtatDePresence etat,
  @Schema(description = "De l'arrivee au depart. Absente tant que la journee est ouverte.") RestPeriode amplitude,
  @Schema(description = "Les intervalles de presence, chacun de l'arrivee au depart.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestFenetreDePresence> fenetres,
  @Schema(description = "Le journal complet, annules compris, du plus ancien au plus recent.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestEvenementDePresence> journal
) {
  static RestJourneeDeTravail from(JourneeDeTravail journee, AnnuaireDAtelier annuaire) {
    return new RestJourneeDeTravail(
      journee.id().uuid(),
      RestOperateur.resolu(annuaire, journee.operateur()),
      journee.etat(),
      journee.amplitude().map(RestPeriode::from).orElse(null),
      journee.fenetres().stream().map(RestFenetreDePresence::from).toList(),
      journee.journal().evenements().stream().map(RestEvenementDePresence::from).toList()
    );
  }
}
