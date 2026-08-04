package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.Annee;
import org.junit.jupiter.api.Test;

@UnitTest
class InMemoryCompteurDElementsDeFabricationTest {

  private static final Annee ANNEE_2027 = new Annee(2027);

  private final InMemoryCompteurDElementsDeFabrication compteur = new InMemoryCompteurDElementsDeFabrication();

  @Test
  void shouldStartOrdreDeFabricationSerieAtOne() {
    assertThat(compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026)).isEqualTo(1);
  }

  @Test
  void shouldIncrementOrdreDeFabricationSerie() {
    compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026);

    assertThat(compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026)).isEqualTo(2);
  }

  @Test
  void shouldStartProduitSerieAtOne() {
    assertThat(compteur.prochainNumeroDeProduit(ANNEE_2026)).isEqualTo(1);
  }

  @Test
  void shouldNotShareSerieBetweenOrdresDeFabricationAndProduits() {
    compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026);
    compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026);

    assertThat(compteur.prochainNumeroDeProduit(ANNEE_2026)).isEqualTo(1);
  }

  @Test
  void shouldRestartSerieOnNewAnnee() {
    compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026);
    compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2026);

    assertThat(compteur.prochainNumeroDOrdreDeFabrication(ANNEE_2027)).isEqualTo(1);
  }
}
