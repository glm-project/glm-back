package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationDejaExistantException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class InMemoryElementDeFabricationRepositoryTest {

  private final InMemoryElementDeFabricationRepository repository = new InMemoryElementDeFabricationRepository();

  @Test
  void shouldCreateElementDeFabrication() {
    ElementDeFabrication ordre = elementDeFabricationAssemblageCarter();

    ElementDeFabrication cree = repository.create(ordre);

    assertThat(cree).isEqualTo(ordre);
    assertThat(repository.get(ordre.id())).contains(ordre);
  }

  @Test
  void shouldNotCreateTwiceSameElementDeFabrication() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    ElementDeFabrication ordre = elementDeFabricationAssemblageCarter(id);
    repository.create(ordre);

    assertThatThrownBy(() -> repository.create(ordre))
      .isExactlyInstanceOf(ElementDeFabricationDejaExistantException.class)
      .hasMessageContaining(id.uuid().toString());
  }

  @Test
  void shouldUpdateElementDeFabrication() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    repository.create(elementDeFabricationAssemblageCarter(id));

    ElementDeFabrication modifie = elementDeFabricationAssemblageCarterRevise(id);

    assertThat(repository.update(modifie)).isEqualTo(modifie);
    assertThat(repository.get(id)).contains(modifie);
  }

  @Test
  void shouldNotUpdateUnknownElementDeFabrication() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    ElementDeFabrication inconnu = elementDeFabricationAssemblageCarter(id);

    assertThatThrownBy(() -> repository.update(inconnu))
      .isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class)
      .hasMessageContaining(id.uuid().toString());
  }

  @Test
  void shouldDeleteElementDeFabrication() {
    ElementDeFabrication ordre = elementDeFabricationAssemblageCarter();
    repository.create(ordre);

    repository.delete(ordre.id());

    assertThat(repository.get(ordre.id())).isEmpty();
  }

  @Test
  void shouldNotDeleteUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = ElementDeFabricationId.newId();

    assertThatThrownBy(() -> repository.delete(inconnu))
      .isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class)
      .hasMessageContaining(inconnu.uuid().toString());
  }

  @Test
  void shouldNotGetUnknownElementDeFabrication() {
    assertThat(repository.get(ElementDeFabricationId.newId())).isEmpty();
  }

  @Test
  void shouldListOrdresDeFabricationAndProduitsInPeriode() {
    ElementDeFabrication ordre = elementDeFabricationAssemblageCarter();
    ElementDeFabrication produit = elementDeFabricationCarterMoteur();
    repository.create(ordre);
    repository.create(produit);

    Page<ElementDeFabrication> page = repository.list(criteresPremierTrimestre2026(), firstPageOfTen());

    assertThat(page.content()).containsExactlyInAnyOrder(ordre, produit);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  @Test
  void shouldNotListElementDeFabricationOutOfPeriode() {
    repository.create(elementDeFabricationAssemblageCarterCreeLe(LE_31_MARS_2026.plusSeconds(1)));

    Page<ElementDeFabrication> page = repository.list(criteresPremierTrimestre2026(), firstPageOfTen());

    assertThat(page.content()).isEmpty();
    assertThat(page.totalElementsCount()).isZero();
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  void shouldListElementsDeFabricationMostRecentFirst() {
    ElementDeFabrication ancien = elementDeFabricationAssemblageCarterCreeLe(LE_15_JANVIER_2026);
    ElementDeFabrication recent = elementDeFabricationAssemblageCarterCreeLe(LE_20_FEVRIER_2026);
    repository.create(ancien);
    repository.create(recent);

    Page<ElementDeFabrication> page = repository.list(criteresPremierTrimestre2026(), firstPageOfTen());

    assertThat(page.content()).containsExactly(recent, ancien);
  }

  @Test
  void shouldListSecondPageOfElementsDeFabrication() {
    repository.create(elementDeFabricationAssemblageCarterCreeLe(Instant.parse("2026-01-10T00:00:00Z")));
    repository.create(elementDeFabricationAssemblageCarterCreeLe(Instant.parse("2026-01-20T00:00:00Z")));
    ElementDeFabrication plusAncien = elementDeFabricationAssemblageCarterCreeLe(Instant.parse("2026-01-05T00:00:00Z"));
    repository.create(plusAncien);

    Page<ElementDeFabrication> premierePage = repository.list(criteresPremierTrimestre2026(), new Pageable(0, 2));
    Page<ElementDeFabrication> secondePage = repository.list(criteresPremierTrimestre2026(), new Pageable(1, 2));

    assertThat(premierePage.content()).hasSize(2);
    assertThat(premierePage.totalPagesCount()).isEqualTo(2);
    assertThat(premierePage.hasNext()).isTrue();
    assertThat(secondePage.content()).containsExactly(plusAncien);
    assertThat(secondePage.totalElementsCount()).isEqualTo(3);
    assertThat(secondePage.hasPrevious()).isTrue();
  }
}
