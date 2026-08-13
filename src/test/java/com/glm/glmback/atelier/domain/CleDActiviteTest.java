package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CleDActiviteTest {

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new CleDActivite(null, Optional.of(POSTE_ID_FRAISEUSE_1)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() -> new CleDActivite(OPERATEUR_ID_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldDistinguerLesPostesDUnMemeOperateur() {
    assertThat(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_1))).isNotEqualTo(
      new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2))
    );
  }

  @Test
  void shouldDistinguerLesOperateursDUnMemePoste() {
    assertThat(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_1))).isNotEqualTo(
      new CleDActivite(OPERATEUR_ID_MARTIN, Optional.of(POSTE_ID_FRAISEUSE_1))
    );
  }

  @Test
  void shouldReduireLaCleALOperateurSansPoste() {
    assertThat(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty())).isEqualTo(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty()));
  }
}
