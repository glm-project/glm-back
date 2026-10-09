package com.glm.glmback.postedetravail.infrastructure.primary;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.postedetravail.domain.NatureChoisie;
import org.junit.jupiter.api.Test;

@UnitTest
@SuppressWarnings("removal")
class NatureDemandeeTest {

  @Test
  void shouldBeDesignatedByItsId() {
    assertThat(NatureDemandee.estDesignee(NATURE_DE_TRAVAIL_ID_TOURNAGE.uuid(), null)).isTrue();
  }

  @Test
  void shouldBeDesignatedByItsLibelle() {
    assertThat(NatureDemandee.estDesignee(null, "tournage")).isTrue();
  }

  @Test
  void shouldNotBeDesignatedByNothing() {
    assertThat(NatureDemandee.estDesignee(null, null)).isFalse();
  }

  @Test
  void shouldNotBeDesignatedByBlankLibelle() {
    assertThat(NatureDemandee.estDesignee(null, "  ")).isFalse();
  }

  @Test
  void shouldPreferIdToLibelle() {
    assertThat(NatureDemandee.choisie(NATURE_DE_TRAVAIL_ID_TOURNAGE.uuid(), "fraisage")).isEqualTo(NATURE_CHOISIE_TOURNAGE);
  }

  @Test
  void shouldChooseByLibelleWithoutId() {
    assertThat(NatureDemandee.choisie(null, "tournage")).isEqualTo(new NatureChoisie.ParLibelle(NATURE_TOURNAGE));
  }
}
