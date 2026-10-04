package com.glm.glmback.atelier.gestionconflits.domain;

import static com.glm.glmback.atelier.gestionconflits.domain.ActeDeResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import org.junit.jupiter.api.Test;

@UnitTest
class ActeDeResolutionTest {

  @Test
  void shouldIdentifierLaCorrectionEtConserverLInstantSaisiExactement() {
    ActeDeResolution.Correction correction = correctionDeLaFinAvecNeufDecimales();

    assertThat(correction.kind()).isEqualTo(TypeDActeDeResolution.CORRECTION);
    assertThat(correction.instant()).isEqualTo("2026-05-10T19:00:00.123456789+02:00");
  }

  @Test
  void shouldIdentifierLAnnulation() {
    var correction = correctionDeLaFinAvecNeufDecimales();
    var commande = AnnulationAEnregistrer.builder()
      .suivi(correction.commande().remplacement().suivi())
      .evenement(correction.commande().evenement())
      .auteur(correction.commande().remplacement().auteur())
      .motif(correction.commande().motif());

    assertThat(new ActeDeResolution.Annulation(commande).kind()).isEqualTo(TypeDActeDeResolution.ANNULATION);
  }

  @Test
  void shouldIdentifierLaRegularisationSansPerdreLInstantSaisi() {
    var correction = correctionDeLaFinAvecNeufDecimales();
    var acte = new ActeDeResolution.Regularisation(correction.commande().remplacement(), correction.instant());

    assertThat(acte.kind()).isEqualTo(TypeDActeDeResolution.REGULARISATION);
    assertThat(acte.instant()).isEqualTo(correction.instant());
  }
}
