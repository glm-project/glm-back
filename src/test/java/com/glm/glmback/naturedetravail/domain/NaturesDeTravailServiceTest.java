package com.glm.glmback.naturedetravail.domain;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class NaturesDeTravailServiceTest {

  private NaturesDeTravailEnMemoire repository;
  private NaturesUtiliseesEnMemoire usages;
  private NaturesDeTravailService natures;

  @BeforeEach
  void setUp() {
    repository = new NaturesDeTravailEnMemoire();
    usages = new NaturesUtiliseesEnMemoire();
    natures = new NaturesDeTravailService(repository, usages);
  }

  @Test
  void shouldDeclareNature() {
    NatureDeTravail declaree = natures.declare(LIBELLE_SOUDAGE);

    assertThat(declaree.libelle()).isEqualTo(LIBELLE_SOUDAGE);
    assertThat(repository.get(declaree.id())).contains(declaree);
  }

  @ParameterizedTest
  @ValueSource(strings = { "Soudage", "SOUDAGE", "Soudâge", " soudage " })
  void shouldNotDeclareNatureWithSameKey(String libelle) {
    natures.declare(LIBELLE_SOUDAGE);

    assertThatThrownBy(() -> natures.declare(new LibelleDeNature(libelle)))
      .isExactlyInstanceOf(NatureDejaExistanteException.class)
      .hasMessageContaining(libelle.strip());
  }

  @Test
  void shouldListNaturesByKey() {
    repository.create(natureDeTravailTournage());
    repository.create(natureDeTravailSoudage());

    assertThat(natures.list(firstPageOfTen()).content())
      .extracting(NatureDeTravailListee::nature)
      .containsExactly(natureDeTravailSoudage(), natureDeTravailTournage());
  }

  @Test
  void shouldListAccentedNatureInAlphabeticalOrder() {
    NatureDeTravail electroErosion = natures.declare(new LibelleDeNature("Électro-érosion"));
    NatureDeTravail dessin = natures.declare(new LibelleDeNature("dessin"));
    NatureDeTravail fraisage = natures.declare(new LibelleDeNature("Fraisage"));

    assertThat(natures.list(firstPageOfTen()).content())
      .extracting(NatureDeTravailListee::nature)
      .containsExactly(dessin, electroErosion, fraisage);
  }

  @Test
  void shouldTellWhichListedNaturesAreUsed() {
    repository.create(natureDeTravailSoudage());
    repository.create(natureDeTravailTournage());
    usages.utilise(NATURE_DE_TRAVAIL_ID_SOUDAGE);

    assertThat(natures.list(firstPageOfTen()).content()).containsExactly(
      new NatureDeTravailListee(natureDeTravailSoudage(), true),
      new NatureDeTravailListee(natureDeTravailTournage(), false)
    );
  }

  @Test
  void shouldKeepPaginationOfListedNatures() {
    repository.create(natureDeTravailSoudage());
    repository.create(natureDeTravailTournage());

    Page<NatureDeTravailListee> page = natures.list(new Pageable(1, 1));

    assertThat(page.content()).containsExactly(new NatureDeTravailListee(natureDeTravailTournage(), false));
    assertThat(page.currentPage()).isEqualTo(1);
    assertThat(page.pageSize()).isEqualTo(1);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }
}
