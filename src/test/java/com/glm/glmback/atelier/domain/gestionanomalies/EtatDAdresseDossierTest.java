package com.glm.glmback.atelier.domain.gestionanomalies;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class EtatDAdresseDossierTest {

  @ParameterizedTest
  @EnumSource(value = EtatDAdresseDossier.class, names = { "EN_CONFLIT", "FIN_AUTOMATIQUE" })
  void shouldOuvrirUneAnomalieATraiter(EtatDAdresseDossier etat) {
    assertThat(etat.estATraiter()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = EtatDAdresseDossier.class, names = { "INTROUVABLE", "ANCRE_ANNULEE", "SANS_ANOMALIE" })
  void shouldNePasOuvrirDAnomalieATraiter(EtatDAdresseDossier etat) {
    assertThat(etat.estATraiter()).isFalse();
  }
}
