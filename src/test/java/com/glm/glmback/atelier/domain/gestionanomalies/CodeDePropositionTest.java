package com.glm.glmback.atelier.domain.gestionanomalies;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CodeDePropositionTest {

  @Test
  void shouldDireLeTypeDActeDeChaqueProposition() {
    assertThat(CodeDeProposition.RATTACHER_FIN_A_ACTIVITE_REMPLACANTE.kind()).isEqualTo(TypeDActeDeResolution.CORRECTION);
    assertThat(CodeDeProposition.ANNULER_TRANSITION.kind()).isEqualTo(TypeDActeDeResolution.ANNULATION);
    assertThat(CodeDeProposition.REGULARISER_FIN.kind()).isEqualTo(TypeDActeDeResolution.REGULARISATION);
    assertThat(CodeDeProposition.CORRIGER_FIN_TARDIVE.kind()).isEqualTo(TypeDActeDeResolution.CORRECTION);
    assertThat(CodeDeProposition.CORRIGER_TRANSITION_TARDIVE.kind()).isEqualTo(TypeDActeDeResolution.CORRECTION);
  }
}
