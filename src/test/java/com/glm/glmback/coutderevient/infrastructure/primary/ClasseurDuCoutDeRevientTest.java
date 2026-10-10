package com.glm.glmback.coutderevient.infrastructure.primary;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
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
    assertThat(ClasseurDuCoutDeRevient.nomDeFichier(new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, FUSEAU_DE_PARIS))).isEqualTo(
      "cout-de-revient-OF-2026-000001.xlsx"
    );
  }

  @Test
  void shouldPresenterLElementEtLInstantDeGenerationDansLeFuseauDeLEntreprise() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      XSSFSheet synthese = classeur.getSheet("Synthèse");

      assertThat(texte(synthese, 0, 0)).isEqualTo("Coût de revient");
      assertThat(texte(synthese, 1, 0)).isEqualTo("Élément");
      assertThat(texte(synthese, 1, 1)).isEqualTo("OF · OF-2026-000001");
      assertThat(texte(synthese, 2, 0)).isEqualTo("Généré le");
      Cell generation = synthese.getRow(2).getCell(1);
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
      assertThat(natures.getArea().formatAsString()).isEqualTo("A5:G7");
      assertThat(natures.getStyleName()).isEqualTo("TableStyleMedium2");
      assertThat(natures.getCTTable().getAutoFilter().getRef()).isEqualTo("A5:G7");
      assertThat(
        natures
          .getColumns()
          .stream()
          .map(colonne -> colonne.getName())
          .toList()
      ).containsExactly("Nature", "Travail (h)", "Non-conformité (h)", "Temps total (h)", "Machine (€)", "Main d’œuvre (€)", "Total (€)");
      assertThat(texte(synthese, 5, 0)).isEqualTo("Fraisage");
      assertThat(nombres(synthese.getRow(5))).containsExactly(2.0, 1.0, 3.0, 135.0, 60.0, 195.0);
      assertThat(texte(synthese, 6, 0)).isEqualTo("Sans poste");
      assertThat(nombres(synthese.getRow(6))).containsExactly(1.0, 0.0, 1.0, 0.0, 20.0, 20.0);
      assertThat(synthese.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 5);
    }
  }

  @Test
  void shouldEcrireLesMontantsEnEurosEtMasquerUnCoutNul() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      Row sansPoste = classeur.getSheet("Synthèse").getRow(6);

      assertThat(sansPoste.getCell(1).getCellStyle().getDataFormatString()).isEqualTo("0.00");
      assertThat(sansPoste.getCell(4).getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(sansPoste.getCell(4).getCellStyle().getDataFormatString()).isEqualTo(FORMAT_EURO);
      assertThat(sansPoste.getCell(6).getCellStyle().getDataFormatString()).isEqualTo(FORMAT_EURO);
    }
  }

  @Test
  void shouldTotaliserCommeLEcranSousLeTableau() throws IOException {
    try (XSSFWorkbook classeur = classeur()) {
      Row total = classeur.getSheet("Synthèse").getRow(7);

      assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
      assertThat(nombres(total)).containsExactly(3.0, 1.0, 4.0, 135.0, 80.0, 215.0);
      assertThat(classeur.getFontAt(total.getCell(6).getCellStyle().getFontIndex()).getBold()).isTrue();
    }
  }

  @Test
  void shouldNeDresserAucunTableauPourUnRapportVide() throws IOException {
    try (XSSFWorkbook classeur = lis(new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, FUSEAU_DE_PARIS))) {
      XSSFSheet synthese = classeur.getSheet("Synthèse");

      assertThat(synthese.getTables()).isEmpty();
      assertThat(texte(synthese, 5, 0)).isEqualTo("Total");
      assertThat(nombres(synthese.getRow(5))).containsExactly(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
  }

  private static XSSFWorkbook classeur() throws IOException {
    return lis(new CompteRenduDuCout(COUT_DE_REVIENT_FRAISAGE_ET_SANS_POSTE, FUSEAU_DE_PARIS));
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
