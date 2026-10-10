package com.glm.glmback.coutderevient.infrastructure.primary;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.StatutDeLElement;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** Le PDF produit, relu avec PDFBox : mise en page et texte extrait. */
@UnitTest
class DocumentDuCoutDeRevientTest {

  private static final CompteRenduDuCout FRAISAGE_ET_SANS_POSTE_EN_COURS = new CompteRenduDuCout(
    COUT_DE_REVIENT_FRAISAGE_ET_SANS_POSTE,
    StatutDeLElement.EN_COURS,
    FUSEAU_DE_PARIS
  );

  @Test
  void shouldNommerLeFichierDApresLElementEtLaVersion() {
    assertThat(DocumentDuCoutDeRevient.nomDeFichier(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE)).isEqualTo(
      "cout-de-revient-OF-2026-000001-synthese.pdf"
    );
    assertThat(DocumentDuCoutDeRevient.nomDeFichier(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.DETAIL)).isEqualTo(
      "cout-de-revient-OF-2026-000001-detail.pdf"
    );
  }

  @Test
  void shouldMettreEnPageUneSyntheseSurUneSeulePageA4Paysage() throws IOException {
    try (PDDocument pdf = Loader.loadPDF(DocumentDuCoutDeRevient.de(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE))) {
      PDRectangle page = pdf.getPage(0).getMediaBox();

      assertThat(pdf.getNumberOfPages()).isEqualTo(1);
      assertThat(page.getWidth()).isGreaterThan(page.getHeight());
      assertThat(page.getWidth()).isCloseTo(PDRectangle.A4.getHeight(), within(1f));
    }
  }

  @Test
  void shouldPresenterLEnTeteEtLesIndicateurs() throws IOException {
    String texte = texte(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE);

    assertThat(texte)
      .contains("Coût de revient")
      .contains("OF · OF-2026-000001")
      .contains("En cours")
      .contains("Généré le 11 mai 2026 à 19:00")
      .contains("215,00 €")
      .contains("Machine 135,00 € · Main d’œuvre 80,00 €")
      .contains("Temps passé\n4 h 00")
      .contains("dont non-conformité 1 h 00")
      .contains("Page 1 / 1");
  }

  @Test
  void shouldRendreUneLigneParNatureSansAfficherUnCoutNul() throws IOException {
    String texte = texte(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE);

    assertThat(texte)
      .contains("Fraisage 2 h 00 1 h 00 3 h 00 135,00 € 60,00 € 195,00 €")
      .contains("Sans poste 1 h 00 0 h 00 1 h 00 20,00 € 20,00 €")
      .contains("Total 3 h 00 1 h 00 4 h 00 135,00 € 80,00 € 215,00 €");
  }

  @Test
  void shouldDireQuandUnElementTermineAEteClos() throws IOException {
    CompteRenduDuCout termine = new CompteRenduDuCout(
      COUT_DE_REVIENT_VIDE,
      new StatutDeLElement(Optional.of(LE_12_MAI_A_18H)),
      FUSEAU_DE_PARIS
    );

    assertThat(texte(termine, VersionDuDocument.SYNTHESE)).contains("Terminé le 12 mai 2026 à 20:00");
  }

  @Test
  void shouldFormaterDureesEtMontants() {
    assertThat(DocumentDuCoutDeRevient.duree(Duration.ofMinutes(625))).isEqualTo("10 h 25");
    assertThat(DocumentDuCoutDeRevient.euros(new BigDecimal("1164.25"))).isEqualTo("1\u202f164,25 €");
    assertThat(DocumentDuCoutDeRevient.euros(new BigDecimal("0.00"))).isEmpty();
  }

  private static String texte(CompteRenduDuCout compteRendu, VersionDuDocument version) throws IOException {
    try (PDDocument pdf = Loader.loadPDF(DocumentDuCoutDeRevient.de(compteRendu, version))) {
      return new PDFTextStripper().getText(pdf).replace(' ', ' ').replace(' ', ' ').replaceAll("[ \\t]+", " ");
    }
  }
}
