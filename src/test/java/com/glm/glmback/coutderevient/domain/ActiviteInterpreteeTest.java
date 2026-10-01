package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.AssertionException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteInterpreteeTest {

  @Test
  void shouldRequireAnActivity() {
    assertThatThrownBy(() ->
      new ActiviteInterpretee(
        new ActiviteId(java.util.UUID.randomUUID()),
        null,
        new Plage(LE_11_MAI_A_8H, Optional.empty()),
        LE_11_MAI_A_21H,
        Optional.empty()
      )
    ).isInstanceOf(AssertionException.class);
  }

  @Test
  void shouldRequireBounds() {
    assertThatThrownBy(() ->
      new ActiviteInterpretee(new ActiviteId(java.util.UUID.randomUUID()), ACTIVITE_FRAISAGE, null, LE_11_MAI_A_21H, Optional.empty())
    ).isInstanceOf(AssertionException.class);
  }

  @Test
  void shouldRequireADeadline() {
    assertThatThrownBy(() ->
      new ActiviteInterpretee(
        new ActiviteId(java.util.UUID.randomUUID()),
        ACTIVITE_FRAISAGE,
        new Plage(LE_11_MAI_A_8H, Optional.empty()),
        null,
        Optional.empty()
      )
    ).isInstanceOf(AssertionException.class);
  }
}
