package com.glm.glmback.categoriedeproduit.domain;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class CategoriesDeProduitServiceTest {

  private CategoriesDeProduitEnMemoire repository;
  private ElementsRangesEnMemoire elements;
  private CategoriesDeProduitService categories;

  @BeforeEach
  void setUp() {
    repository = new CategoriesDeProduitEnMemoire();
    elements = new ElementsRangesEnMemoire();
    categories = new CategoriesDeProduitService(repository, elements);
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

  @Test
  void shouldDeleteUnusedCategorie() {
    repository.create(categorieDeProduitMoule());

    categories.delete(CODE_MOULE);

    assertThat(repository.get(CODE_MOULE)).isEmpty();
  }

  @Test
  void shouldNotDeleteUnknownCategorie() {
    assertThatThrownBy(() -> categories.delete(CODE_MOULE))
      .isExactlyInstanceOf(CategorieIntrouvableException.class)
      .hasMessageContaining("MOULE");
  }

  @Test
  void shouldNotDeleteCategorieHoldingProduits() {
    repository.create(categorieDeProduitMoule());
    elements.range(CODE_MOULE);

    assertThatThrownBy(() -> categories.delete(CODE_MOULE))
      .isExactlyInstanceOf(CategorieUtiliseeException.class)
      .hasMessageContaining("MOULE");
    assertThat(repository.get(CODE_MOULE)).contains(categorieDeProduitMoule());
  }

  @Test
  void shouldCreateAgainDeletedCategorie() {
    categories.create(CODE_MOULE);
    categories.delete(CODE_MOULE);

    assertThat(categories.create(CODE_MOULE)).isEqualTo(categorieDeProduitMoule());
  }

  @Test
  void shouldReorderCategories() {
    repository.create(categorieDeProduitMoule());
    repository.create(categorieDeProduitOf());

    categories.reordonne(List.of(CODE_OF, CODE_MOULE));

    assertThat(categories.list(firstPageOfTen()).content()).containsExactly(
      categorieDeProduitOfEnTete(),
      categorieDeProduitMouleEnSecond()
    );
  }

  @Test
  void shouldNotReorderWithMissingCategorie() {
    repository.create(categorieDeProduitMoule());
    repository.create(categorieDeProduitOf());

    assertThatThrownBy(() -> categories.reordonne(List.of(CODE_OF))).isExactlyInstanceOf(OrdreIncompletException.class);
  }

  @Test
  void shouldNotReorderWithDuplicatedCategorie() {
    repository.create(categorieDeProduitMoule());
    repository.create(categorieDeProduitOf());

    assertThatThrownBy(() -> categories.reordonne(List.of(CODE_OF, CODE_OF))).isExactlyInstanceOf(OrdreIncompletException.class);
  }

  @Test
  void shouldNotReorderWithUnknownCategorie() {
    repository.create(categorieDeProduitMoule());

    assertThatThrownBy(() -> categories.reordonne(List.of(CODE_OF))).isExactlyInstanceOf(OrdreIncompletException.class);
    assertThat(repository.get(CODE_MOULE)).contains(categorieDeProduitMoule());
  }

  @Test
  void shouldNotReorderWithMoreCategoriesThanDeclared() {
    repository.create(categorieDeProduitMoule());

    assertThatThrownBy(() -> categories.reordonne(List.of(CODE_MOULE, CODE_OF))).isExactlyInstanceOf(OrdreIncompletException.class);
  }
}
