package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static com.glm.glmback.shared.time.domain.TimeFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import java.time.Instant;
import java.util.Set;
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
    .categories(new CategoriesFigees(Set.of(CATEGORIE_OF, CATEGORIE_MOULE)))
    .clock(maintenant::get);

  @Test
  void shouldCreateOrdreDeFabricationWithGeneratedNom() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateOrdre1015());

    assertThat(cree.categorie()).isEqualTo(CATEGORIE_OF);
    assertThat(cree.nom().value()).isEqualTo("OF-2026-000042");
    assertThat(cree.reference()).contains(reference1015());
    assertThat(cree.description()).contains(descriptionCarterEnFonte());
    assertThat(cree.dateDeCreation()).isEqualTo(LE_10_MAI_2026);
    assertThat(cree.dateDeModification()).isEqualTo(LE_10_MAI_2026);
    assertThat(elements.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreateMouleWithGeneratedNom() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateMoule2456());

    assertThat(cree.categorie()).isEqualTo(CATEGORIE_MOULE);
    assertThat(cree.nom().value()).isEqualTo("MOULE-2026-000042");
    assertThat(cree.reference()).contains(reference2456());
    assertThat(elements.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreateMouleWithoutReferenceNorDescription() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateMouleSansReference());

    assertThat(cree.nom().value()).isEqualTo("MOULE-2026-000042");
    assertThat(cree.reference()).isEmpty();
    assertThat(cree.description()).isEmpty();
    assertThat(elements.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreateManyElementsDeFabricationWithoutReference() {
    elements.create(elementDeFabricationToCreateMouleSansReference());

    ElementDeFabrication second = elements.create(elementDeFabricationToCreateMouleSansReference());

    assertThat(second.reference()).isEmpty();
  }

  @Test
  void shouldNotCreateElementDeFabricationWithReferenceOfAnotherOne() {
    elements.create(elementDeFabricationToCreateOrdre1015());

    ElementDeFabricationToCreate memeReference = elementDeFabricationToCreateMoule1015();

    assertThatThrownBy(() -> elements.create(memeReference))
      .isExactlyInstanceOf(ReferenceDejaUtiliseeException.class)
      .hasMessageContaining("1015");
  }

  @Test
  void shouldPrefixNomWithAnyCategorieOfTheEntreprise() {
    ElementsDeFabricationService autreEntreprise = ElementsDeFabricationService.builder()
      .repository(new ElementsDeFabricationEnMemoire())
      .compteur(new CompteurFige())
      .categories(new CategoriesFigees(Set.of(new Categorie("FAB"))))
      .clock(fixedClock(LE_10_MAI_2026));

    ElementDeFabrication cree = autreEntreprise.create(new ElementDeFabricationToCreate("FAB", "1015", null));

    assertThat(cree.nom().value()).isEqualTo("FAB-2026-000042");
  }

  @Test
  void shouldNotCreateElementInUndeclaredCategorie() {
    ElementDeFabricationToCreate dansUneCategorieInconnue = new ElementDeFabricationToCreate("ART", "1015", null);

    assertThatThrownBy(() -> elements.create(dansUneCategorieInconnue))
      .isExactlyInstanceOf(CategorieInconnueException.class)
      .hasMessageContaining("ART");
  }

  @Test
  void shouldGenerateNomFromAnneeOfClock() {
    ElementsDeFabricationService elementsDe2027 = ElementsDeFabricationService.builder()
      .repository(new ElementsDeFabricationEnMemoire())
      .compteur(new CompteurFige())
      .categories(new CategoriesFigees(Set.of(CATEGORIE_OF, CATEGORIE_MOULE)))
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
    ElementDeFabrication produit = elements.create(elementDeFabricationToCreateMoule2456());

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

    assertThat(modifie.categorie()).isEqualTo(CATEGORIE_OF);
    assertThat(modifie.id()).isEqualTo(cree.id());
    assertThat(modifie.nom()).isEqualTo(cree.nom());
    assertThat(modifie.reference()).contains(reference1017());
    assertThat(modifie.dateDeCreation()).isEqualTo(LE_10_MAI_2026);
    assertThat(modifie.dateDeModification()).isEqualTo(LE_15_JUIN_2026);
    assertThat(elements.get(cree.id())).isEqualTo(modifie);
  }

  @Test
  void shouldUpdateMoule() {
    ElementDeFabrication cree = elements.create(elementDeFabricationToCreateMoule2456());
    maintenant.set(LE_15_JUIN_2026);

    ElementDeFabrication modifie = elements.update(elementDeFabricationToUpdate1017(cree.id()));

    assertThat(modifie.categorie()).isEqualTo(CATEGORIE_MOULE);
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
    ElementDeFabrication produit = elements.create(elementDeFabricationToCreateMoule2456());

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
    public long prochainNumero(Categorie categorie, Annee annee) {
      return NUMERO_FIGE;
    }
  }

  private record CategoriesFigees(Set<Categorie> declarees) implements CategoriesDeclarees {
    @Override
    public boolean existe(Categorie categorie) {
      return declarees.contains(categorie);
    }
  }
}
