package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.categoriedeproduit.domain.CategoriesUtilisees;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ElementsDesCategoriesIT {

  private static final String CATEGORIES_FIXTURE = "categories_fixture";

  @Autowired
  private CategoriesUtilisees usages;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldSeeCategorieHoldingAnElement() {
    rangeUnElementDans("USAGEA");

    assertThat(inTransaction(() -> usages.estUtilisee(new CodeDeCategorie("USAGEA")))).isTrue();
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldNotSeeCategorieWithoutElement() {
    assertThat(inTransaction(() -> usages.estUtilisee(new CodeDeCategorie("USAGEB")))).isFalse();
  }

  @Test
  void shouldOnlySeeElementsOfCurrentTenant() {
    TenantSecurityContexts.authenticateOn(CATEGORIES_FIXTURE);
    rangeUnElementDans("USAGEC");

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(inTransaction(() -> usages.estUtilisee(new CodeDeCategorie("USAGEC")))).isFalse();
  }

  private void rangeUnElementDans(String categorie) {
    Instant maintenant = Instant.now();
    inTransaction(() ->
      entityManager
        .createNativeQuery(
          "INSERT INTO element_de_fabrication (id, type, nom, date_de_creation, date_de_modification) VALUES (?, ?, ?, ?, ?)"
        )
        .setParameter(1, UUID.randomUUID())
        .setParameter(2, categorie)
        .setParameter(3, categorie + "-2026-000001")
        .setParameter(4, maintenant)
        .setParameter(5, maintenant)
        .executeUpdate()
    );
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
