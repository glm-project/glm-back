package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolution;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestActeDeResolutionTest {

  @Test
  void shouldEchoLaCorrectionAvecLInstantExactementDemande() {
    var correction = correctionDeLaFinAvecNeufDecimales();
    var json = JsonMapper.builder().build().valueToTree(RestActeDeResolution.from(correction));
    assertThat(json.path("kind").asString()).isEqualTo("CORRECTION");
    assertThat(json.path("pointage").asString()).isEqualTo(correction.commande().evenement().uuid().toString());
    assertThat(json.at("/fait/instant").asString()).isEqualTo("2026-05-10T19:00:00.123456789+02:00");
    assertThat(json.at("/fait/activiteVisee").asString()).isEqualTo(
      correction.commande().remplacement().activiteVisee().orElseThrow().uuid().toString()
    );
    assertThat(json.has("auteur")).isFalse();
  }

  @Test
  void shouldEchoLAnnulationAvecLeMotifEtLePointage() {
    var proposition = propositionDAnnulationDeTransition(suiviAvecTransitionDeMemeCategorie());
    var json = JsonMapper.builder().build().valueToTree(RestActeDeResolution.from(proposition.acte()));
    assertThat(json.path("kind").asString()).isEqualTo("ANNULATION");
    assertThat(json.path("pointage").asString()).isEqualTo(proposition.adresse().pointage().uuid().toString());
    assertThat(json.path("motif").asString()).isEqualTo("Erreur de saisie");
    assertThat(json.has("auteur")).isFalse();
  }

  @Test
  void shouldEchoLaRegularisationAvecLInstantExactEtSansIdentiteDuSuivi() {
    var proposition = propositionDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var json = JsonMapper.builder().build().valueToTree(RestActeDeResolution.from(proposition.acte()));
    assertThat(json.path("kind").asString()).isEqualTo("REGULARISATION");
    assertThat(json.at("/fait/instant").asString()).isEqualTo("2026-05-10T14:00:00.123456789+02:00");
    assertThat(json.at("/fait/type").asString()).isEqualTo("FIN");
    assertThat(json.has("auteur")).isFalse();
    assertThat(json.has("suivi")).isFalse();
  }

  @Test
  void shouldLireLAnnulationAvecLeSuiviEtLAuteurDeLaRequete() {
    var proposition = propositionDAnnulationDeTransition(suiviAvecTransitionDeMemeCategorie());
    var wire = RestActeDeResolution.from(proposition.acte());
    var acte = wire.toDomain(proposition.adresse().suivi(), GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(proposition.acte().memeDemandeQue(acte)).isTrue();
    assertThat(acte).isInstanceOf(ActeDeResolution.Annulation.class);
    var annulation = ((ActeDeResolution.Annulation) acte).commande();
    assertThat(annulation.suivi()).isEqualTo(proposition.adresse().suivi());
    assertThat(annulation.evenement()).isEqualTo(proposition.adresse().pointage());
    assertThat(annulation.auteur()).isEqualTo(GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(annulation.motif().value()).isEqualTo("Erreur de saisie");
  }

  @Test
  void shouldLireLaRegularisationSansPerdreLeDecalageNiLesNeufDecimales() {
    var proposition = propositionDeRegularisationDeFin(suiviAvecTransitionDeMemeCategorie());
    var wire = RestActeDeResolution.from(proposition.acte());
    var acte = wire.toDomain(proposition.adresse().suivi(), GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(proposition.acte().memeDemandeQue(acte)).isTrue();
    assertThat(acte).isInstanceOf(ActeDeResolution.Regularisation.class);
    var regularisation = (ActeDeResolution.Regularisation) acte;
    assertThat(regularisation.instant()).isEqualTo("2026-05-10T14:00:00.123456789+02:00");
    assertThat(regularisation.commande().dateDeSurvenue()).isEqualTo(java.time.Instant.parse("2026-05-10T12:00:00.123456789Z"));
    assertThat(regularisation.commande().auteur()).isEqualTo(GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(regularisation.commande().suivi()).isEqualTo(proposition.adresse().suivi());
    assertThat(regularisation.commande().poste()).isEmpty();
  }

  @Test
  void shouldLireLaCorrectionAvecSonLienEtLAuteurDeLaRequete() {
    var correction = correctionDeLaFinAvecNeufDecimales();
    var wire = RestActeDeResolution.from(correction);
    var suivi = correction.commande().remplacement().suivi();
    var acte = wire.toDomain(suivi, GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(acte).isInstanceOf(ActeDeResolution.Correction.class);
    var lu = (ActeDeResolution.Correction) acte;
    assertThat(lu.instant()).isEqualTo("2026-05-10T19:00:00.123456789+02:00");
    assertThat(lu.commande().evenement()).isEqualTo(correction.commande().evenement());
    assertThat(lu.commande().motif()).isEqualTo(correction.commande().motif());
    assertThat(lu.commande().remplacement().auteur()).isEqualTo(GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(lu.commande().remplacement().poste()).isEqualTo(correction.commande().remplacement().poste());
    assertThat(lu.commande().remplacement().activiteVisee()).isEqualTo(correction.commande().remplacement().activiteVisee());
  }

  @Test
  void shouldGarderLaMemeCorrectionApresUnChangementDuNomDAuteur() {
    var proposition = propositionDeCorrectionDeTransition(suiviAvecTransitionDeMemeCategorie());
    var acte = RestActeDeResolution.from(proposition.acte()).toDomain(proposition.adresse().suivi(), GESTIONNAIRE_LEROY_RENOMME.auteur());
    assertThat(proposition.acte().memeDemandeQue(acte)).isTrue();
    assertThat(acte).isNotEqualTo(proposition.acte());
  }
}
