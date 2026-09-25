package com.glm.glmback.coutderevient.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.coutderevient.domain.AmplitudeMaximale;
import com.glm.glmback.coutderevient.domain.SeuilDuCout;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Le cout de revient lit le seuil dans la table du parametrage, sans importer ce contexte. Chaque test annule sa transaction.
 */
@IntegrationTest
class SeuilDuCoutDuParametrageIT {

  @Autowired
  private SeuilDuCout seuil;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant("katilys")
  void shouldLireLeSeuilSeme() {
    AmplitudeMaximale lue = transactions.execute(status -> {
      status.setRollbackOnly();
      return seuil.amplitudeMaximale();
    });

    assertThat(lue).isEqualTo(new AmplitudeMaximale(Duration.ofHours(13)));
  }

  @Test
  @WithTenant("impeccmold")
  void shouldLireLeSeuilModifie() {
    AmplitudeMaximale lue = transactions.execute(status -> {
      status.setRollbackOnly();
      entityManager.createNativeQuery("update parametrage set amplitude_maximale_minutes = 600").executeUpdate();
      entityManager.clear();
      return seuil.amplitudeMaximale();
    });

    assertThat(lue).isEqualTo(new AmplitudeMaximale(Duration.ofHours(10)));
  }
}
