package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

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
}
