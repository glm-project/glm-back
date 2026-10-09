package com.glm.glmback.parametrage.infrastructure.secondary;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.parametrage.domain.ParametrageEnMemoire;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class DureeMaximaleDActiviteParametreeTest {

  private final ParametrageEnMemoire parametrage = new ParametrageEnMemoire();
  private final DureeMaximaleDActiviteParametree durees = new DureeMaximaleDActiviteParametree(parametrage);

  /**
   * Tant que le gestionnaire n'a rien fixe, le parametrage rend sa valeur par defaut : c'est lui, et lui seul, qui la
   * connait.
   */
  @Test
  void shouldDonnerLaDureeParDefautDuParametrageTantQueRienNEstFixe() {
    assertThat(durees.current()).isEqualTo(new MaximumActivityDuration(Duration.ofHours(13)));
  }

  @Test
  void shouldDonnerLaDureeQueLeGestionnaireAFixee() {
    parametrage.update(parametrageDixHeures());

    assertThat(durees.current()).isEqualTo(new MaximumActivityDuration(Duration.ofHours(10)));
  }

  /**
   * La duree est relue a chaque appel : elle ne se fige pas dans l'adaptateur.
   */
  @Test
  void shouldSuivreUnChangementDeLaDuree() {
    durees.current();
    parametrage.update(parametrageDixHeures());

    assertThat(durees.current().value()).isEqualTo(Duration.ofHours(10));
  }
}
