package com.glm.glmback.atelier.application.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class ContexteDeResolutionTest {

  @Test
  void shouldReconnaitreLaMemePersonneApresUnChangementDeNomDAuteur() {
    // GIVEN
    var contexte = CONTEXTE_LEROY_IMPECCMOLD;
    // WHEN
    var memePersonne = contexte.correspondA(CONTEXTE_LEROY_RENOMME_IMPECCMOLD);
    // THEN
    assertThat(memePersonne).isTrue();
  }

  @ParameterizedTest
  @MethodSource("autresContextes")
  void shouldDistinguerLeSujetLEmetteurEtLEntreprise(ContexteDeResolution autre) {
    assertThat(CONTEXTE_LEROY_IMPECCMOLD.correspondA(autre)).isFalse();
  }

  private static Stream<ContexteDeResolution> autresContextes() {
    return Stream.of(CONTEXTE_MARTIN_IMPECCMOLD, CONTEXTE_LEROY_AUTRE_EMETTEUR, CONTEXTE_LEROY_KATILYS);
  }
}
