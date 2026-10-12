package com.glm.glmback.syntheseheures.infrastructure.primary;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.syntheseheures.domain.FormatDuLogo;
import com.glm.glmback.syntheseheures.domain.LogoDeLEntreprise;
import com.glm.glmback.syntheseheures.infrastructure.primary.DocumentDuRapportDActivite.CaseVue;
import com.glm.glmback.syntheseheures.infrastructure.primary.DocumentDuRapportDActivite.JourVue;
import com.glm.glmback.syntheseheures.infrastructure.primary.DocumentDuRapportDActivite.LogoVue;
import com.glm.glmback.syntheseheures.infrastructure.primary.DocumentDuRapportDActivite.PointageVue;
import com.glm.glmback.syntheseheures.infrastructure.primary.DocumentDuRapportDActivite.SemaineVue;
import com.glm.glmback.syntheseheures.infrastructure.primary.DocumentDuRapportDActivite.Vue;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** Le PDF de mai 2026 : ce que le gabarit recoit deja formate, puis le document rendu relu avec PDFBox. */
@UnitTest
class DocumentDuRapportDActiviteTest {

  @Test
  void shouldNommerLeFichierDApresLOperateurLaPeriodeEtLaVersion() {
    assertThat(DocumentDuRapportDActivite.nomDeFichier(rapportDActiviteDeMai2026(), VersionDuRapport.DETAIL)).isEqualTo(
      "activite-dupont-jean-2026-05-detail.pdf"
    );
  }

  @Test
  void shouldFormaterLEnTeteEtLesChiffresCles() {
    Vue vue = DocumentDuRapportDActivite.vue(rapportDActiviteDeMai2026(), VersionDuRapport.SYNTHESE);

    assertThat(vue.titre()).isEqualTo("Rapport d'activité");
    assertThat(vue.operateur()).isEqualTo("Jean Dupont");
    assertThat(vue.periode()).isEqualTo("Mai 2026");
    assertThat(vue.generation()).isEqualTo("13/05/2026 à 12:00");
    assertThat(vue.tempsPointe()).isEqualTo("16 h 00");
    assertThat(vue.tempsPointeEnHeures()).isEqualTo("16,00 h");
    assertThat(vue.joursAvecPointage()).isEqualTo("3");
    assertThat(vue.pointages()).isEqualTo("6 pointages");
    assertThat(vue.nombreDeNatures()).isEqualTo("3 natures de travail");
    assertThat(vue.nonConformite()).isEqualTo("1 h 00");
    assertThat(vue.partDeNonConformite()).isEqualTo("6 % du temps pointé");
    assertThat(vue.simultanee()).isTrue();
    assertThat(vue.finAutomatique()).isTrue();
  }

  /** Mai 2026 commence un vendredi et finit un dimanche : cinq semaines, du lundi 27 avril au dimanche 31 mai. */
  @Test
  void shouldPoserLeMoisDansUnCalendrierDeSemainesCompletes() {
    List<SemaineVue> semaines = DocumentDuRapportDActivite.vue(rapportDActiviteDeMai2026(), VersionDuRapport.SYNTHESE).semaines();

    assertThat(semaines).extracting(SemaineVue::libelle).containsExactly("S18", "S19", "S20", "S21", "S22");
    assertThat(semaines.getFirst().cases()).hasSize(7);
    assertThat(semaines.getFirst().cases().getFirst()).isEqualTo(new CaseVue("27/04", "", true, "", false, false, false, true));
    assertThat(semaines.getFirst().cases().get(4)).isEqualTo(new CaseVue("01/05", "—", true, "", false, false, false, false));
    assertThat(semaines.getFirst().cases().get(5).weekEnd()).isTrue();
    assertThat(semaines.get(2).cases().getFirst()).isEqualTo(
      new CaseVue("11/05", "5 h 00", false, "dont NC 1 h 00", false, true, false, false)
    );
    assertThat(semaines.get(2).cases().get(1).finAutomatique()).isTrue();
    assertThat(semaines.get(2).total()).isEqualTo("16 h 00");
    assertThat(semaines.getFirst().total()).isEqualTo("0 h 00");
  }

  @Test
  void shouldPorterLesElementsEtLesNaturesSansDureeNulle() {
    Vue vue = DocumentDuRapportDActivite.vue(rapportDActiviteDeMai2026(), VersionDuRapport.SYNTHESE);

    assertThat(vue.elements().getFirst()).isEqualTo(
      new DocumentDuRapportDActivite.ElementVue(
        "PRD-2026-000015",
        "1015",
        "Carter de pompe",
        "MOULE",
        "DMU 50, Tour 14",
        "1 h 00",
        "13 h 00"
      )
    );
    assertThat(vue.elements().get(1).nonConformite()).isEmpty();
    assertThat(vue.elements().get(1).reference()).isEmpty();
    assertThat(vue.natures()).containsExactly(
      new DocumentDuRapportDActivite.NatureVue("Fraisage", "15 h 00", "94 %"),
      new DocumentDuRapportDActivite.NatureVue("Sans poste", "1 h 00", "6 %"),
      new DocumentDuRapportDActivite.NatureVue("Tournage", "0 h 00", "0 %")
    );
    assertThat(vue.journal()).isEmpty();
  }

