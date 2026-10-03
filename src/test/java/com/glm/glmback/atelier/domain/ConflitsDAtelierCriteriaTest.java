package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.ConflitsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class ConflitsDAtelierCriteriaTest {

  @Test
  void shouldCombinerLesTextesPartielsSansCasseEtSansInterpreteLesCaracteresSpeciaux() {
    var operateur = operateurJeanMartinPourcent();
    var ouvrant = debutDu9Janvier2043A8hPar(operateur.id());
    var suivi = suiviPourFiltre2043(elementLitteralePourcentSoulignementAntislash2043())
      .enregistre(ouvrant)
      .enregistre(finDe(ouvrant).a(ouvrant.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(ouvrant).a(ouvrant.dateDeSurvenue().plusSeconds(7200)));
    var ligne = ligneDuPremierConflitDe(suivi);
    var annuaire = new AnnuaireDAtelier(Map.of(operateur.id(), operateur), Map.of());

    assertThat(new ConflitsDAtelierCriteria("mArTiN_%", "lItTeRaLe_%\\2043").matches(ligne, annuaire)).isTrue();
  }

  @Test
  void shouldRetrouverLesIdentifiantsPartielsSansFiches() {
    var ouvrant = debutDu9Janvier2043A8hPar(OPERATEUR_ID_DUPONT);
    var suivi = suiviPourFiltre2043(elementFiltrePourcentA())
      .enregistre(ouvrant)
      .enregistre(finDe(ouvrant).a(ouvrant.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(ouvrant).a(ouvrant.dateDeSurvenue().plusSeconds(7200)));
    var ligne = ligneDuPremierConflitDe(suivi);
    var annuaire = new AnnuaireDAtelier(Map.of(), Map.of());
    var criteria = new ConflitsDAtelierCriteria(
      ouvrant.operateur().uuid().toString().substring(0, 12).toUpperCase(java.util.Locale.ROOT),
      suivi.element().id().uuid().toString().substring(0, 12)
    );

    assertThat(criteria.matches(ligne, annuaire)).isTrue();
  }
}
