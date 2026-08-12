package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class SuivisDAtelierEnMemoireTest {

  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();

  @Test
  void shouldNotCreateTwiceTheSameIdentite() {
    SuiviDAtelier suivi = suiviDAtelierEngage();
    suivis.create(suivi);

    assertThatThrownBy(() -> suivis.create(suivi))
      .isExactlyInstanceOf(SuiviDAtelierDejaExistantException.class)
      .hasMessageContaining("deja");
  }

  @Test
  void shouldNotUpdateUnknownIdentite() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThatThrownBy(() -> suivis.update(suivi)).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  @Test
  void shouldCreateThenGet() {
    SuiviDAtelier suivi = suivis.create(suiviDAtelierEngage());

    assertThat(suivis.get(suivi.id())).contains(suivi);
    assertThat(suivis.getEnCoursPour(ELEMENT_OF_2026_000042)).contains(suivi);
  }

  @Test
  void shouldNotGetEnCoursPourUnElementCloture() {
    SuiviDAtelier suivi = suivis.create(suiviDAtelierEngage());
    suivis.update(suivi.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H)));

    assertThat(suivis.getEnCoursPour(ELEMENT_OF_2026_000042)).isEmpty();
  }

  @Test
  void shouldListerLesSuivisDeLaPeriodeDuPlusRecentAuPlusAncien() {
    SuiviDAtelier suivi = suivis.create(suiviDAtelierEngage());

    assertThat(
      suivis.list(new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of()), firstPageOfTen()).content()
    ).containsExactly(suivi);
  }
}
