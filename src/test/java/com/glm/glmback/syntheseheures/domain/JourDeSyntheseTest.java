package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class JourDeSyntheseTest {

  private static final LocalDate LUNDI_11_MAI_2026 = LocalDate.of(2026, 5, 11);
  private static final EvenementDePresence ARRIVEE = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);

  @Test
  void shouldNotBuildWithoutJour() {
    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(null)
        .pointages(List.of())
        .duree(Duration.ZERO)
        .dureePresumee(Duration.ZERO)
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(Duration.ZERO)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jour");
  }

  @Test
  void shouldNotBuildWithoutPointages() {
    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(null)
        .duree(Duration.ZERO)
        .dureePresumee(Duration.ZERO)
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(Duration.ZERO)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("pointages");
  }

  @Test
  void shouldNotBuildWithNullPointage() {
    List<PointageDuJour> pointages = Arrays.asList(ARRIVEE, null);

    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(pointages)
        .duree(Duration.ZERO)
        .dureePresumee(Duration.ZERO)
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(Duration.ZERO)
    )
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("pointages");
  }

  @Test
  void shouldNotBuildWithoutDuree() {
    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(List.of())
        .duree(null)
        .dureePresumee(Duration.ZERO)
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(Duration.ZERO)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree");
  }

  @Test
  void shouldPorterSonJourSesPointagesEtSaDuree() {
    JourDeSynthese jour = JourDeSynthese.builder()
      .jour(LUNDI_11_MAI_2026)
      .pointages(List.of(ARRIVEE))
      .duree(Duration.ofHours(8))
      .dureePresumee(Duration.ZERO)
      .dureeOperationnelle(Duration.ZERO)
      .dureeOperationnellePresumee(Duration.ZERO);

    assertThat(jour.jour()).isEqualTo(LUNDI_11_MAI_2026);
    assertThat(jour.pointages()).containsExactly(ARRIVEE);
    assertThat(jour.duree()).isEqualTo(Duration.ofHours(8));
  }

  @Test
  void shouldNotBuildWithoutDureePresumee() {
    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(List.of())
        .duree(Duration.ZERO)
        .dureePresumee(null)
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(Duration.ZERO)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree presumee");
  }

  @Test
  void shouldPorterSaDureePresumee() {
    assertThat(
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(List.of())
        .duree(Duration.ZERO)
        .dureePresumee(Duration.ofHours(3))
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(Duration.ZERO)
        .dureePresumee()
    ).isEqualTo(Duration.ofHours(3));
  }

  @Test
  void shouldNotBuildWithoutDureeOperationnelle() {
    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(List.of())
        .duree(Duration.ZERO)
        .dureePresumee(Duration.ZERO)
        .dureeOperationnelle(null)
        .dureeOperationnellePresumee(Duration.ZERO)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree operationnelle");
  }

  @Test
  void shouldNotBuildWithoutDureeOperationnellePresumee() {
    assertThatThrownBy(() ->
      JourDeSynthese.builder()
        .jour(LUNDI_11_MAI_2026)
        .pointages(List.of())
        .duree(Duration.ZERO)
        .dureePresumee(Duration.ZERO)
        .dureeOperationnelle(Duration.ZERO)
        .dureeOperationnellePresumee(null)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree operationnelle presumee");
  }

  @Test
  void shouldPorterSonTempsOperationnel() {
    JourDeSynthese jour = JourDeSynthese.builder()
      .jour(LUNDI_11_MAI_2026)
      .pointages(List.of())
      .duree(Duration.ZERO)
      .dureePresumee(Duration.ZERO)
      .dureeOperationnelle(Duration.ofHours(13))
      .dureeOperationnellePresumee(Duration.ofHours(2));

    assertThat(jour.dureeOperationnelle()).isEqualTo(Duration.ofHours(13));
    assertThat(jour.dureeOperationnellePresumee()).isEqualTo(Duration.ofHours(2));
  }
}
