package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduitRepository;
import com.glm.glmback.categoriedeproduit.domain.CategorieDejaExistanteException;
import com.glm.glmback.categoriedeproduit.domain.CategorieIntrouvableException;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.categoriedeproduit.domain.Rang;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaCategorieDeProduitRepositoryIT {

  private static final String CATEGORIES_FIXTURE = "categories_fixture";
  private static final String KATILYS = "katilys";
  private static final AtomicInteger COMPTEUR = new AtomicInteger();

  @Autowired
  private CategorieDeProduitRepository categories;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldCreateAndGetCategorie() {
    CategorieDeProduit categorie = categorieApresLaDerniere();

    inTransaction(() -> categories.create(categorie));

    assertThat(inTransaction(() -> categories.get(categorie.code()))).contains(categorie);
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldNotCreateCategorieTwice() {
    CategorieDeProduit categorie = categorieApresLaDerniere();
    inTransaction(() -> categories.create(categorie));

    assertThatThrownBy(() -> inTransaction(() -> categories.create(categorie))).isExactlyInstanceOf(CategorieDejaExistanteException.class);
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldNotGetUnknownCategorie() {
    assertThat(inTransaction(() -> categories.get(codeDeTest()))).isEmpty();
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldUpdateCategorie() {
    CategorieDeProduit categorie = categorieApresLaDerniere();
    inTransaction(() -> categories.create(categorie));
    CategorieDeProduit deplacee = categorie.deplace(categorie.rang().suivant());

    inTransaction(() -> categories.update(deplacee));

    assertThat(inTransaction(() -> categories.get(categorie.code()))).contains(deplacee);
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldNotUpdateUnknownCategorie() {
    CategorieDeProduit inconnue = new CategorieDeProduit(codeDeTest(), Rang.premier());

    assertThatThrownBy(() -> inTransaction(() -> categories.update(inconnue))).isExactlyInstanceOf(CategorieIntrouvableException.class);
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldCountCategories() {
    long avant = inTransaction(() -> categories.compte());
    inTransaction(() -> categories.create(categorieApresLaDerniere()));

    assertThat(inTransaction(() -> categories.compte())).isEqualTo(avant + 1);
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldDeleteCategorie() {
    CategorieDeProduit categorie = categorieApresLaDerniere();
    inTransaction(() -> categories.create(categorie));

    inTransaction(() -> {
      categories.delete(categorie.code());
      return null;
    });

    assertThat(inTransaction(() -> categories.get(categorie.code()))).isEmpty();
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldNotDeleteUnknownCategorie() {
    CodeDeCategorie inconnu = codeDeTest();

    assertThatThrownBy(() ->
      inTransaction(() -> {
        categories.delete(inconnu);
        return null;
      })
    ).isExactlyInstanceOf(CategorieIntrouvableException.class);
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldReadLastRang() {
    CategorieDeProduit categorie = categorieApresLaDerniere();
    inTransaction(() -> categories.create(categorie));

    assertThat(inTransaction(() -> categories.dernierRang())).contains(categorie.rang());
  }

  @Test
  @WithTenant("categories_vierges")
  void shouldHaveNoLastRangWithoutCategorie() {
    assertThat(inTransaction(() -> categories.dernierRang())).isEmpty();
  }

  @Test
  @WithTenant(CATEGORIES_FIXTURE)
  void shouldListCategoriesByRangThenCode() {
    Rang rang = rangApresLeDernier();
    CategorieDeProduit derniere = new CategorieDeProduit(codeDeTest(), rang.suivant());
    CategorieDeProduit exAequoB = new CategorieDeProduit(new CodeDeCategorie("ZB" + lettres()), rang);
    CategorieDeProduit exAequoA = new CategorieDeProduit(new CodeDeCategorie("ZA" + lettres()), rang);
    inTransaction(() -> categories.create(derniere));
    inTransaction(() -> categories.create(exAequoB));
    inTransaction(() -> categories.create(exAequoA));

    assertThat(inTransaction(() -> categories.list(new Pageable(0, 100))).content()).endsWith(exAequoA, exAequoB, derniere);
  }

  @Test
  void shouldIsolateCategoriesByTenant() {
    TenantSecurityContexts.authenticateOn(CATEGORIES_FIXTURE);
    CategorieDeProduit categorie = categorieApresLaDerniere();
    inTransaction(() -> categories.create(categorie));

    TenantSecurityContexts.authenticateOn(KATILYS);

    assertThat(inTransaction(() -> categories.get(categorie.code()))).isEmpty();
  }

  private CategorieDeProduit categorieApresLaDerniere() {
    return new CategorieDeProduit(codeDeTest(), rangApresLeDernier());
  }

  private Rang rangApresLeDernier() {
    return inTransaction(() -> categories.dernierRang())
      .map(Rang::suivant)
      .orElseGet(Rang::premier);
  }

  private static CodeDeCategorie codeDeTest() {
    return new CodeDeCategorie("IT" + lettres());
  }

  /**
   * Le code n'admet que des lettres : le compteur s'ecrit en base 26, de A a Z.
   */
  private static String lettres() {
    StringBuilder lettres = new StringBuilder();
    int reste = COMPTEUR.incrementAndGet();
    while (reste > 0) {
      lettres.insert(0, (char) ('A' + (reste % 26)));
      reste /= 26;
    }

    return lettres.toString();
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
