package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationCriteria;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationDejaExistantException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.Nom;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.Prefixe;
import com.glm.glmback.elementdefabrication.domain.Reference;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaElementDeFabricationRepositoryIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final Prefixe PREFIXE_IT = new Prefixe("IT");
  private static final AtomicLong COMPTEUR = new AtomicLong();

  @Autowired
  private ElementDeFabricationRepository elements;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateAndGetElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationCreeLe(LE_15_JANVIER_2026);

    inTransaction(() -> elements.create(element));

    assertThat(inTransaction(() -> elements.get(element.id()))).contains(element);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotCreateAlreadyExistingElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationCreeLe(LE_15_JANVIER_2026);
    inTransaction(() -> elements.create(element));

    assertThatThrownBy(() -> inTransaction(() -> elements.create(element))).isExactlyInstanceOf(
      ElementDeFabricationDejaExistantException.class
    );
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = ElementDeFabricationId.newId();

    assertThat(inTransaction(() -> elements.get(inconnu))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldUpdateExistingElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationCreeLe(LE_15_JANVIER_2026);
    inTransaction(() -> elements.create(element));

    ElementDeFabrication revise = element.revise(
      Optional.of(referenceDeTest(COMPTEUR.incrementAndGet())),
      Optional.of(descriptionCarterEnFonte()),
      LE_20_FEVRIER_2026
    );
    inTransaction(() -> elements.update(revise));

    assertThat(inTransaction(() -> elements.get(element.id()))).contains(revise);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotUpdateUnknownElementDeFabrication() {
    ElementDeFabrication inconnu = elementDeFabricationCreeLe(LE_15_JANVIER_2026);

    assertThatThrownBy(() -> inTransaction(() -> elements.update(inconnu))).isExactlyInstanceOf(
      ElementDeFabricationIntrouvableException.class
    );
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldDeleteExistingElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationCreeLe(LE_15_JANVIER_2026);
    inTransaction(() -> elements.create(element));

    inTransaction(() -> {
      elements.delete(element.id());

      return null;
    });

    assertThat(inTransaction(() -> elements.get(element.id()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotDeleteUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = ElementDeFabricationId.newId();

    assertThatThrownBy(() ->
      inTransaction(() -> {
        elements.delete(inconnu);

        return null;
      })
    ).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListElementsDeFabricationOfPeriodeFromNewestToOldest() {
    Instant lundi = Instant.parse("2031-03-02T08:00:00Z");
    Instant mardi = Instant.parse("2031-03-03T08:00:00Z");
    ElementDeFabrication ancien = elementDeFabricationCreeLe(lundi);
    ElementDeFabrication recent = elementDeFabricationCreeLe(mardi);
    inTransaction(() -> elements.create(ancien));
    inTransaction(() -> elements.create(recent));

    Page<ElementDeFabrication> page = inTransaction(() -> elements.list(criteres(lundi, mardi), firstPageOfTen()));

    assertThat(page.content()).containsExactly(recent, ancien);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotListElementsDeFabricationOutOfPeriode() {
    Instant creation = Instant.parse("2032-04-05T08:00:00Z");
    inTransaction(() -> elements.create(elementDeFabricationCreeLe(creation)));

    Page<ElementDeFabrication> page = inTransaction(() ->
      elements.list(criteres(creation.plusSeconds(1), creation.plusSeconds(2)), firstPageOfTen())
    );

    assertThat(page.content()).isEmpty();
  }

  @Test
  void shouldNotReadElementDeFabricationOfAnotherTenant() {
    Instant creation = Instant.parse("2033-05-06T08:00:00Z");
    ElementDeFabrication element = elementDeFabricationCreeLe(creation);

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    inTransaction(() -> elements.create(element));

    TenantSecurityContexts.authenticateOn(KATILYS);
    Optional<ElementDeFabrication> chezKatilys = inTransaction(() -> elements.get(element.id()));
    Page<ElementDeFabrication> pageChezKatilys = inTransaction(() -> elements.list(criteres(creation, creation), firstPageOfTen()));

    assertThat(chezKatilys).isEmpty();
    assertThat(pageChezKatilys.content()).isEmpty();
  }

  @Test
  void shouldReuseSameIdentityInEachTenant() {
    Instant creation = Instant.parse("2034-06-07T08:00:00Z");
    ElementDeFabricationId partage = ElementDeFabricationId.newId();
    ElementDeFabrication chezImpeccMold = elementDeFabrication(partage, creation);
    ElementDeFabrication chezKatilys = elementDeFabrication(partage, creation);

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    inTransaction(() -> elements.create(chezImpeccMold));

    TenantSecurityContexts.authenticateOn(KATILYS);
    inTransaction(() -> elements.create(chezKatilys));

    assertThat(inTransaction(() -> elements.get(partage))).contains(chezKatilys);
    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    assertThat(inTransaction(() -> elements.get(partage))).contains(chezImpeccMold);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldGetIdOfElementDeFabricationHoldingReference() {
    ElementDeFabrication element = elementDeFabricationCreeLe(LE_15_JANVIER_2026);
    inTransaction(() -> elements.create(element));

    assertThat(inTransaction(() -> elements.idPourReference(element.reference().orElseThrow()))).contains(element.id());
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetIdOfUnusedReference() {
    assertThat(inTransaction(() -> elements.idPourReference(new Reference("IT-inconnue")))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateManyElementsDeFabricationWithoutReference() {
    ElementDeFabrication premier = elementDeFabricationSansReference(LE_15_JANVIER_2026);
    ElementDeFabrication second = elementDeFabricationSansReference(LE_15_JANVIER_2026);

    inTransaction(() -> elements.create(premier));
    inTransaction(() -> elements.create(second));

    assertThat(inTransaction(() -> elements.get(premier.id()))).contains(premier);
    assertThat(inTransaction(() -> elements.get(second.id()))).contains(second);
  }

  @Test
  void shouldReuseSameReferenceInEachTenant() {
    ElementDeFabrication chezImpeccMold = elementDeFabricationCreeLe(LE_15_JANVIER_2026);
    ElementDeFabrication chezKatilys = ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(Nom.of(PREFIXE_IT, ANNEE_2026, COMPTEUR.incrementAndGet()))
      .reference(chezImpeccMold.reference().orElseThrow().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    inTransaction(() -> elements.create(chezImpeccMold));

    TenantSecurityContexts.authenticateOn(KATILYS);
    inTransaction(() -> elements.create(chezKatilys));

    assertThat(inTransaction(() -> elements.get(chezKatilys.id()))).contains(chezKatilys);
  }

  private static ElementDeFabricationCriteria criteres(Instant debut, Instant fin) {
    return new ElementDeFabricationCriteria(new Periode(debut, fin));
  }

  private static ElementDeFabrication elementDeFabricationCreeLe(Instant dateDeCreation) {
    return elementDeFabrication(ElementDeFabricationId.newId(), dateDeCreation);
  }

  private static ElementDeFabrication elementDeFabrication(ElementDeFabricationId id, Instant dateDeCreation) {
    long numero = COMPTEUR.incrementAndGet();

    return ElementDeFabrication.builder()
      .id(id)
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(Nom.of(PREFIXE_IT, ANNEE_2026, numero))
      .reference(referenceDeTest(numero).value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(dateDeCreation)
      .dateDeModification(dateDeCreation);
  }

  private static ElementDeFabrication elementDeFabricationSansReference(Instant dateDeCreation) {
    return ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(Nom.of(PREFIXE_IT, ANNEE_2026, COMPTEUR.incrementAndGet()))
      .reference(null)
      .description(null)
      .dateDeCreation(dateDeCreation)
      .dateDeModification(dateDeCreation);
  }

  private static Reference referenceDeTest(long numero) {
    return new Reference("IT-%06d".formatted(numero));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
