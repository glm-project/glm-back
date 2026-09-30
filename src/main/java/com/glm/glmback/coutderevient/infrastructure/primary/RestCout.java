package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Cout;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
  description = """
  Un cout, separe en ce que coute la machine et en ce que coute la personne.

  Les deux ne s'agregent pas de la meme facon : le cout horaire de chaque poste actif court en entier, alors que le
  taux horaire de l'operateur est divise par le nombre de postes qu'il occupait simultanement, tous elements
  confondus. Les afficher separement est ce qui rend la regle lisible.
  """
)
record RestCout(
  @Schema(description = "Cout des postes de travail, jamais divise.") RestMontantDuCout machine,
  @Schema(description = "Cout des operateurs, divise par le parallelisme.") RestMontantDuCout mainDOeuvre,
  @Schema(description = "Somme des deux.") RestMontantDuCout total
) {
  static RestCout from(Cout cout) {
    return new RestCout(
      RestMontantDuCout.from(cout.machine()),
      RestMontantDuCout.from(cout.mainDOeuvre()),
      RestMontantDuCout.from(cout.total())
    );
  }
}
