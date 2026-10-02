package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class AnnuaireDuCoutTest {

  private static final AnnuaireDuCout ANNUAIRE = new AnnuaireDuCout(
    List.of(OPERATEUR_NOMME_JEAN_DUPONT),
    List.of(POSTE_NOMME_DMG_DMU_50),
    List.of(ELEMENT_VALORISE_OF)
  );

  @Test
  void shouldNotBuildWithoutOperateurs() {
    assertThatThrownBy(() -> new AnnuaireDuCout(null, List.of(), List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateurs");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    assertThatThrownBy(() -> new AnnuaireDuCout(List.of(), null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldNotBuildWithoutElements() {
    assertThatThrownBy(() -> new AnnuaireDuCout(List.of(), List.of(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("elements");
  }

  @Test
  void shouldFindWhatItNames() {
    assertThat(ANNUAIRE.operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_NOMME_JEAN_DUPONT);
    assertThat(ANNUAIRE.poste(POSTE_ID_FRAISEUSE)).contains(POSTE_NOMME_DMG_DMU_50);
    assertThat(ANNUAIRE.element(ELEMENT_ID_OF)).contains(ELEMENT_VALORISE_OF);
  }

  /**
   * Un nom absent ne fait pas echouer le rapport : le detail garde alors l'identifiant seul.
   */
  @Test
  void shouldNotFindWhatItDoesNotName() {
    assertThat(ANNUAIRE.operateur(OPERATEUR_ID_MARTIN)).isEmpty();
    assertThat(ANNUAIRE.poste(POSTE_ID_TOUR)).isEmpty();
    assertThat(ANNUAIRE.element(ELEMENT_ID_OF_2)).isEmpty();
  }
}
