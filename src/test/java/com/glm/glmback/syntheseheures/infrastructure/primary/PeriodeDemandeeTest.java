package com.glm.glmback.syntheseheures.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.syntheseheures.domain.MoisCalendaire;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@UnitTest
class PeriodeDemandeeTest {

  @Test
  void shouldLireUneSemaine() {
    assertThat(PeriodeDemandee.de(2026, Optional.of(38), Optional.empty())).isEqualTo(new SemaineCalendaire(2026, 38));
  }

  @Test
  void shouldLireUnMois() {
    assertThat(PeriodeDemandee.de(2026, Optional.empty(), Optional.of(9))).isEqualTo(new MoisCalendaire(2026, 9));
  }

  @Test
  void shouldRefuserNiSemaineNiMois() {
    assertThatThrownBy(() -> PeriodeDemandee.de(2026, Optional.empty(), Optional.empty()))
      .isExactlyInstanceOf(ResponseStatusException.class)
      .extracting(erreur -> ((ResponseStatusException) erreur).getStatusCode())
      .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldRefuserUneSemaineEtUnMois() {
    assertThatThrownBy(() -> PeriodeDemandee.de(2026, Optional.of(38), Optional.of(9)))
      .isExactlyInstanceOf(ResponseStatusException.class)
      .extracting(erreur -> ((ResponseStatusException) erreur).getStatusCode())
      .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
