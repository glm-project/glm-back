package com.glm.glmback.categoriedeproduit.domain;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class CategoriesDeProduitServiceTest {

  private CategoriesDeProduitEnMemoire repository;
  private CategoriesDeProduitService categories;

  @BeforeEach
  void setUp() {
    repository = new CategoriesDeProduitEnMemoire();
    categories = new CategoriesDeProduitService(repository);
  }

  @Test
  void shouldCreateFirstCategorieAtFirstRang() {
    CategorieDeProduit creee = categories.create(CODE_MOULE);

    assertThat(creee).isEqualTo(categorieDeProduitMoule());
    assertThat(repository.get(CODE_MOULE)).contains(creee);
  }

  @Test
  void shouldCreateNextCategorieAfterLastOne() {
    categories.create(CODE_MOULE);

    assertThat(categories.create(CODE_OF)).isEqualTo(categorieDeProduitOf());
  }

  @Test
  void shouldNotCreateAlreadyExistingCategorie() {
    categories.create(CODE_MOULE);

    assertThatThrownBy(() -> categories.create(CODE_MOULE))
      .isExactlyInstanceOf(CategorieDejaExistanteException.class)
      .hasMessageContaining("MOULE");
  }

  @Test
  void shouldListCategoriesByRang() {
    repository.create(categorieDeProduitOf());
    repository.create(categorieDeProduitMoule());

    assertThat(categories.list(firstPageOfTen()).content()).containsExactly(categorieDeProduitMoule(), categorieDeProduitOf());
  }
}
