package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.ConflitsFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class AnomaliesDAtelierCriteriaTest {

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

    assertThat(new AnomaliesDAtelierCriteria("mArTiN_%", "lItTeRaLe_%\\2043").matches(ligne, annuaire)).isTrue();
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
    var criteria = new AnomaliesDAtelierCriteria(
      ouvrant.operateur().uuid().toString().substring(0, 12).toUpperCase(java.util.Locale.ROOT),
      suivi.element().id().uuid().toString().substring(0, 12)
    );

    assertThat(criteria.matches(ligne, annuaire)).isTrue();
  }

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
  void shouldExigerQueLesDeuxRecherchesCorrespondent() {
    var ouvrant = debutDu9Janvier2043A8hPar(OPERATEUR_ID_DUPONT);
    var ligne = ligneDuPremierConflitDe(
      suiviPourFiltre2043(elementFiltrePourcentA())
        .enregistre(ouvrant)
        .enregistre(finDe(ouvrant).a(ouvrant.dateDeSurvenue().plusSeconds(3600)))
        .enregistre(finDe(ouvrant).a(ouvrant.dateDeSurvenue().plusSeconds(7200)))
    );
    var annuaire = new AnnuaireDAtelier(Map.of(), Map.of());

    assertThat(new AnomaliesDAtelierCriteria("", "").matches(ligne, annuaire)).isTrue();
    assertThat(new AnomaliesDAtelierCriteria("inconnu", "").matches(ligne, annuaire)).isFalse();
    assertThat(new AnomaliesDAtelierCriteria("", "inconnu").matches(ligne, annuaire)).isFalse();
    assertThat(new AnomaliesDAtelierCriteria("", "FILTREx2043").matches(ligne, annuaire)).isFalse();
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
