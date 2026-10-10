package com.glm.glmback.coutderevient.infrastructure.primary;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.ChargesDesOperateurs;
import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.EvaluationDuCout;
import com.glm.glmback.coutderevient.domain.LogoDeLEntreprise;
import com.glm.glmback.coutderevient.domain.Periode;
import com.glm.glmback.coutderevient.domain.StatutDeLElement;
import com.glm.glmback.coutderevient.domain.TrancheDActivite;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
    try (
      PDDocument pdf = Loader.loadPDF(
        DocumentDuCoutDeRevient.de(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE, Optional.empty())
      )
    ) {
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

  @Test
  void shouldNeDetaillerLesPointagesQueDansLaVersionDetaillee() throws IOException {
    assertThat(texte(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE)).doesNotContain("pointages");
  }

  @Test
  void shouldDetaillerChaquePointageParNature() throws IOException {
    String texte = texte(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.DETAIL);

    assertThat(texte)
      .contains("Coût de revient — détaillé")
      .contains("Fraisage — 2 pointages")
      .contains("DMG DMU 50 Jean Dupont 11 mai · 11:00 → 13:00 2 h 00 90,00 € 40,00 € 130,00 €")
      .contains("DMG DMU 50 NC Jean Dupont 11 mai · 13:00 → 14:00 1 h 00 45,00 € 20,00 € 65,00 €")
      .contains("Total Fraisage 3 h 00 135,00 € 60,00 € 195,00 €")
      .contains("Sans poste — 1 pointage")
      .contains("Sans poste Jean Dupont 11 mai · 15:00 → 16:00 1 h 00 20,00 € 20,00 €")
      .contains("Total Sans poste 1 h 00 20,00 € 20,00 €");
  }

  @Test
  void shouldRepeterLEnTeteEtNumeroterChaquePageDUnLongDetail() throws IOException {
    List<TrancheDActivite> tranches = IntStream.range(0, 300)
      .mapToObj(heure ->
        new TrancheDActivite(
          ACTIVITE_FRAISAGE,
          new Periode(LE_11_MAI_A_8H.plusSeconds(3600L * heure), LE_11_MAI_A_9H.plusSeconds(3600L * heure))
        )
      )
      .toList();
    CoutDeRevient troisCentsHeures = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .annuaire(AnnuaireDuCout.VIDE);

    try (
      PDDocument pdf = Loader.loadPDF(
        DocumentDuCoutDeRevient.de(
          new CompteRenduDuCout(troisCentsHeures, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS),
          VersionDuDocument.DETAIL,
          Optional.empty()
        )
      )
    ) {
      int pages = pdf.getNumberOfPages();
      assertThat(pages).isGreaterThan(5);
      PDFTextStripper extraction = new PDFTextStripper();
      for (int page = 1; page <= pages; page++) {
        extraction.setStartPage(page);
        extraction.setEndPage(page);
        assertThat(extraction.getText(pdf)).contains("Coût de revient — détaillé").contains("Page %d / %d".formatted(page, pages));
      }
    }
  }

  @Test
  void shouldRepeterLeJourDeFinDUnPointageQuiPasseMinuit() {
    assertThat(DocumentDuCoutDeRevient.periode(LE_11_MAI_A_21H, LE_12_MAI_A_7H, FUSEAU_DE_PARIS)).isEqualTo(
      "11 mai · 23:00 → 12 mai · 09:00"
    );
  }

  @Test
  void shouldAjusterLeLogoALaHauteurDeSaCaseSansLeDeformer() throws IOException {
    DocumentDuCoutDeRevient.LogoVue logo = DocumentDuCoutDeRevient.LogoVue.de(logoPngDe256Sur100()).orElseThrow();

    assertThat(logo.source()).startsWith("data:image/png;base64,");
    assertThat(logo.largeur()).isEqualTo("40.96mm");
    assertThat(logo.hauteur()).isEqualTo("16.00mm");
  }

  @Test
  void shouldBornerUnLogoTropLargeALaLargeurDeSaCase() throws IOException {
    DocumentDuCoutDeRevient.LogoVue logo = DocumentDuCoutDeRevient.LogoVue.de(logoPngDe300Sur40()).orElseThrow();

    assertThat(logo.largeur()).isEqualTo("48.00mm");
    assertThat(logo.hauteur()).isEqualTo("6.40mm");
  }

  @Test
  void shouldEmbarquerUnLogoJpegSousSonType() throws IOException {
    DocumentDuCoutDeRevient.LogoVue logo = DocumentDuCoutDeRevient.LogoVue.de(logoJpegDe100Sur100()).orElseThrow();

    assertThat(logo.source()).startsWith("data:image/jpeg;base64,");
    assertThat(logo.largeur()).isEqualTo("16.00mm");
  }

  @Test
  void shouldPorterLeLogoDansLEnTete() throws IOException {
    assertThat(images(Optional.of(logoPngDe256Sur100()))).containsExactly("256x100");
  }

  @Test
  void shouldLaisserLaCaseDuLogoVideSansLogo() throws IOException {
    assertThat(images(Optional.empty())).isEmpty();
  }

  @Test
  void shouldIgnorerUnLogoIllisible() throws IOException {
    assertThat(images(Optional.of(logoIllisible()))).isEmpty();
  }

  private static List<String> images(Optional<LogoDeLEntreprise> logo) throws IOException {
    try (PDDocument pdf = Loader.loadPDF(DocumentDuCoutDeRevient.de(FRAISAGE_ET_SANS_POSTE_EN_COURS, VersionDuDocument.SYNTHESE, logo))) {
      PDResources ressources = pdf.getPage(0).getResources();
      List<String> images = new ArrayList<>();
      for (COSName nom : ressources.getXObjectNames()) {
        if (ressources.getXObject(nom) instanceof PDImageXObject image) {
          images.add(image.getWidth() + "x" + image.getHeight());
        }
      }
      return images;
    }
  }

  private static String texte(CompteRenduDuCout compteRendu, VersionDuDocument version) throws IOException {
    try (PDDocument pdf = Loader.loadPDF(DocumentDuCoutDeRevient.de(compteRendu, version, Optional.empty()))) {
      return new PDFTextStripper().getText(pdf).replace(' ', ' ').replace(' ', ' ').replaceAll("[ \\t]+", " ");
    }
  }
}
