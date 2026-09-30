package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.AssertionException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteInterpreteeTest {

  @Test
  void shouldRequireAnActivity() {
    assertThatThrownBy(() -> new ActiviteInterpretee(null, new Plage(LE_11_MAI_A_8H, Optional.empty()), LE_11_MAI_A_21H)).isInstanceOf(
      AssertionException.class
    );
  }

  @Test
  void shouldRequireBounds() {
    assertThatThrownBy(() -> new ActiviteInterpretee(activite(), null, LE_11_MAI_A_21H)).isInstanceOf(AssertionException.class);
  }

  @Test
  void shouldRequireADeadline() {
    assertThatThrownBy(() -> new ActiviteInterpretee(activite(), new Plage(LE_11_MAI_A_8H, Optional.empty()), null)).isInstanceOf(
      AssertionException.class
    );
  }

  private static Activite activite() {
    return Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .categorie(CategorieDActivite.TRAVAIL);
  }
}
