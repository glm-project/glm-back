package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.Annee;
import org.junit.jupiter.api.Test;

@UnitTest
class InMemoryCompteurDElementsDeFabricationTest {

  private static final Annee ANNEE_2027 = new Annee(2027);

  private final InMemoryCompteurDElementsDeFabrication compteur = new InMemoryCompteurDElementsDeFabrication();

  @Test
  void shouldStartSerieAtOne() {
    assertThat(compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026)).isEqualTo(1);
  }

  @Test
  void shouldIncrementSerie() {
    compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026);

    assertThat(compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026)).isEqualTo(2);
  }

  @Test
  void shouldNotShareSerieBetweenTypes() {
    compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026);
    compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026);

    assertThat(compteur.prochainNumero(PRODUIT, ANNEE_2026)).isEqualTo(1);
  }

  @Test
  void shouldRestartSerieOnNewAnnee() {
    compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026);
    compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2026);

    assertThat(compteur.prochainNumero(ORDRE_DE_FABRICATION, ANNEE_2027)).isEqualTo(1);
  }
}
