package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteInterpreteeTest {

  @Test
  void shouldConserverUneActiviteAResoudreSansFinMalgreLEcheance() {
    ActiviteInterpretee activite = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()))
      .echeance(LE_LUNDI_11_MAI_2026_A_20H)
      .finAuPlusTard(Optional.of(LE_LUNDI_11_MAI_2026_A_20H));
    IntervalleDActivite lue = activite.a(LE_MARDI_12_MAI_2026_A_10H);
    assertThat(lue.lecture().etat()).isEqualTo(EtatDActivite.A_RESOUDRE);
    assertThat(lue.plage().fin()).isEmpty();
  }
}
