package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.AssertionException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PlageTest {

  @Test
  void shouldRequireAStart() {
    assertThatThrownBy(() -> new Plage(null, Optional.empty())).isInstanceOf(AssertionException.class);
  }

  @Test
  void shouldRequireAnOptionalEnd() {
    assertThatThrownBy(() -> new Plage(LE_11_MAI_A_8H, null)).isInstanceOf(AssertionException.class);
  }

  @Test
  void shouldRejectAnEndBeforeTheStart() {
    assertThatThrownBy(() -> new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_8H))).isInstanceOf(AssertionException.class);
  }

  @Test
  void shouldAcceptBothOpenAndClosedBounds() {
    assertThat(new Plage(LE_11_MAI_A_8H, Optional.empty()).fin()).isEmpty();
    assertThat(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_8H)).fin()).contains(LE_11_MAI_A_8H);
  }
}
