package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.LigneDeCout;
import com.glm.glmback.coutderevient.domain.NatureDOperation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
  description = """
  Une ligne du rapport : tout ce qui a ete fait sur l'element a une meme nature d'operation.

  La nature vient du poste de travail, jamais de la personne, et elle a ete copiee au moment de la saisie : un poste
  requalifie depuis ne requalifie pas les heures deja passees. Elle est absente quand le pointage n'a engage aucun
  poste, ce qui est le comportement nominal d'une entreprise sans parc machine.
  """
)
record RestLigneDeCout(
  @Schema(description = "Nature de l'operation, absente pour un pointage sans poste.", example = "Fraisage") String nature,
  @Schema(description = "Du premier debut a la derniere fin de cette nature.") RestPeriode periode,
  @Schema(description = "Temps passe, bon travail et non conformite separes.") RestTempsPasse temps,
  @Schema(description = "Les periodes de reprise, datees, dans l'ordre.") List<RestPeriode> nonConformites,
  @Schema(description = "Cout de la ligne, arrondi au centime.") RestCout cout
) {
  static RestLigneDeCout from(LigneDeCout ligne) {
    return new RestLigneDeCout(
      ligne.nature().map(NatureDOperation::value).orElse(null),
      RestPeriode.from(ligne.periode()),
      RestTempsPasse.from(ligne.temps()),
      ligne.nonConformites().stream().map(RestPeriode::from).toList(),
      RestCout.from(ligne.cout())
    );
  }
}
