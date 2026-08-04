package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementsDeFabricationTest {

  private static final long NUMERO_FIGE = 42;

  private final ElementsDeFabrication elements = new ElementsDeFabrication(new CompteurFige(), new PrefixesFiges(PREFIXE_OF, PREFIXE_PRD));

  @Test
  void shouldNotBuildWithoutCompteur() {
    PrefixesDElementsDeFabrication prefixes = new PrefixesFiges(PREFIXE_OF, PREFIXE_PRD);

    assertThatThrownBy(() -> new ElementsDeFabrication(null, prefixes))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("compteur");
  }

  @Test
  void shouldNotBuildWithoutPrefixes() {
    CompteurDElementsDeFabrication compteur = new CompteurFige();

    assertThatThrownBy(() -> new ElementsDeFabrication(compteur, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prefixes");
  }

  @Test
  void shouldKeepNomDOrdreDeFabricationWhenGiven() {
    OrdreDeFabrication ordre = elements.ordreDeFabrication(
      OrdreDeFabricationId.newId(),
      Optional.of(OF_2026_000001),
      ficheAssemblageCarter()
    );

    assertThat(ordre.nom()).isEqualTo(OF_2026_000001);
    assertThat(ordre.fiche()).isEqualTo(ficheAssemblageCarter());
  }

  @Test
  void shouldGenerateNomDOrdreDeFabricationWhenMissing() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();

    OrdreDeFabrication ordre = elements.ordreDeFabrication(id, Optional.empty(), ficheAssemblageCarter());

    assertThat(ordre.id()).isEqualTo(id);
    assertThat(ordre.nom().value()).isEqualTo("OF-2026-000042");
  }

  @Test
  void shouldGenerateNomFromPrefixesOfSecondaryAdapter() {
    ElementsDeFabrication autreNomenclature = new ElementsDeFabrication(
      new CompteurFige(),
      new PrefixesFiges(new Prefixe("FAB"), new Prefixe("ART"))
    );

    OrdreDeFabrication ordre = autreNomenclature.ordreDeFabrication(
      OrdreDeFabricationId.newId(),
      Optional.empty(),
      ficheAssemblageCarter()
    );
    Produit produit = autreNomenclature.produit(ProduitId.newId(), Optional.empty(), ficheCarterMoteur());

    assertThat(ordre.nom().value()).isEqualTo("FAB-2026-000042");
    assertThat(produit.nom().value()).isEqualTo("ART-2026-000042");
  }

  @Test
  void shouldGenerateNomFromAnneeOfDateDeCreation() {
    Fiche ficheDe2027 = Fiche.builder()
      .titre(titreAssemblageCarter())
      .description(descriptionCarterEnFonte())
      .dateDeCreation(Instant.parse("2027-06-01T00:00:00Z"))
      .dateDeModification(Instant.parse("2027-06-01T00:00:00Z"));

    OrdreDeFabrication ordre = elements.ordreDeFabrication(OrdreDeFabricationId.newId(), Optional.empty(), ficheDe2027);

    assertThat(ordre.nom().value()).isEqualTo("OF-2027-000042");
  }

  @Test
  void shouldKeepNomDeProduitWhenGiven() {
    Produit produit = elements.produit(ProduitId.newId(), Optional.of(PRD_2026_000001), ficheCarterMoteur());

    assertThat(produit.nom()).isEqualTo(PRD_2026_000001);
    assertThat(produit.fiche()).isEqualTo(ficheCarterMoteur());
  }

  @Test
  void shouldGenerateNomDeProduitWhenMissing() {
    ProduitId id = ProduitId.newId();

    Produit produit = elements.produit(id, Optional.empty(), ficheCarterMoteur());

    assertThat(produit.id()).isEqualTo(id);
    assertThat(produit.nom().value()).isEqualTo("PRD-2026-000042");
  }

  private static final class CompteurFige implements CompteurDElementsDeFabrication {

    @Override
    public long prochainNumeroDOrdreDeFabrication(Annee annee) {
      return NUMERO_FIGE;
    }

    @Override
    public long prochainNumeroDeProduit(Annee annee) {
      return NUMERO_FIGE;
    }
  }

  private record PrefixesFiges(Prefixe ordreDeFabrication, Prefixe produit) implements PrefixesDElementsDeFabrication {
    @Override
    public Prefixe prefixeDOrdreDeFabrication() {
      return ordreDeFabrication;
    }

    @Override
    public Prefixe prefixeDeProduit() {
      return produit;
    }
  }
}
