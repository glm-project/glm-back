package com.glm.glmback.parametrage.infrastructure.secondary;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.parametrage.domain.DureeMaxDActivite;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaParametrageRepositoryIT {

  @Autowired
  private ParametrageRepository parametrages;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant("parametrage_vierge")
  void shouldReadDefaultDureeMaxDActiviteOfANewTenant() {
    Parametrage parametrage = transactions.execute(status -> parametrages.get());

    assertThat(parametrage).isEqualTo(new Parametrage(DureeMaxDActivite.parDefaut()));
  }

  @Test
  @WithTenant("parametrage_fixture")
  void shouldUpdateParametrage() {
    transactions.execute(status -> parametrages.update(parametrageDixHeures()));

    Parametrage relu = transactions.execute(status -> parametrages.get());
    assertThat(relu).isEqualTo(parametrageDixHeures());
  }
}
