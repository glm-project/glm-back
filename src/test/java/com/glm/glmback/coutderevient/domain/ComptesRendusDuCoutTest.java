package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ComptesRendusDuCoutTest {

  @Test
  void shouldMettreEnFormeLeRapportQueLEcranLit() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H))));

    CompteRenduDuCout compteRendu = comptesRendus(atelier).compteRendu(ELEMENT_ID_OF);

    assertThat(compteRendu.fuseau()).isEqualTo(FUSEAU_DE_PARIS);
    assertThat(compteRendu.rapport().lecture().evaluation()).isEqualTo(LE_11_MAI_A_17H);
    assertThat(compteRendu.rapport().cout().total().valeur()).isEqualTo(new Montant(new BigDecimal("130.00")));
  }

  @Test
  void shouldRefuserUnElementInconnu() {
    assertThatThrownBy(() -> comptesRendus(new AtelierEnMemoire()).compteRendu(ELEMENT_ID_OF)).isExactlyInstanceOf(
      ElementInconnuException.class
    );
  }

  private static ComptesRendusDuCout comptesRendus(AtelierEnMemoire atelier) {
    CoutsDeRevientService coutsDeRevient = CoutsDeRevientService.builder()
      .elements(atelier)
      .travaux(atelier)
      .occupations(atelier)
      .operateursNommes(atelier)
      .postesNommes(atelier)
      .clock(() -> LE_11_MAI_A_17H);
    return new ComptesRendusDuCout(coutsDeRevient, () -> FUSEAU_DE_PARIS);
  }
}
