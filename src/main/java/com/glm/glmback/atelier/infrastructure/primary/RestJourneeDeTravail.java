package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  La journee de travail d'un operateur : une venue, bornee par une arrivee et un depart.

  Ce n'est **pas** un jour calendaire — le contexte ne connait ni fuseau horaire ni date. L'API expose a la fois
  `amplitude` (de l'arrivee au depart) et `fenetres` (pauses retirees) sans choisir laquelle compte pour la paie.
  """
)
record RestJourneeDeTravail(
  @Schema(description = "Identifiant de la journee.") UUID id,
  @Schema(description = "Operateur concerne.", example = "dupont") String operateur,
  @Schema(description = "ABSENT, PRESENT ou EN_PAUSE. Deduit du journal.") EtatDePresence etat,
  @Schema(description = "De l'arrivee au depart, pauses comprises. Absente tant que la journee est ouverte.") RestPeriode amplitude,
  @Schema(description = "Les intervalles de presence effective, pauses retirees.") List<RestFenetreDePresence> fenetres,
  @Schema(description = "Le journal complet, annules compris, du plus ancien au plus recent.") List<RestEvenementDePresence> journal
) {
  static RestJourneeDeTravail from(JourneeDeTravail journee) {
    return new RestJourneeDeTravail(
      journee.id().uuid(),
      journee.operateur().value(),
      journee.etat(),
      journee.amplitude().map(RestPeriode::from).orElse(null),
      journee.fenetres().stream().map(RestFenetreDePresence::from).toList(),
      journee.journal().evenements().stream().map(RestEvenementDePresence::from).toList()
    );
  }
}
