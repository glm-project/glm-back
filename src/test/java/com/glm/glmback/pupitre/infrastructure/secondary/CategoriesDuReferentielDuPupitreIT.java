package com.glm.glmback.pupitre.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.pupitre.domain.CategorieDElement;
import com.glm.glmback.pupitre.domain.CategoriesDuPupitre;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class CategoriesDuReferentielDuPupitreIT {

  @Autowired
  private CategoriesDuPupitre categories;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  /**
   * Le schema est partage avec les autres tests des categories : seules les dernieres positions sont celles de ce test.
   */
  @Test
  @WithTenant("categories_fixture")
  void shouldRangerLesCategoriesParRangPuisParCode() {
    int dernierRang = inTransaction(() ->
      ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(rang), 0) FROM categorie_de_produit").getSingleResult()).intValue()
    );
    declare("PUPITREY", dernierRang + 2);
    declare("PUPITREZ", dernierRang + 1);
    declare("PUPITREX", dernierRang + 2);

    assertThat(inTransaction(categories::toutes)).endsWith(
      new CategorieDElement("PUPITREZ"),
      new CategorieDElement("PUPITREX"),
      new CategorieDElement("PUPITREY")
    );
  }

  @Test
  @WithTenant("categories_vierges")
  void shouldRendreAucuneCategorieAUneEntrepriseQuiNEnADeclareAucune() {
    assertThat(inTransaction(categories::toutes)).isEmpty();
  }

  private void declare(String code, int rang) {
    inTransaction(() ->
      entityManager
        .createNativeQuery("INSERT INTO categorie_de_produit (code, rang) VALUES (?, ?)")
        .setParameter(1, code)
        .setParameter(2, rang)
        .executeUpdate()
    );
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
