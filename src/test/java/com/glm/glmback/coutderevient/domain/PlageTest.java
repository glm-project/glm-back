package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PlageTest {

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new Plage(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new Plage(LE_11_MAI_A_9H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_9H)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldIntersectWithClosedPlage() {
    Plage intersection = new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_11H))
      .intersection(new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_12H)))
      .orElseThrow();

    assertThat(intersection).isEqualTo(new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_11H)));
  }

  /**
   * Deux plages ouvertes se croisent en une plage ouverte : rien n'est encore termine, et c'est la fermeture a
   * l'horloge, plus tard, qui tranchera.
   */
  @Test
  void shouldIntersectTwoOpenPlages() {
    Plage intersection = new Plage(LE_11_MAI_A_9H, Optional.empty())
      .intersection(new Plage(LE_11_MAI_A_10H, Optional.empty()))
      .orElseThrow();

    assertThat(intersection).isEqualTo(new Plage(LE_11_MAI_A_10H, Optional.empty()));
  }

  @Test
  void shouldIntersectOpenPlageWithClosedOne() {
    Plage intersection = new Plage(LE_11_MAI_A_9H, Optional.empty())
      .intersection(new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_11H)))
      .orElseThrow();

    assertThat(intersection).isEqualTo(new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_11H)));
  }

  @Test
  void shouldNotIntersectAdjacentPlages() {
    assertThat(
      new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_10H)).intersection(new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_11H)))
    ).isEmpty();
  }

  @Test
  void shouldCloseOpenPlageAtInstant() {
    assertThat(new Plage(LE_11_MAI_A_9H, Optional.empty()).fermee(LE_11_MAI_A_10H)).isEqualTo(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H));
  }

  @Test
  void shouldKeepFinOfClosedPlage() {
    assertThat(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_10H)).fermee(LE_11_MAI_A_12H)).isEqualTo(
      new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H)
    );
  }
}
