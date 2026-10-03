package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.Annulation;
import org.junit.jupiter.api.Test;

@UnitTest
class Sha256EmpreintesDesConsequencesTest {

  private final Sha256EmpreintesDesConsequences empreintes = new Sha256EmpreintesDesConsequences(new CryptographieDesReferences());

  @Test
  void shouldIgnorerLaRevisionEtLesMetadonneesDuFaitEnregistre() {
    var relectures = EmpreinteFixture.relecturesDUnDebutRegularise();
    assertThat(empreintes.calcule(relectures.reel(), LE_10_MAI_2026_A_17H)).isEqualTo(
      empreintes.calcule(relectures.previsionnel(), LE_10_MAI_2026_A_17H)
    );
  }

  @Test
  void shouldIgnorerLAuteurEtLInstantTechniquesMaisDetecterLEcheance() {
    var debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(debut).a(LE_10_MAI_2026_A_12H);
    var avant = suiviDAtelierEngage().enregistre(debut).enregistre(transition);
    var treizeHeures = LE_10_MAI_2026_A_12H.plusSeconds(3600);
    var quatorzeHeures = treizeHeures.plusSeconds(3600);
    var previsionnel = avant.annule(transition.id(), new Annulation(AUTEUR_LEROY, treizeHeures, MOTIF_ERREUR_DE_SAISIE));
    var reel = avant.annule(transition.id(), new Annulation(AUTEUR_MARTIN, quatorzeHeures, MOTIF_ERREUR_DE_SAISIE));

    assertThat(empreintes.calcule(reel, quatorzeHeures)).isEqualTo(empreintes.calcule(previsionnel, treizeHeures));
    assertThat(empreintes.calcule(reel, LE_10_MAI_2026_A_8H.plusSeconds(13 * 3600))).isNotEqualTo(
      empreintes.calcule(previsionnel, treizeHeures)
    );
  }

  @Test
  void shouldLierLEmpreinteAuxFaitsEtALeursIntervalles() {
    var debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(debut);
    var termine = suivi.enregistre(finDe(debut).a(LE_10_MAI_2026_A_17H));
    String avant = empreintes.calcule(suivi, LE_10_MAI_2026_A_17H);
    String apres = empreintes.calcule(termine, LE_10_MAI_2026_A_17H);
    assertThat(avant).matches("[0-9a-f]{64}");
    assertThat(apres).isNotEqualTo(avant);
  }
}
