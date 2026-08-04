package com.glm.glmback.elementdefabrication.application;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabrication;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.Produit;
import com.glm.glmback.elementdefabrication.domain.ProduitId;
import com.glm.glmback.elementdefabrication.infrastructure.secondary.InMemoryCompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.infrastructure.secondary.InMemoryElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.infrastructure.secondary.InMemoryPrefixesDElementsDeFabrication;
import com.glm.glmback.shared.pagination.domain.Page;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationApplicationServiceTest {

  private static final Instant MAINTENANT = Instant.parse("2026-05-10T08:00:00Z");

  private ElementDeFabricationApplicationService applicationService;

  @BeforeEach
  void createApplicationService() {
    applicationService = new ElementDeFabricationApplicationService(
      new InMemoryElementDeFabricationRepository(),
      new InMemoryCompteurDElementsDeFabrication(),
      new InMemoryPrefixesDElementsDeFabrication(),
      Clock.fixed(MAINTENANT, ZoneOffset.UTC)
    );
  }

  @Test
  void shouldCreateOrdreDeFabricationWithGivenNom() {
    ElementDeFabrication cree = applicationService.creerOrdreDeFabrication(
      Optional.of(OF_2026_000001),
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );

    assertThat(cree).isInstanceOf(OrdreDeFabrication.class);
    assertThat(cree.nom()).isEqualTo(OF_2026_000001);
    assertThat(cree.titre()).isEqualTo(titreAssemblageCarter());
    assertThat(cree.description()).isEqualTo(descriptionCarterEnFonte());
    assertThat(cree.dateDeCreation()).isEqualTo(MAINTENANT);
    assertThat(cree.dateDeModification()).isEqualTo(MAINTENANT);
  }

  @Test
  void shouldCreateOrdreDeFabricationWithGeneratedNomWhenMissing() {
    ElementDeFabrication cree = applicationService.creerOrdreDeFabrication(
      Optional.empty(),
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );

    assertThat(cree.nom().value()).startsWith("OF-2026-");
  }

  @Test
  void shouldCreateProduitWithGivenNom() {
    ElementDeFabrication cree = applicationService.creerProduit(
      Optional.of(PRD_2026_000001),
      titreCarterMoteur(),
      descriptionCarterEnFonte()
    );

    assertThat(cree).isInstanceOf(Produit.class);
    assertThat(cree.nom()).isEqualTo(PRD_2026_000001);
  }

  @Test
  void shouldCreateProduitWithGeneratedNomWhenMissing() {
    ElementDeFabrication cree = applicationService.creerProduit(Optional.empty(), titreCarterMoteur(), descriptionCarterEnFonte());

    assertThat(cree.nom().value()).startsWith("PRD-2026-");
  }

  @Test
  void shouldGetExistingElementDeFabrication() {
    ElementDeFabrication cree = applicationService.creerOrdreDeFabrication(
      Optional.empty(),
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );

    ElementDeFabrication trouve = applicationService.obtenir(cree.id());

    assertThat(trouve).isEqualTo(cree);
  }

  @Test
  void shouldNotGetUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = OrdreDeFabricationId.newId();

    assertThatThrownBy(() -> applicationService.obtenir(inconnu)).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  @Test
  void shouldListElementsDeFabricationInPeriode() {
    ElementDeFabrication ordre = applicationService.creerOrdreDeFabrication(
      Optional.empty(),
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );
    ElementDeFabrication produit = applicationService.creerProduit(Optional.empty(), titreCarterMoteur(), descriptionCarterEnFonte());

    Page<ElementDeFabrication> page = applicationService.lister(
      new Periode(MAINTENANT.minusSeconds(1), MAINTENANT.plusSeconds(1)),
      firstPageOfTen()
    );

    assertThat(page.content()).containsExactlyInAnyOrder(ordre, produit);
  }

  @Test
  void shouldModifyExistingOrdreDeFabrication() {
    ElementDeFabrication cree = applicationService.creerOrdreDeFabrication(
      Optional.empty(),
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );

    ElementDeFabrication modifie = applicationService.modifier(cree.id(), titreAssemblageCarterRevise(), descriptionCarterEnFonte());

    assertThat(modifie.id()).isEqualTo(cree.id());
    assertThat(modifie.nom()).isEqualTo(cree.nom());
    assertThat(modifie.titre()).isEqualTo(titreAssemblageCarterRevise());
    assertThat(modifie.dateDeCreation()).isEqualTo(cree.dateDeCreation());
    assertThat(modifie.dateDeModification()).isEqualTo(MAINTENANT);
    assertThat(applicationService.obtenir(cree.id())).isEqualTo(modifie);
  }

  @Test
  void shouldModifyExistingProduit() {
    ElementDeFabrication cree = applicationService.creerProduit(Optional.empty(), titreCarterMoteur(), descriptionCarterEnFonte());

    ElementDeFabrication modifie = applicationService.modifier(cree.id(), titreAssemblageCarterRevise(), descriptionCarterEnFonte());

    assertThat(modifie).isInstanceOf(Produit.class);
    assertThat(modifie.id()).isEqualTo(cree.id());
    assertThat(modifie.nom()).isEqualTo(cree.nom());
  }

  @Test
  void shouldNotModifyUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = ProduitId.newId();

    assertThatThrownBy(() -> applicationService.modifier(inconnu, titreAssemblageCarter(), descriptionCarterEnFonte())).isExactlyInstanceOf(
      ElementDeFabricationIntrouvableException.class
    );
  }

  @Test
  void shouldDeleteExistingElementDeFabrication() {
    ElementDeFabrication cree = applicationService.creerOrdreDeFabrication(
      Optional.empty(),
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );

    applicationService.supprimer(cree.id());

    assertThatThrownBy(() -> applicationService.obtenir(cree.id())).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }

  @Test
  void shouldNotDeleteUnknownElementDeFabrication() {
    ElementDeFabricationId inconnu = OrdreDeFabricationId.newId();

    assertThatThrownBy(() -> applicationService.supprimer(inconnu)).isExactlyInstanceOf(ElementDeFabricationIntrouvableException.class);
  }
}
