package com.glm.glmback.coutderevient.infrastructure.primary;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.ChargesDesOperateurs;
import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.EvaluationDuCout;
import com.glm.glmback.coutderevient.domain.Periode;
import com.glm.glmback.coutderevient.domain.StatutDeLElement;
import com.glm.glmback.coutderevient.domain.TrancheDActivite;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Le classeur produit, relu avec POI : cellules, types et formats. */
@UnitTest
class ClasseurDuCoutDeRevientTest {

  private static final String FORMAT_EURO = "#,##0.00 \"€\";-#,##0.00 \"€\";\"\"";

  @Test
  void shouldNommerLeFichierDApresLElement() {
    assertThat(
      ClasseurDuCoutDeRevient.nomDeFichier(new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS))
    ).isEqualTo("cout-de-revient-OF-2026-000001.xlsx");
  }

  @Test
  void shouldPresenterLElementEtLInstantDeGenerationDansLeFuseauDeLEntreprise() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      XSSFSheet synthese = classeur.getSheet("Synthèse");

      assertThat(texte(synthese, 0, 0)).isEqualTo("Coût de revient");
      assertThat(texte(synthese, 1, 0)).isEqualTo("Élément");
      assertThat(texte(synthese, 1, 1)).isEqualTo("OF · OF-2026-000001");
      assertThat(texte(synthese, 2, 0)).isEqualTo("Statut");
      assertThat(texte(synthese, 2, 1)).isEqualTo("En cours");
      assertThat(texte(synthese, 3, 0)).isEqualTo("Généré le");
      Cell generation = synthese.getRow(3).getCell(1);
      assertThat(generation.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.parse("2026-05-11T19:00"));
      assertThat(generation.getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy hh:mm");
    }
  }

  @Test
  void shouldRendreUneLigneParNatureDansUnTableauNomme() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      XSSFSheet synthese = classeur.getSheet("Synthèse");
      XSSFTable natures = classeur.getTable("Natures");

      assertThat(natures.getSheetName()).isEqualTo("Synthèse");
      assertThat(natures.getArea().formatAsString()).isEqualTo("A6:G8");
      assertThat(natures.getStyleName()).isEqualTo("TableStyleMedium2");
      assertThat(natures.getCTTable().getAutoFilter().getRef()).isEqualTo("A6:G8");
      assertThat(
        natures
          .getColumns()
          .stream()
          .map(colonne -> colonne.getName())
          .toList()
      ).containsExactly("Nature", "Travail (h)", "Non-conformité (h)", "Temps total (h)", "Machine (€)", "Main d’œuvre (€)", "Total (€)");
      assertThat(texte(synthese, 6, 0)).isEqualTo("Fraisage");
      assertThat(nombres(synthese.getRow(6))).containsExactly(2.0, 1.0, 3.0, 135.0, 60.0, 195.0);
      assertThat(texte(synthese, 7, 0)).isEqualTo("Sans poste");
      assertThat(nombres(synthese.getRow(7))).containsExactly(1.0, 0.0, 1.0, 0.0, 20.0, 20.0);
      assertThat(synthese.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 6);
    }
  }

  @Test
  void shouldEcrireLesMontantsEnEurosEtMasquerUnCoutNul() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      Row sansPoste = classeur.getSheet("Synthèse").getRow(7);

      assertThat(sansPoste.getCell(1).getCellStyle().getDataFormatString()).isEqualTo("0.00");
      assertThat(sansPoste.getCell(4).getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(sansPoste.getCell(4).getCellStyle().getDataFormatString()).isEqualTo(FORMAT_EURO);
      assertThat(sansPoste.getCell(6).getCellStyle().getDataFormatString()).isEqualTo(FORMAT_EURO);
    }
  }

  @Test
  void shouldTotaliserCommeLEcranSousLeTableau() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      Row total = classeur.getSheet("Synthèse").getRow(8);

      assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
      assertThat(nombres(total)).containsExactly(3.0, 1.0, 4.0, 135.0, 80.0, 215.0);
      assertThat(classeur.getFontAt(total.getCell(6).getCellStyle().getFontIndex()).getBold()).isTrue();
    }
  }

  @Test
  void shouldNeDresserAucunTableauPourUnRapportVide() throws IOException {
    try (XSSFWorkbook classeur = lis(new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS))) {
      XSSFSheet synthese = classeur.getSheet("Synthèse");

      assertThat(synthese.getTables()).isEmpty();
      assertThat(texte(synthese, 6, 0)).isEqualTo("Total");
      assertThat(nombres(synthese.getRow(6))).containsExactly(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
  }

  @Test
  void shouldDireQuandUnElementTermineAEteClos() throws IOException {
    StatutDeLElement termine = new StatutDeLElement(Optional.of(LE_12_MAI_A_18H));

    try (XSSFWorkbook classeur = lis(new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, termine, FUSEAU_DE_PARIS))) {
      assertThat(texte(classeur.getSheet("Synthèse"), 2, 1)).isEqualTo("Terminé le 12 mai 2026 à 20:00");
    }
  }

  @Test
  void shouldRendreUneLigneParPointageDansLeTableauPointages() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      XSSFTable pointages = classeur.getTable("Pointages");

      assertThat(classeur.getSheetName(1)).isEqualTo("Pointages");
      assertThat(pointages.getSheetName()).isEqualTo("Pointages");
      assertThat(pointages.getArea().formatAsString()).isEqualTo("A1:J4");
      assertThat(pointages.getCTTable().getAutoFilter().getRef()).isEqualTo("A1:J4");
      assertThat(
        pointages
          .getColumns()
          .stream()
          .map(colonne -> colonne.getName())
          .toList()
      ).containsExactly(
        "Nature",
        "Poste",
        "Opérateur",
        "Catégorie",
        "Début",
        "Fin",
        "Durée (h)",
        "Machine (€)",
        "Main d’œuvre (€)",
        "Total (€)"
      );
      assertThat(classeur.getSheet("Pointages").getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
      assertThat(classeur.getSheet("Pointages").getNumMergedRegions()).isZero();
    }
  }

  @Test
  void shouldEcrireChaquePointageAvecDesTypesNatifs() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      XSSFSheet pointages = classeur.getSheet("Pointages");

      assertThat(textes(pointages.getRow(1))).containsExactly("Fraisage", "DMG DMU 50", "Jean Dupont", "Travail");
      assertThat(pointages.getRow(1).getCell(4).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.parse("2026-05-11T11:00"));
      assertThat(pointages.getRow(1).getCell(5).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.parse("2026-05-11T13:00"));
      assertThat(pointages.getRow(1).getCell(4).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy hh:mm");
      assertThat(valeursDuPointage(pointages.getRow(1))).containsExactly(2.0, 90.0, 40.0, 130.0);
      assertThat(textes(pointages.getRow(2))).containsExactly("Fraisage", "DMG DMU 50", "Jean Dupont", "Non-conformité");
      assertThat(valeursDuPointage(pointages.getRow(2))).containsExactly(1.0, 45.0, 20.0, 65.0);
      assertThat(textes(pointages.getRow(3))).containsExactly("Sans poste", "Sans poste", "Jean Dupont", "Travail");
      assertThat(valeursDuPointage(pointages.getRow(3))).containsExactly(1.0, 0.0, 20.0, 20.0);
      assertThat(pointages.getRow(3).getCell(6).getCellStyle().getDataFormatString()).isEqualTo("0.00");
      assertThat(pointages.getRow(3).getCell(7).getCellStyle().getDataFormatString()).isEqualTo(FORMAT_EURO);
    }
  }

  @Test
  void shouldRetrouverLaSyntheseEnRegroupantLesPointagesParNature() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      XSSFSheet pointages = classeur.getSheet("Pointages");
      List<Double> premier = valeursDuPointage(pointages.getRow(1));
      List<Double> second = valeursDuPointage(pointages.getRow(2));
      Row fraisage = classeur.getSheet("Synthèse").getRow(6);

      assertThat(
        List.of(0, 1, 2, 3)
          .stream()
          .map(colonne -> premier.get(colonne) + second.get(colonne))
          .toList()
      ).containsExactly(
        fraisage.getCell(3).getNumericCellValue(),
        fraisage.getCell(4).getNumericCellValue(),
        fraisage.getCell(5).getNumericCellValue(),
        fraisage.getCell(6).getNumericCellValue()
      );
    }
  }

  @Test
  void shouldGarderLIdentifiantDUnNomAbsent() throws IOException {
    CoutDeRevient sansNoms = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(List.of(new TrancheDActivite(ACTIVITE_TOURNAGE, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H))))
      .charges(ChargesDesOperateurs.de(List.of(new TrancheDActivite(ACTIVITE_TOURNAGE, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H)))))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .annuaire(AnnuaireDuCout.VIDE);

    try (XSSFWorkbook classeur = lis(new CompteRenduDuCout(sansNoms, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS))) {
      assertThat(textes(classeur.getSheet("Pointages").getRow(1))).containsExactly(
        "Tournage",
        POSTE_ID_TOUR.uuid().toString(),
        OPERATEUR_ID_DUPONT.uuid().toString(),
        "Travail"
      );
    }
  }

  @Test
  void shouldNeDresserQueLEnTeteDesPointagesPourUnRapportVide() throws IOException {
    try (XSSFWorkbook classeur = lis(new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS))) {
      XSSFSheet pointages = classeur.getSheet("Pointages");

      assertThat(pointages.getTables()).isEmpty();
      assertThat(pointages.getLastRowNum()).isZero();
      assertThat(texte(pointages, 0, 9)).isEqualTo("Total (€)");
    }
  }

  private static List<String> textes(Row ligne) {
    return List.of(0, 1, 2, 3)
      .stream()
      .map(colonne -> ligne.getCell(colonne).getStringCellValue())
      .toList();
  }

  private static List<Double> valeursDuPointage(Row ligne) {
    return List.of(6, 7, 8, 9)
      .stream()
      .map(colonne -> ligne.getCell(colonne).getNumericCellValue())
      .toList();
  }

  private static XSSFWorkbook classeur() throws IOException {
    return lis(new CompteRenduDuCout(COUT_DE_REVIENT_FRAISAGE_ET_SANS_POSTE, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS));
  }

  private static XSSFWorkbook lis(CompteRenduDuCout compteRendu) throws IOException {
    return new XSSFWorkbook(new ByteArrayInputStream(ClasseurDuCoutDeRevient.de(compteRendu)));
  }

  private static String texte(XSSFSheet feuille, int ligne, int colonne) {
    return feuille.getRow(ligne).getCell(colonne).getStringCellValue();
  }

  private static List<Double> nombres(Row ligne) {
    return List.of(1, 2, 3, 4, 5, 6)
      .stream()
      .map(colonne -> ligne.getCell(colonne).getNumericCellValue())
      .toList();
  }
}
