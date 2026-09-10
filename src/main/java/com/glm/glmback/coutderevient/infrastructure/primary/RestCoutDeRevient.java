package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
  description = """
  Le cout de revient d'un element de fabrication, une ligne par nature d'operation.

  Rien n'est stocke : le rapport est recalcule a chaque lecture depuis les journaux de l'atelier, pour qu'une saisie
  regularisee apres coup compte a l'heure ou le travail a eu lieu.

  Le total est la somme des lignes deja arrondies : il vaut donc exactement ce que l'ecran affiche.
  """
)
record RestCoutDeRevient(
  @Schema(description = "L'element de fabrication, tous ses passages en atelier confondus.") RestElement element,
  @Schema(description = "Une ligne par nature, la ligne sans nature en dernier.") List<RestLigneDeCout> lignes,
  @Schema(description = "Temps total passe sur l'element.") RestTempsPasse temps,
  @Schema(description = "Cout total de l'element.") RestCout cout
) {
  static RestCoutDeRevient from(CoutDeRevient rapport) {
    return new RestCoutDeRevient(
      RestElement.from(rapport.element()),
      rapport.lignes().stream().map(RestLigneDeCout::from).toList(),
      RestTempsPasse.from(rapport.temps()),
      RestCout.from(rapport.cout())
    );
  }
}
