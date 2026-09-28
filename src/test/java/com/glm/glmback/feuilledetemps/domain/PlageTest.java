package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PlageTest {

  private static final Instant A_8H = Instant.parse("2026-05-11T06:00:00Z");
  private static final Instant A_10H = Instant.parse("2026-05-11T08:00:00Z");
  private static final Instant A_12H = Instant.parse("2026-05-11T10:00:00Z");
  private static final Instant A_14H = Instant.parse("2026-05-11T12:00:00Z");

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new Plage(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new Plage(A_8H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new Plage(A_12H, Optional.of(A_8H)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldBeOuverteWithoutFin() {
    assertThat(new Plage(A_8H, Optional.empty()).estOuverte()).isTrue();
  }

  @Test
  void shouldNotBeOuverteWithFin() {
    assertThat(new Plage(A_8H, Optional.of(A_12H)).estOuverte()).isFalse();
  }

  @Test
  void shouldEtrePointeeParDefaut() {
    assertThat(new Plage(A_8H, Optional.empty()).presumee()).isFalse();
  }

  @Test
  void shouldContenirSesBornesEtCeQuiEstEntre() {
    Plage plage = new Plage(A_8H, Optional.of(A_12H));

    assertThat(plage.contient(A_8H)).isTrue();
    assertThat(plage.contient(A_12H)).isTrue();
    assertThat(plage.contient(A_8H.minusSeconds(1))).isFalse();
    assertThat(plage.contient(A_12H.plusSeconds(1))).isFalse();
  }

  @Test
  void shouldContenirToutCeQuiSuitQuandElleEstOuverte() {
    assertThat(new Plage(A_8H, Optional.empty()).contient(A_12H.plusSeconds(86_400))).isTrue();
  }

  @Test
  void shouldGarderLaPartCommuneADeuxPlages() {
    Plage travail = new Plage(A_8H, Optional.of(A_12H));

    assertThat(travail.intersection(new Plage(A_10H, Optional.of(A_14H)))).contains(new Plage(A_10H, Optional.of(A_12H)));
  }

  @Test
  void shouldCroiserDeuxPlagesOuvertesEnUnePlageOuverte() {
    assertThat(new Plage(A_8H, Optional.empty()).intersection(new Plage(A_10H, Optional.empty()))).contains(
      new Plage(A_10H, Optional.empty())
    );
  }

  @Test
  void shouldFermerUnePlageOuverteALaFinDeLaFenetre() {
    assertThat(new Plage(A_10H, Optional.empty()).intersection(new Plage(A_8H, Optional.of(A_12H)))).contains(
      new Plage(A_10H, Optional.of(A_12H))
    );
  }

  @Test
  void shouldNeRienRendreDeDeuxPlagesDisjointes() {
    assertThat(new Plage(A_8H, Optional.of(A_10H)).intersection(new Plage(A_12H, Optional.empty()))).isEmpty();
  }

  /**
   * Un depart pointe a l'instant ou le travail commence ne doit pas ouvrir une activite sur du vide.
   */
  @Test
  void shouldNeRienRendreDeDeuxPlagesQuiNeFontQueSeToucher() {
    assertThat(new Plage(A_8H, Optional.of(A_10H)).intersection(new Plage(A_10H, Optional.of(A_12H)))).isEmpty();
  }

  @Test
  void shouldDevenirPresumeeDansUneFenetrePresumee() {
    Plage fenetrePresumee = new Plage(A_8H, Optional.of(A_12H), true);

    assertThat(new Plage(A_10H, Optional.empty()).intersection(fenetrePresumee)).contains(new Plage(A_10H, Optional.of(A_12H), true));
  }

  @Test
  void shouldResterPresumeeDansUneFenetrePointee() {
    Plage presumee = new Plage(A_8H, Optional.of(A_12H), true);

    assertThat(presumee.intersection(new Plage(A_10H, Optional.empty()))).contains(new Plage(A_10H, Optional.of(A_12H), true));
  }
}
