package com.glm.glmback.syntheseheures.infrastructure.primary;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.syntheseheures.domain.MoisCalendaire;
import com.glm.glmback.syntheseheures.domain.Nom;
import com.glm.glmback.syntheseheures.domain.OperateurConnu;
import com.glm.glmback.syntheseheures.domain.Prenom;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import org.junit.jupiter.api.Test;

@UnitTest
class LibellesDuRapportDActiviteTest {

  @Test
  void shouldNommerUneSemaineDansUnMois() {
    assertThat(LibellesDuRapportDActivite.periode(new SemaineCalendaire(2026, 38))).isEqualTo("Semaine 38 · du 14 au 20 septembre 2026");
  }

  @Test
  void shouldNommerUneSemaineAChevalSurDeuxMois() {
    assertThat(LibellesDuRapportDActivite.periode(new SemaineCalendaire(2026, 40))).isEqualTo(
      "Semaine 40 · du 28 septembre au 4 octobre 2026"
    );
  }

  @Test
  void shouldNommerUneSemaineAChevalSurDeuxAnnees() {
    assertThat(LibellesDuRapportDActivite.periode(new SemaineCalendaire(2026, 1))).isEqualTo(
      "Semaine 1 · du 29 décembre 2025 au 4 janvier 2026"
    );
  }

  @Test
  void shouldNommerUnMoisAvecSaMajuscule() {
    assertThat(LibellesDuRapportDActivite.periode(new MoisCalendaire(2026, 9))).isEqualTo("Septembre 2026");
  }

  @Test
  void shouldNommerLeFichierSansAccentNiEspace() {
    OperateurConnu operateur = new OperateurConnu(OPERATEUR_ID_DUPONT, new Nom("Lefèvre Le Gall"), new Prenom("Zoé"));

    assertThat(LibellesDuRapportDActivite.nomDeFichier(operateur, new SemaineCalendaire(2026, 1))).isEqualTo(
      "activite-lefevre-le-gall-zoe-S01-2026"
    );
  }
}