  @Test
  void shouldTairePourUneSeuleNatureLeTableauDesNatures() {
    Vue vue = DocumentDuRapportDActivite.vue(rapportDActiviteDeLaSemaine20(), VersionDuRapport.SYNTHESE);

    assertThat(vue.natures()).isEmpty();
    assertThat(vue.nombreDeNatures()).isEqualTo("1 nature de travail");
    assertThat(vue.pointages()).isEqualTo("1 pointage");
    assertThat(vue.semaines()).hasSize(1);
  }

  @Test
  void shouldCompterZeroPourUnePeriodeSansPointage() {
    Vue vue = DocumentDuRapportDActivite.vue(rapportDActiviteVideDeLaSemaine19(), VersionDuRapport.SYNTHESE);

    assertThat(vue.tempsPointe()).isEqualTo("0 h 00");
    assertThat(vue.partDeNonConformite()).isEqualTo("0 % du temps pointé");
    assertThat(vue.simultanee()).isFalse();
    assertThat(vue.finAutomatique()).isFalse();
  }

  @Test
  void shouldDetaillerLeJournalJourParJour() {
    List<JourVue> journal = DocumentDuRapportDActivite.vue(rapportDActiviteDeMai2026(), VersionDuRapport.DETAIL).journal();

    assertThat(journal)
      .extracting(JourVue::libelle, JourVue::total)
      .containsExactly(tuple("Lundi 11 mai", "5 h 00"), tuple("Mardi 12 mai", "10 h 00"), tuple("Mercredi 13 mai", "1 h 00"));
    assertThat(journal.getFirst().pointages().getFirst()).isEqualTo(
      new PointageVue(
        "08:00",
        "10:00",
        "PRD-2026-000015",
        "1015",
        "DMU 50",
        "Fraisage",
        "2 h 00",
        false,
        List.of("en même temps qu'un autre pointage")
      )
    );
    assertThat(journal.getFirst().pointages().get(2).nonConformite()).isTrue();
    assertThat(journal.getFirst().pointages().get(1).reference()).isEmpty();
    assertThat(journal.get(2).pointages().get(1)).isEqualTo(
      new PointageVue("10:00", "", "PRD-2026-000015", "1015", "Tour 14", "Tournage", "", false, List.of("en cours"))
    );
  }

  @Test
  void shouldRendreUnPdfA4PortraitAvecSonJournal() throws IOException {
    try (
      PDDocument pdf = Loader.loadPDF(DocumentDuRapportDActivite.de(rapportDActiviteDeMai2026(), VersionDuRapport.DETAIL, Optional.empty()))
    ) {
      String texte = new PDFTextStripper().getText(pdf);

      assertThat(pdf.getPage(0).getMediaBox().getHeight()).isCloseTo(PDRectangle.A4.getHeight(), within(1f));
      assertThat(pdf.getNumberOfPages()).isGreaterThanOrEqualTo(2);
      assertThat(texte)
        .contains("Rapport d'activité · détaillé")
        .contains("Jean Dupont")
        .contains("Journal des pointages")
        .contains("Le temps pointé mesure le temps passé sur les éléments, pas la présence.")
        .contains("Page 1 / " + pdf.getNumberOfPages());
      assertThat(images(pdf)).isEmpty();
    }
  }

  @Test
  void shouldPorterLeLogoDansLEnTete() throws IOException {
    LogoDeLEntreprise logo = new LogoDeLEntreprise(png(256, 100), FormatDuLogo.PNG);

    try (
      PDDocument pdf = Loader.loadPDF(
        DocumentDuRapportDActivite.de(rapportDActiviteDeLaSemaine20(), VersionDuRapport.SYNTHESE, Optional.of(logo))
      )
    ) {
      assertThat(images(pdf)).containsExactly("256 x 100");
    }
  }

  @Test
  void shouldAjusterLeLogoASaCaseSansLeDeformer() throws IOException {
    assertThat(LogoVue.de(new LogoDeLEntreprise(png(256, 100), FormatDuLogo.PNG)).orElseThrow())
      .extracting(LogoVue::largeur, LogoVue::hauteur)
      .containsExactly("35.84mm", "14.00mm");
    assertThat(LogoVue.de(new LogoDeLEntreprise(png(256, 16), FormatDuLogo.JPEG)).orElseThrow())
      .extracting(LogoVue::source, LogoVue::largeur, LogoVue::hauteur)
      .satisfies(valeurs -> assertThat((String) valeurs.getFirst()).startsWith("data:image/jpeg;base64,"))
      .contains("46.00mm", "2.88mm");
  }

  @Test
  void shouldIgnorerUnLogoIllisible() throws IOException {
    assertThat(LogoVue.de(new LogoDeLEntreprise(new byte[] { 1, 2, 3 }, FormatDuLogo.PNG))).isEmpty();
  }

  private static byte[] png(int largeur, int hauteur) throws IOException {
    try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
      ImageIO.write(new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB), "png", sortie);
      return sortie.toByteArray();
    }
  }

  private static List<String> images(PDDocument pdf) throws IOException {
    PDResources ressources = pdf.getPage(0).getResources();
    List<String> images = new ArrayList<>();
    for (COSName nom : ressources.getXObjectNames()) {
      if (ressources.getXObject(nom) instanceof PDImageXObject image) {
        images.add(image.getWidth() + " x " + image.getHeight());
      }
    }
    return images;
  }
}
