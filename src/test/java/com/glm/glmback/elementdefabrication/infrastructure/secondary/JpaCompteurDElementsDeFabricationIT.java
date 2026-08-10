package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.elementdefabrication.domain.Annee;
import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaCompteurDElementsDeFabricationIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final Annee ANNEE_2098 = new Annee(2098);
  private static final Annee ANNEE_2099 = new Annee(2099);

  @Autowired
  private CompteurDElementsDeFabrication compteur;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldIncrementNumeroOnEachCall() {
    long premier = prochainNumero(TypeDElementDeFabrication.PRODUIT, ANNEE_2098);

    assertThat(prochainNumero(TypeDElementDeFabrication.PRODUIT, ANNEE_2098)).isEqualTo(premier + 1);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCountEachTypeSeparately() {
    prochainNumero(TypeDElementDeFabrication.PRODUIT, ANNEE_2098);

    assertThat(prochainNumero(TypeDElementDeFabrication.ORDRE_DE_FABRICATION, ANNEE_2098)).isEqualTo(1);
  }

  @Test
  void shouldCountSeparatelyInEachTenant() {
    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    long chezImpeccMold = prochainNumero(TypeDElementDeFabrication.PRODUIT, ANNEE_2099);

    TenantSecurityContexts.authenticateOn(KATILYS);
    long chezKatilys = prochainNumero(TypeDElementDeFabrication.PRODUIT, ANNEE_2099);

    assertThat(chezImpeccMold).isEqualTo(1);
    assertThat(chezKatilys).isEqualTo(1);
  }

  private long prochainNumero(TypeDElementDeFabrication type, Annee annee) {
    return inTransaction(() -> compteur.prochainNumero(type, annee));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
