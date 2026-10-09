package com.glm.glmback.postedetravail.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import com.glm.glmback.postedetravail.domain.NaturesDeclarees;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@SuppressWarnings("removal")
class NaturesDuReferentielIT {

  private static final String NATURES_FIXTURE = "natures_fixture";

  @Autowired
  private NaturesDeclarees natures;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldFindDeclaredNatureIgnoringCaseAccentsAndSpaces() {
    String suffixe = UUID.randomUUID().toString().substring(0, 8);
    NatureDuPoste nature = new NatureDuPoste(NatureDeTravailId.newId(), new NatureDeTravail("Électro-érosion " + suffixe));

    inTransaction(() -> {
      natures.declare(nature);
      return null;
    });

    assertThat(inTransaction(() -> natures.parLibelle(new NatureDeTravail("  ELECTRO-EROSION   " + suffixe)))).contains(nature);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotFindUnknownNature() {
    assertThat(inTransaction(() -> natures.parLibelle(new NatureDeTravail("Inconnue " + UUID.randomUUID())))).isEmpty();
  }

  @Test
  void shouldOnlyFindNaturesOfCurrentTenant() {
    TenantSecurityContexts.authenticateOn(NATURES_FIXTURE);
    NatureDuPoste nature = new NatureDuPoste(NatureDeTravailId.newId(), new NatureDeTravail("Voisine " + UUID.randomUUID()));
    inTransaction(() -> {
      natures.declare(nature);
      return null;
    });

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(inTransaction(() -> natures.parLibelle(nature.libelle()))).isEmpty();
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
