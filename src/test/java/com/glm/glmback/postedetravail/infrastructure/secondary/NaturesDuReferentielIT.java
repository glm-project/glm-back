package com.glm.glmback.postedetravail.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import com.glm.glmback.postedetravail.domain.NaturesDeclarees;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class NaturesDuReferentielIT {

  private static final String NATURES_FIXTURE = "natures_fixture";

  @Autowired
  private NaturesDeclarees natures;

  @Autowired
  private EntityManager entities;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldGetDeclaredNatureById() {
    NatureDuPoste nature = NaturesEnBase.nature(entities, transactions, "ebavurage " + UUID.randomUUID());

    assertThat(inTransaction(() -> natures.get(nature.id()))).contains(nature);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotGetUnknownNature() {
    assertThat(inTransaction(() -> natures.get(NatureDeTravailId.newId()))).isEmpty();
  }

  @Test
  void shouldOnlyGetNaturesOfCurrentTenant() {
    TenantSecurityContexts.authenticateOn(NATURES_FIXTURE);
    NatureDuPoste nature = NaturesEnBase.nature(entities, transactions, "voisine " + UUID.randomUUID());

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(inTransaction(() -> natures.get(nature.id()))).isEmpty();
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
