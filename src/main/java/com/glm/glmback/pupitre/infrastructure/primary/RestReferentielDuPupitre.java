package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.ReferentielDuPupitre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  name = "RestReferentielDuPupitre",
  description = """
  Tout ce que le pupitre met en cache pour continuer a fonctionner sans reseau, en un seul appel.

  Operateurs, habilitations et elements pointables sont relus dans une transaction unique, sans pagination.
  Les activites interpretees par atelier sont lues dans sa projection et leur expiration est jugee a genereLe.
  Les requetes successives ne garantissent pas un instantane face aux ecritures concurrentes.
  """
)
record RestReferentielDuPupitre(
  @Schema(
    description = "Instant ou le serveur a produit cet instantane. C'est la version : elle dit de quand date ce que l'ecran affiche.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant genereLe,
  @Schema(description = "Les operateurs designables, tries par nom puis prenom.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestOperateurDuPupitre> operateurs,
  @Schema(description = "Les elements pointables, tries par nom.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestSuiviDuPupitre> suivis
) {
  static RestReferentielDuPupitre from(ReferentielDuPupitre referentiel) {
    return new RestReferentielDuPupitre(
      referentiel.genereLe(),
      referentiel.operateurs().stream().map(RestOperateurDuPupitre::from).toList(),
      referentiel
        .suivis()
        .stream()
        .map(suivi -> RestSuiviDuPupitre.from(suivi, referentiel.genereLe()))
        .toList()
    );
  }
}
