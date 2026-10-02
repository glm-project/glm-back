package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  description = """
  Le cout de revient d'un element de fabrication, une ligne par nature d'operation.

  Rien n'est stocke : le rapport est recalcule a chaque lecture depuis les activites interpretees par atelier, pour qu'une saisie
  regularisee apres coup compte a l'heure ou le travail a eu lieu.

  Chaque total complet est chiffre ; un total incomplet ne porte aucune somme partielle.
  La main d'oeuvre est arrondie par fenetre de partage puis repartie au centime, la machine une fois par activite ;
  lignes et rapport additionnent ces montants.
  """
)
record RestCoutDeRevient(
  @Schema(description = "Instant d'evaluation commun du rapport.", requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(
    description = "Nombre d'activites en cours exclues du temps, des couts et du diviseur.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  int activitesEnCours,
  @Schema(description = "L'element de fabrication, tous ses passages en atelier confondus.") RestElement element,
  @Schema(description = "Une ligne par nature, la ligne sans nature en dernier.") List<RestLigneDeCout> lignes,
  @Schema(description = "Conflits de l'element et sequences responsables de valeurs incompletes, meme sur un autre element.")
  List<RestConflitDuCout> conflits,
  @Schema(description = "Temps total passe sur l'element.") RestTempsPasse temps,
  @Schema(description = "Cout total de l'element.") RestCout cout
) {
  static RestCoutDeRevient from(CoutDeRevient rapport) {
    return new RestCoutDeRevient(
      rapport.lecture().evaluation(),
      rapport.lecture().activitesEnCours(),
      RestElement.from(rapport.element()),
      rapport.lignes().stream().map(RestLigneDeCout::from).toList(),
      rapport.conflits().stream().map(RestConflitDuCout::from).toList(),
      RestTempsPasse.from(rapport.temps()),
      RestCout.from(rapport.cout())
    );
  }
}
