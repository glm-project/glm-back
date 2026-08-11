package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static com.glm.glmback.shared.time.domain.TimeFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementsDeFabricationServiceTest {

  private static final Instant LE_10_MAI_2026 = Instant.parse("2026-05-10T08:00:00Z");
  private static final Instant LE_15_JUIN_2026 = Instant.parse("2026-06-15T09:00:00Z");
  private static final Instant LE_1ER_JUIN_2027 = Instant.parse("2027-06-01T00:00:00Z");
  private static final long NUMERO_FIGE = 42;

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026);
  private final ElementsDeFabricationService elements = ElementsDeFabricationService.builder()
    .repository(new ElementsDeFabricationEnMemoire())
    .compteur(new CompteurFige())
    .prefixes(new PrefixesFiges(PREFIXE_OF, PREFIXE_PRD))
    .clock(maintenant::get);

  @Test
  void shouldCreateOrdreDeFabricationWithGeneratedNom() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateOrdre1015());

    assertThat(cree.type()).isEqualTo(TypeDElementDeFabrication.ORDRE_DE_FABRICATION);
    assertThat(cree.nom().value()).isEqualTo("OF-2026-000042");
    assertThat(cree.reference()).contains(reference1015());
    assertThat(cree.description()).contains(descriptionCarterEnFonte());
    assertThat(cree.dateDeCreation()).isEqualTo(LE_10_MAI_2026);
    assertThat(cree.dateDeModification()).isEqualTo(LE_10_MAI_2026);
    assertThat(elements.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreateProduitWithGeneratedNom() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateProduit2456());

    assertThat(cree.type()).isEqualTo(TypeDElementDeFabrication.PRODUIT);
    assertThat(cree.nom().value()).isEqualTo("PRD-2026-000042");
    assertThat(cree.reference()).contains(reference2456());
    assertThat(elements.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreateProduitWithoutReferenceNorDescription() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateProduitSansReference());

    assertThat(cree.nom().value()).isEqualTo("PRD-2026-000042");
    assertThat(cree.reference()).isEmpty();
    assertThat(cree.description()).isEmpty();
    assertThat(elements.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreateManyElementsDeFabricationWithoutReference() {
    elements.create(elementDeFabricationToCreateProduitSansReference());

    ElementDeFabrication second = elements.create(elementDeFabricationToCreateProduitSansReference());

    assertThat(second.reference()).isEmpty();
  }

  @Test
  void shouldNotCreateElementDeFabricationWithReferenceOfAnotherOne() {
    elements.create(elementDeFabricationToCreateOrdre1015());

    ElementDeFabricationToCreate memeReference = elementDeFabricationToCreateProduit1015();

    assertThatThrownBy(() -> elements.create(memeReference))
      .isExactlyInstanceOf(ReferenceDejaUtiliseeException.class)
      .hasMessageContaining("1015");
  }

  @Test
  void shouldGenerateNomFromPrefixesOfSecondaryAdapter() {
    ElementsDeFabricationService autreNomenclature = ElementsDeFabricationService.builder()
      .repository(new ElementsDeFabricationEnMemoire())
      .compteur(new CompteurFige())
      .prefixes(new PrefixesFiges(new Prefixe("FAB"), new Prefixe("ART")))
      .clock(fixedClock(LE_10_MAI_2026));

    ElementDeFabrication ordre = autreNomenclature.create(elementDeFabricationToCreateOrdre1015());
    ElementDeFabrication produit = autreNomenclature.create(elementDeFabricationToCreateProduit2456());

    assertThat(ordre.nom().value()).isEqualTo("FAB-2026-000042");
    assertThat(produit.nom().value()).isEqualTo("ART-2026-000042");
  }

  @Test
  void shouldGenerateNomFromAnneeOfClock() {
    ElementsDeFabricationService elementsDe2027 = ElementsDeFabricationService.builder()
      .repository(new ElementsDeFabricationEnMemoire())
      .compteur(new CompteurFige())
      .prefixes(new PrefixesFiges(PREFIXE_OF, PREFIXE_PRD))
      .clock(fixedClock(LE_1ER_JUIN_2027));

    ElementDeFabrication cree = elementsDe2027.create(elementDeFabricationToCreateOrdre1015());

    assertThat(cree.nom().value()).isEqualTo("OF-2027-000042");
  }

  @Test
  void shouldNotGetUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = ElementDeFabricationId.newId();

    assertThatThrownBy(() -> elements.get(inconnu)).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  @Test
  void shouldListElementsDeFabricationInPeriode() {
    ElementDeFabrication ordre = elements.create(elementDeFabricationToCreateOrdre1015());
    ElementDeFabrication produit = elements.create(elementDeFabricationToCreateProduit2456());

    Page<ElementDeFabrication> page = elements.list(
      new Periode(LE_10_MAI_2026.minusSeconds(1), LE_10_MAI_2026.plusSeconds(1)),
      firstPageOfTen()
    );

    assertThat(page.content()).containsExactlyInAnyOrder(ordre, produit);
  }

  @Test
  void shouldNotListElementsDeFabricationOutOfPeriode() {
    elements.create(elementDeFabricationToCreateOrdre1015());

    Page<ElementDeFabrication> page = elements.list(premierTrimestre2026(), firstPageOfTen());

    assertThat(page.content()).isEmpty();
  }

  @Test
  void shouldUpdateOrdreDeFabrication() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateOrdre1015());
    maintenant.set(LE_15_JUIN_2026);

    ElementDeFabrication modifie = elements.update(elementDeFabricationToUpdate1017(cree.id()));

    assertThat(modifie.type()).isEqualTo(TypeDElementDeFabrication.ORDRE_DE_FABRICATION);
    assertThat(modifie.id()).isEqualTo(cree.id());
    assertThat(modifie.nom()).isEqualTo(cree.nom());
    assertThat(modifie.reference()).contains(reference1017());
    assertThat(modifie.dateDeCreation()).isEqualTo(LE_10_MAI_2026);
    assertThat(modifie.dateDeModification()).isEqualTo(LE_15_JUIN_2026);
    assertThat(elements.get(cree.id())).isEqualTo(modifie);
  }

  @Test
  void shouldUpdateProduit() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateProduit2456());
    maintenant.set(LE_15_JUIN_2026);

    ElementDeFabrication modifie = elements.update(elementDeFabricationToUpdate1017(cree.id()));

    assertThat(modifie.type()).isEqualTo(TypeDElementDeFabrication.PRODUIT);
    assertThat(modifie.id()).isEqualTo(cree.id());
    assertThat(modifie.nom()).isEqualTo(cree.nom());
    assertThat(modifie.reference()).contains(reference1017());
    assertThat(modifie.dateDeCreation()).isEqualTo(LE_10_MAI_2026);
    assertThat(modifie.dateDeModification()).isEqualTo(LE_15_JUIN_2026);
  }

  @Test
  void shouldUpdateElementDeFabricationKeepingItsOwnReference() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateOrdre1015());
    maintenant.set(LE_15_JUIN_2026);

    ElementDeFabrication modifie = elements.update(elementDeFabricationToUpdate1015(cree.id()));

    assertThat(modifie.reference()).contains(reference1015());
    assertThat(modifie.dateDeModification()).isEqualTo(LE_15_JUIN_2026);
  }

  @Test
  void shouldNotUpdateElementDeFabricationWithReferenceOfAnotherOne() {
    elements.create(elementDeFabricationToCreateOrdre1015());
    ElementDeFabrication produit = elements.create(elementDeFabricationToCreateProduit2456());

    ElementDeFabricationToUpdate memeReference = elementDeFabricationToUpdate1015(produit.id());

    assertThatThrownBy(() -> elements.update(memeReference))
      .isExactlyInstanceOf(ReferenceDejaUtiliseeException.class)
      .hasMessageContaining("1015");
  }

  @Test
  void shouldNotUpdateUnknownElementDeFabrication() {
    ElementDeFabricationToUpdate inconnu = elementDeFabricationToUpdate1017(ElementDeFabricationId.newId());

    assertThatThrownBy(() -> elements.update(inconnu)).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  @Test
  void shouldDeleteExistingElementDeFabrication() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateOrdre1015());

    elements.delete(cree.id());

    assertThatThrownBy(() -> elements.get(cree.id())).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  @Test
  void shouldNotDeleteUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = ElementDeFabricationId.newId();

    assertThatThrownBy(() -> elements.delete(inconnu)).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  private static final class CompteurFige implements CompteurDElementsDeFabrication {

    @Override
    public long prochainNumero(TypeDElementDeFabrication type, Annee annee) {
      return NUMERO_FIGE;
    }
  }

  private record PrefixesFiges(Prefixe ordreDeFabrication, Prefixe produit) implements PrefixesDElementsDeFabrication {
    @Override
    public Prefixe prefixe(TypeDElementDeFabrication type) {
      return type == TypeDElementDeFabrication.ORDRE_DE_FABRICATION ? ordreDeFabrication : produit;
    }
  }
}
