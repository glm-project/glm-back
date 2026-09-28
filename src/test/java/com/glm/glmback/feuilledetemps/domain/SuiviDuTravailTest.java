package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDuTravailTest {

  private static final JournalDAtelier JOURNAL_DU_CARTER = new JournalDAtelier(List.of(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H)));

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() -> new SuiviDuTravail(null, JOURNAL_DU_CARTER, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new SuiviDuTravail(ELEMENT_ID_CARTER, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldNotBuildWithoutCloture() {
    assertThatThrownBy(() -> new SuiviDuTravail(ELEMENT_ID_CARTER, JOURNAL_DU_CARTER, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cloture");
  }

  @Test
  void shouldLaisserOuvertUnTravailEnCoursSurUnSuiviOuvert() {
    assertThat(new SuiviDuTravail(ELEMENT_ID_CARTER, JOURNAL_DU_CARTER, Optional.empty()).intervalles()).containsExactly(
      new IntervalleDActivite(activiteDeTravailDuCarterSurLaDmu50(), new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()))
    );
  }

  @Test
  void shouldArreterALaClotureLeTravailQuePersonneNAArrete() {
    assertThat(
      new SuiviDuTravail(ELEMENT_ID_CARTER, JOURNAL_DU_CARTER, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)).intervalles()
    ).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      )
    );
  }
}
