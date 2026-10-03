package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.BeanValidationAssertions.*;
import static com.glm.glmback.atelier.application.ResolutionFixture.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import org.junit.jupiter.api.Test;

@UnitTest
class RestFaitDeResolutionTest {

  @Test
  void shouldRefuserUneOuverturePointeeCommeUneFin() {
    var preuve = preuveDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var fait = ((RestActeDeResolution.Regularisation) RestActeDeResolution.from(preuve.acte())).fait();
    assertThatBean(fait).isValid();
    var incoherent = new RestFaitDeResolution(
      fait.type(), IntentionDePointage.OUVERTURE, fait.activiteVisee(), fait.operateur(), fait.poste(), fait.instant()
    );
    assertThatBean(incoherent).hasInvalidProperty("intentionAdmiseParLeType");
  }

  @Test
  void shouldRefuserUneFinSansActiviteVisee() {
    var preuve = preuveDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var fait = ((RestActeDeResolution.Regularisation) RestActeDeResolution.from(preuve.acte())).fait();
    var incoherent = new RestFaitDeResolution(fait.type(), fait.intention(), null, fait.operateur(), fait.poste(), fait.instant());
    assertThatBean(incoherent).hasInvalidProperty("cibleConformeALIntention");
  }
}
