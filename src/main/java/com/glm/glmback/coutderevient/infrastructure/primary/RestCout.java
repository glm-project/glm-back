package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Cout;
import com.glm.glmback.coutderevient.domain.Montant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
  description = """
  Un cout, separe en ce que coute la machine et en ce que coute la personne.

  Les deux ne s'agregent pas de la meme facon : le cout horaire de chaque poste actif court en entier, alors que le
  taux horaire de l'operateur est divise par le nombre de postes qu'il occupait simultanement, tous elements
  confondus. Les afficher separement est ce qui rend la regle lisible.
  """
)
record RestCout(
  @Schema(description = "Cout des postes de travail, jamais divise.", example = "90.00") BigDecimal machine,
  @Schema(description = "Cout des operateurs, divise par le parallelisme.", example = "40.00") BigDecimal mainDOeuvre,
  @Schema(description = "Somme des deux.", example = "130.00") BigDecimal total
) {
  static RestCout from(Cout cout) {
    return new RestCout(cout.machine().value(), cout.mainDOeuvre().value(), valeur(cout.total()));
  }

  private static BigDecimal valeur(Montant montant) {
    return montant.value();
  }
}
