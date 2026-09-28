package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
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
    assertThatThrownBy(() -> new SuiviDuTravail(ELEMENT_ENGAGE_CARTER, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldNotBuildWithoutCloture() {
    assertThatThrownBy(() -> new SuiviDuTravail(ELEMENT_ENGAGE_CARTER, JOURNAL_DU_CARTER, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cloture");
  }

  @Test
  void shouldLaisserOuvertUnTravailEnCoursSurUnSuiviOuvert() {
    assertThat(new SuiviDuTravail(ELEMENT_ENGAGE_CARTER, JOURNAL_DU_CARTER, Optional.empty()).intervalles()).containsExactly(
      new IntervalleDActivite(activiteDeTravailDuCarterSurLaDmu50(), new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()))
    );
  }

  @Test
  void shouldArreterALaClotureLeTravailQuePersonneNAArrete() {
    assertThat(
      new SuiviDuTravail(ELEMENT_ENGAGE_CARTER, JOURNAL_DU_CARTER, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)).intervalles()
    ).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      )
    );
  }

  /**
   * Le journal brut garde tous les gestes, meme ceux que l'automate ignore : c'est ce que l'operateur a pointe.
   */
  @Test
  void shouldRendreSesPointagesSurLElementDansLOrdreDuJournal() {
    SuiviDuTravail suivi = new SuiviDuTravail(
      ELEMENT_ENGAGE_CARTER,
      new JournalDAtelier(List.of(finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_7H), debutSansPosteA(LE_LUNDI_11_MAI_2026_A_8H))),
      Optional.empty()
    );

    assertThat(suivi.pointages()).containsExactly(
      new PointageDElement(
        TypeDEvenementDAtelier.FIN,
        ELEMENT_ID_CARTER,
        Optional.of(POSTE_ID_DMU_50),
        Optional.of(NATURE_FRAISAGE),
        LE_LUNDI_11_MAI_2026_A_7H
      ),
      new PointageDElement(TypeDEvenementDAtelier.DEBUT, ELEMENT_ID_CARTER, Optional.empty(), Optional.empty(), LE_LUNDI_11_MAI_2026_A_8H)
    );
  }
}
