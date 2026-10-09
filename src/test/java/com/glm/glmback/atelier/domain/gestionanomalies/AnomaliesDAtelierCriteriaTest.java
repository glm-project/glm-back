package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class AnomaliesDAtelierCriteriaTest {

  @Test
  void shouldExigerLesDeuxTextesDeRecherche() {
    assertThatThrownBy(() -> new AnomaliesDAtelierCriteria(null, "")).isInstanceOf(
      com.glm.glmback.shared.error.domain.MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new AnomaliesDAtelierCriteria("", null)).isInstanceOf(
      com.glm.glmback.shared.error.domain.MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldAppliquerLesMemesRecherchesAUneFinAutomatique() {
    var operateur = operateurJeanMartinPourcent();
    var ouvrant = debutDu9Janvier2043A8hPar(operateur.id());
    var ligne = ligneDeLaFinAutomatiqueDe(suiviPourFiltre2043(elementLitteralePourcentSoulignementAntislash2043()).enregistre(ouvrant));
    var annuaire = new AnnuaireDAtelier(Map.of(operateur.id(), operateur), Map.of());

    assertThat(new AnomaliesDAtelierCriteria("mArTiN_%", "lItTeRaLe_%\\2043").matches(ligne, annuaire)).isTrue();
    assertThat(new AnomaliesDAtelierCriteria("", "").matches(ligne, annuaire)).isTrue();
    assertThat(new AnomaliesDAtelierCriteria("inconnu", "").matches(ligne, annuaire)).isFalse();
    assertThat(new AnomaliesDAtelierCriteria("", "inconnu").matches(ligne, annuaire)).isFalse();
    assertThat(
      new AnomaliesDAtelierCriteria(
        ouvrant.operateur().uuid().toString().substring(0, 12).toUpperCase(java.util.Locale.ROOT),
        ligne.element().id().uuid().toString().substring(0, 12)
      ).matches(ligne, new AnnuaireDAtelier(Map.of(), Map.of()))
    ).isTrue();
  }
}
