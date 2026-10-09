package com.glm.glmback.naturedetravail.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NaturesEnUsage;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class PostesDesNaturesIT {

  private static final String NATURES_FIXTURE = "natures_fixture";

  @Autowired
  private NaturesEnUsage usages;

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
  void shouldSeeNatureCarriedByAPoste() {
    NatureDeTravailId nature = natureDeclaree();
    posteDeLaNature(nature);

    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isTrue();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotSeeNatureWithoutPoste() {
    NatureDeTravailId nature = natureDeclaree();

    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isFalse();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeeUsedNaturesAmongGivenOnes() {
    NatureDeTravailId portee = natureDeclaree();
    NatureDeTravailId libre = natureDeclaree();
    posteDeLaNature(portee);
    posteDeLaNature(portee);

    assertThat(inTransaction(() -> usages.utiliseesParmi(List.of(portee, libre)))).containsExactly(portee);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeeNoUsedNatureAmongNone() {
    assertThat(inTransaction(() -> usages.utiliseesParmi(List.of()))).isEmpty();
  }

  @Test
  void shouldOnlySeePostesOfCurrentTenant() {
    TenantSecurityContexts.authenticateOn(NATURES_FIXTURE);
    NatureDeTravailId nature = natureDeclaree();
    posteDeLaNature(nature);

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isFalse();
  }

  private NatureDeTravailId natureDeclaree() {
    UUID id = UUID.randomUUID();
    inTransaction(() ->
      entities
        .createNativeQuery("insert into nature_de_travail (id, libelle, cle) values (:id, :libelle, :libelle)")
        .setParameter("id", id)
        .setParameter("libelle", "usage " + id)
        .executeUpdate()
    );

    return new NatureDeTravailId(id);
  }

  private void posteDeLaNature(NatureDeTravailId nature) {
    UUID id = UUID.randomUUID();
    inTransaction(() ->
      entities
        .createNativeQuery("insert into poste_de_travail (id, libelle, nature_id) values (:id, :libelle, :nature)")
        .setParameter("id", id)
        .setParameter("libelle", "Poste " + id)
        .setParameter("nature", nature.uuid())
        .executeUpdate()
    );
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
