package com.glm.glmback.atelier.domain.gestionanomalies;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class NatureDAnomalieTest {

  @Test
  void shouldLireLeNomExactDUneNature() {
    assertThat(NatureDAnomalie.of("CONFLIT")).isEqualTo(NatureDAnomalie.CONFLIT);
    assertThat(NatureDAnomalie.of("FIN_AUTOMATIQUE")).isEqualTo(NatureDAnomalie.FIN_AUTOMATIQUE);
  }

  @Test
  void shouldExigerUneNature() {
    assertThatThrownBy(() -> NatureDAnomalie.of(null))
      .isExactlyInstanceOf(NatureDAnomalieInvalideException.class)
      .hasMessage("La nature d'anomalie est obligatoire. Valeurs possibles : CONFLIT, FIN_AUTOMATIQUE.");
  }

  @ParameterizedTest
  @ValueSource(strings = { "INCONNUE", "conflit", " CONFLIT", "", "ANOMALIE", "fin_automatique", "FIN_AUTOMATIQUE " })
  void shouldRefuserUneNatureInconnue(String valeur) {
    assertThatThrownBy(() -> NatureDAnomalie.of(valeur))
      .isExactlyInstanceOf(NatureDAnomalieInvalideException.class)
      .hasMessage("La nature d'anomalie '" + valeur + "' est inconnue. Valeurs possibles : CONFLIT, FIN_AUTOMATIQUE.");
  }
}
