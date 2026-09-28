package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeLaSyntheseTest {

  private static final PosteDeLElement DMU_50_EN_FRAISAGE = new PosteDeLElement(POSTE_CONNU_DMU_50, Optional.of(NATURE_FRAISAGE));

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(null, Optional.empty(), Optional.empty(), Duration.ZERO, Duration.ZERO, Duration.ZERO, List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutReference() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, null, Optional.empty(), Duration.ZERO, Duration.ZERO, Duration.ZERO, List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, Optional.empty(), null, Duration.ZERO, Duration.ZERO, Duration.ZERO, List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildWithoutDuree() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, Optional.empty(), Optional.empty(), null, Duration.ZERO, Duration.ZERO, List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree");
  }

  @Test
  void shouldNotBuildWithoutDureeNonConformite() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, Optional.empty(), Optional.empty(), Duration.ZERO, null, Duration.ZERO, List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree de non conformite");
  }

  @Test
  void shouldNotBuildWithoutDureePresumee() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, Optional.empty(), Optional.empty(), Duration.ZERO, Duration.ZERO, null, List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree presumee");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    assertThatThrownBy(() ->
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, Optional.empty(), Optional.empty(), Duration.ZERO, Duration.ZERO, Duration.ZERO, null)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldNotBuildWithNullPoste() {
    List<PosteDeLElement> postes = Arrays.asList(DMU_50_EN_FRAISAGE, null);

    assertThatThrownBy(() ->
      new ElementDeLaSynthese(
        ELEMENT_ENGAGE_CARTER,
        Optional.empty(),
        Optional.empty(),
        Duration.ZERO,
        Duration.ZERO,
        Duration.ZERO,
        postes
      )
    )
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldPorterSonElementSaFicheSesDureesEtSesPostes() {
    ElementDeLaSynthese element = ElementDeLaSynthese.builder()
      .element(ELEMENT_ENGAGE_CARTER)
      .reference(Optional.of(REFERENCE_1015))
      .description(Optional.of(DESCRIPTION_CARTER_DE_POMPE))
      .duree(Duration.ofHours(8))
      .dureeNonConformite(Duration.ofMinutes(50))
      .dureePresumee(Duration.ofHours(1))
      .postes(List.of(DMU_50_EN_FRAISAGE));

    assertThat(element.element()).isEqualTo(ELEMENT_ENGAGE_CARTER);
    assertThat(element.reference()).contains(REFERENCE_1015);
    assertThat(element.description()).contains(DESCRIPTION_CARTER_DE_POMPE);
    assertThat(element.duree()).isEqualTo(Duration.ofHours(8));
    assertThat(element.dureeNonConformite()).isEqualTo(Duration.ofMinutes(50));
    assertThat(element.dureePresumee()).isEqualTo(Duration.ofHours(1));
    assertThat(element.postes()).containsExactly(DMU_50_EN_FRAISAGE);
  }
}
