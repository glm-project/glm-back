package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import static com.glm.glmback.BeanValidationAssertions.*;
import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class RestFaitDeResolutionTest {

  @Test
  void shouldRefuserUneOuverturePointeeCommeUneFin() {
    var proposition = propositionDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var fait = ((RestActeDeResolution.Regularisation) RestActeDeResolution.from(proposition.acte())).fait();
    assertThatBean(fait).isValid();
    var incoherent = new RestFaitDeResolution(
      fait.type(),
      IntentionDePointage.OUVERTURE,
      fait.activiteVisee(),
      fait.operateur(),
      fait.poste(),
      fait.instant()
    );
    assertThatBean(incoherent).hasInvalidProperty("intentionAdmiseParLeType");
  }

  @Test
  void shouldRefuserUneFinSansActiviteVisee() {
    var proposition = propositionDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var fait = ((RestActeDeResolution.Regularisation) RestActeDeResolution.from(proposition.acte())).fait();
    var incoherent = new RestFaitDeResolution(fait.type(), fait.intention(), null, fait.operateur(), fait.poste(), fait.instant());
    assertThatBean(incoherent).hasInvalidProperty("cibleConformeALIntention");
  }

  @Test
  void shouldRefuserUnInstantQuiNePeutPasEtreLuSansPasserAuDomaine() {
    var proposition = propositionDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var fait = ((RestActeDeResolution.Regularisation) RestActeDeResolution.from(proposition.acte())).fait();
    var incoherent = new RestFaitDeResolution(
      fait.type(),
      fait.intention(),
      fait.activiteVisee(),
      fait.operateur(),
      fait.poste(),
      "hier a midi"
    );
    assertThatBean(incoherent).hasInvalidProperty("instantValide");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { " " })
  void shouldLaisserLInstantManquantASaContrainteObligatoire(String instant) {
    var proposition = propositionDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var fait = ((RestActeDeResolution.Regularisation) RestActeDeResolution.from(proposition.acte())).fait();
    var incomplet = new RestFaitDeResolution(fait.type(), fait.intention(), fait.activiteVisee(), fait.operateur(), fait.poste(), instant);
    assertThatBean(incomplet).hasInvalidProperty("instant");
  }
}
