package com.glm.glmback.syntheseheures.infrastructure.primary;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.syntheseheures.domain.RapportDActivite;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Le classeur de mai 2026, relu avec POI : cellules, types et formats. */
@UnitTest
class ClasseurDuRapportDActiviteTest {

  private static final String FORMAT_HEURES = "0.00;-0.00;\"\"";

  @Test
  void shouldNommerLeFichierDApresLOperateurEtLaPeriode() {
    assertThat(ClasseurDuRapportDActivite.nomDeFichier(rapportDActiviteDeMai2026())).isEqualTo("activite-dupont-jean-2026-05.xlsx");
    assertThat(ClasseurDuRapportDActivite.nomDeFichier(rapportDActiviteDeLaSemaine20())).isEqualTo("activite-dupont-jean-S20-2026.xlsx");
  }

  @Test
  void shouldPorterLEnTeteEtLesChiffresCles() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteDeMai2026())) {
      XSSFSheet synthese = classeur.getSheet("Synthèse");
      assertThat(classeur.getSheetName(0)).isEqualTo("Synthèse");
      assertThat(texte(synthese, 0, 0)).isEqualTo("Rapport d'activité");
      assertThat(texte(synthese, 1, 1)).isEqualTo("Jean Dupont");
      assertThat(texte(synthese, 2, 1)).isEqualTo("Mai 2026");
      assertThat(synthese.getRow(3).getCell(1).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 5, 13, 12, 0));
      assertThat(nombre(synthese, 5, 1)).isEqualTo(16.0);
      assertThat(nombre(synthese, 5, 3)).isEqualTo(3.0);
      assertThat(nombre(synthese, 6, 1)).isEqualTo(1.0);
      assertThat(nombre(synthese, 6, 3)).isEqualTo(2.0);
    }
  }

  @Test
  void shouldOmettreDuMoisLesWeekEndsSansPointage() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteDeMai2026())) {
      XSSFTable jours = classeur.getTable("Jours");
      XSSFSheet synthese = jours.getXSSFSheet();
      assertThat(jours.getEndRowIndex() - jours.getStartRowIndex()).isEqualTo(21);
      Row lundi = synthese.getRow(jours.getStartRowIndex() + 7);
      assertThat(lundi.getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LUNDI_11_MAI_2026);
      assertThat(lundi.getCell(1).getStringCellValue()).isEqualTo("lun.");
      assertThat(lundi.getCell(2).getNumericCellValue()).isEqualTo(5.0);
      assertThat(lundi.getCell(2).getCellStyle().getDataFormatString()).isEqualTo(FORMAT_HEURES);
      assertThat(lundi.getCell(3).getNumericCellValue()).isEqualTo(1.0);
      assertThat(synthese.getRow(jours.getStartRowIndex() + 8).getCell(4).getStringCellValue()).isEqualTo("fin automatique");
      Row total = synthese.getRow(jours.getEndRowIndex() + 1);
      assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
      assertThat(total.getCell(2).getNumericCellValue()).isEqualTo(16.0);
      assertThat(classeur.getFontAt(total.getCell(2).getCellStyle().getFontIndex()).getBold()).isTrue();
    }
  }

  @Test
  void shouldGarderLesSeptJoursDUneSemaine() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteDeLaSemaine20())) {
      XSSFTable jours = classeur.getTable("Jours");
      assertThat(jours.getEndRowIndex() - jours.getStartRowIndex()).isEqualTo(7);
      assertThat(texte(classeur.getSheet("Synthèse"), 2, 1)).isEqualTo("Semaine 20 · du 11 au 17 mai 2026");
    }
  }

  @Test
  void shouldPorterLesElementsEtLesNatures() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteDeMai2026())) {
      assertThat(lignes(classeur.getTable("Elements"))).containsExactly(
        List.of("Élément", "Référence", "Description", "Catégorie", "Temps (h)", "Dont NC (h)"),
        List.of("PRD-2026-000015", "1015", "Carter de pompe", "MOULE", "13.0", "1.0"),
        List.of("OF-2026-000007", "", "", "OF", "3.0", "0.0")
      );
      assertThat(lignes(classeur.getTable("Natures"))).containsExactly(
        List.of("Nature de travail", "Temps (h)"),
        List.of("Fraisage", "15.0"),
        List.of("Sans poste", "1.0"),
        List.of("Tournage", "0.0")
      );
      assertThat(texte(classeur.getSheet("Synthèse"), classeur.getTable("Natures").getEndRowIndex() + 2, 0)).isEqualTo(
        "Le temps pointé mesure le temps passé sur les éléments, pas la présence."
      );
    }
  }

  @Test
  void shouldTairePourUneSeuleNatureLeTableauDesNatures() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteDeLaSemaine20())) {
      assertThat(classeur.getTable("Natures")).isNull();
    }
  }

  @Test
  void shouldPorterUnPointageParLigneDansUnTableauPlatNomme() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteDeMai2026())) {
      XSSFTable pointages = classeur.getTable("Pointages");
      XSSFSheet feuille = pointages.getXSSFSheet();
      assertThat(classeur.getSheetName(1)).isEqualTo("Pointages");
      assertThat(lignes(pointages).getFirst()).containsExactly(
        "Date",
        "Jour",
        "Début",
        "Fin",
        "Durée (h)",
        "Élément",
        "Référence",
        "Description",
        "Catégorie",
        "Poste",
        "Nature",
        "Activité",
        "Mention"
      );
      assertThat(pointages.getEndRowIndex()).isEqualTo(6);
      assertThat(feuille.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
      assertThat(feuille.getNumMergedRegions()).isZero();

      Row premier = feuille.getRow(1);
      assertThat(premier.getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LUNDI_11_MAI_2026);
      assertThat(premier.getCell(2).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 5, 11, 8, 0));
      assertThat(premier.getCell(2).getCellStyle().getDataFormatString()).isEqualTo("hh:mm");
      assertThat(premier.getCell(3).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 5, 11, 10, 0));
      assertThat(lignes(pointages).get(1).subList(4, 13)).containsExactly(
        "2.0",
        "PRD-2026-000015",
        "1015",
        "Carter de pompe",
        "MOULE",
        "DMU 50",
        "Fraisage",
        "Travail",
        "en même temps qu'un autre pointage"
      );
      assertThat(lignes(pointages).get(3).subList(11, 13)).containsExactly("Non-conformité", "en même temps qu'un autre pointage");
      assertThat(lignes(pointages).get(4).get(12)).isEqualTo("fin automatique");
      assertThat(lignes(pointages).get(5).subList(9, 11)).containsExactly("Sans poste", "Sans poste");
      Row enCours = feuille.getRow(6);
      assertThat(enCours.getCell(3).getCellType()).isEqualTo(CellType.BLANK);
      assertThat(enCours.getCell(4).getNumericCellValue()).isZero();
      assertThat(enCours.getCell(12).getStringCellValue()).isEqualTo("en cours");
    }
  }

  @Test
  void shouldGarderLEnTeteDesTableauxSansLigneSansLesNommer() throws IOException {
    try (XSSFWorkbook classeur = lis(rapportDActiviteVideDeLaSemaine19())) {
      assertThat(classeur.getTable("Elements")).isNull();
      assertThat(classeur.getTable("Pointages")).isNull();
      assertThat(texte(classeur.getSheet("Pointages"), 0, 0)).isEqualTo("Date");
      assertThat(nombre(classeur.getSheet("Synthèse"), 5, 1)).isZero();
    }
  }

  private static XSSFWorkbook lis(RapportDActivite rapport) throws IOException {
    return new XSSFWorkbook(new ByteArrayInputStream(ClasseurDuRapportDActivite.de(rapport)));
  }

  private static String texte(XSSFSheet feuille, int ligne, int colonne) {
    return feuille.getRow(ligne).getCell(colonne).getStringCellValue();
  }

  private static double nombre(XSSFSheet feuille, int ligne, int colonne) {
    return feuille.getRow(ligne).getCell(colonne).getNumericCellValue();
  }

  /** Les cellules d'un tableau, en texte : les nombres par leur valeur, les dates non comprises. */
  private static List<List<String>> lignes(XSSFTable tableau) {
    List<List<String>> lignes = new ArrayList<>();
    for (int ligne = tableau.getStartRowIndex(); ligne <= tableau.getEndRowIndex(); ligne++) {
      Row valeurs = tableau.getXSSFSheet().getRow(ligne);
      List<String> cellules = new ArrayList<>();
      for (int colonne = tableau.getStartColIndex(); colonne <= tableau.getEndColIndex(); colonne++) {
        var cellule = valeurs.getCell(colonne);
        cellules.add(
          cellule.getCellType() == CellType.NUMERIC ? String.valueOf(cellule.getNumericCellValue()) : cellule.getStringCellValue()
        );
      }
      lignes.add(cellules);
    }
    return lignes;
  }
}
