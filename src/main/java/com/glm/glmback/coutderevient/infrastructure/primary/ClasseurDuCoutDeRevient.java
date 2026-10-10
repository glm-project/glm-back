package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.DureeTotale;
import com.glm.glmback.coutderevient.domain.LigneDeCout;
import com.glm.glmback.coutderevient.domain.MontantTotal;
import com.glm.glmback.coutderevient.domain.NatureDOperation;
import com.glm.glmback.coutderevient.domain.TempsPasse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;

/**
 * Le compte rendu du cout de revient en classeur Excel, construit avec Apache POI.
 *
 * <p>
 * Le classeur est une source de donnees : montants et durees y sont des nombres, les instants de vraies dates Excel
 * dans le fuseau de l'entreprise, et chaque tableau un tableau Excel nomme. Il ne fait que recopier le rapport : aucun
 * montant n'y est recalcule, le total est celui que l'ecran affiche.
 * </p>
 */
final class ClasseurDuCoutDeRevient {

  static final MediaType TYPE = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  static final String SANS_POSTE = "Sans poste";

  private static final String STYLE_DE_TABLEAU = "TableStyleMedium2";
  private static final String FORMAT_EURO = "#,##0.00 \"€\";-#,##0.00 \"€\";\"\"";
  private static final String FORMAT_HEURES = "0.00";
  private static final String FORMAT_DATE = "dd/mm/yyyy hh:mm";
  private static final double SECONDES_PAR_HEURE = 3600;
  private static final int LARGEUR_D_UN_CARACTERE = 256;
  private static final short TAILLE_DU_TITRE = 16;
  private static final List<String> COLONNES_DES_NATURES = List.of(
    "Nature",
    "Travail (h)",
    "Non-conformité (h)",
    "Temps total (h)",
    "Machine (€)",
    "Main d’œuvre (€)",
    "Total (€)"
  );
  private static final List<Integer> LARGEURS_DES_NATURES = List.of(28, 34, 20, 17, 14, 18, 14);

  private final XSSFWorkbook classeur;
  private final ZoneId fuseau;
  private final CellStyle titre;
  private final CellStyle libelle;
  private final CellStyle date;
  private final CellStyle heures;
  private final CellStyle euros;
  private final CellStyle heuresDuTotal;
  private final CellStyle eurosDuTotal;

  private ClasseurDuCoutDeRevient(XSSFWorkbook classeur, ZoneId fuseau) {
    this.classeur = classeur;
    this.fuseau = fuseau;
    Font gras = classeur.createFont();
    gras.setBold(true);
    Font grandTitre = classeur.createFont();
    grandTitre.setBold(true);
    grandTitre.setFontHeightInPoints(TAILLE_DU_TITRE);
    this.titre = style(grandTitre, null);
    this.libelle = style(gras, null);
    this.date = style(null, FORMAT_DATE);
    this.date.setAlignment(HorizontalAlignment.LEFT);
    this.heures = style(null, FORMAT_HEURES);
    this.euros = style(null, FORMAT_EURO);
    this.heuresDuTotal = style(gras, FORMAT_HEURES);
    this.eurosDuTotal = style(gras, FORMAT_EURO);
  }

  static byte[] de(CompteRenduDuCout compteRendu) throws IOException {
    try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
      new ClasseurDuCoutDeRevient(classeur, compteRendu.fuseau()).synthese(compteRendu.rapport());
      classeur.write(sortie);
      return sortie.toByteArray();
    }
  }

  static String nomDeFichier(CompteRenduDuCout compteRendu) {
    return "cout-de-revient-%s.xlsx".formatted(compteRendu.rapport().element().nom().value());
  }

  private void synthese(CoutDeRevient rapport) {
    XSSFSheet feuille = classeur.createSheet("Synthèse");
    texte(feuille.createRow(0), 0, "Coût de revient").setCellStyle(titre);
    enTete(feuille.createRow(1), "Élément")
      .createCell(1)
      .setCellValue("%s · %s".formatted(rapport.element().categorie().value(), rapport.element().nom().value()));
    instant(enTete(feuille.createRow(2), "Généré le"), 1, rapport.lecture().evaluation()).setCellStyle(date);

    int entete = 4;
    Row colonnes = feuille.createRow(entete);
    for (int colonne = 0; colonne < COLONNES_DES_NATURES.size(); colonne++) {
      texte(colonnes, colonne, COLONNES_DES_NATURES.get(colonne));
      feuille.setColumnWidth(colonne, LARGEURS_DES_NATURES.get(colonne) * LARGEUR_D_UN_CARACTERE);
    }
    int ligne = entete;
    for (LigneDeCout nature : rapport.lignes()) {
      ligne++;
      Row valeurs = feuille.createRow(ligne);
      texte(valeurs, 0, nature(nature));
      ecrisLesValeurs(valeurs, nature.temps(), nature.cout().machine(), nature.cout().mainDOeuvre(), nature.cout().total(), false);
    }
    if (ligne > entete) {
      tableau(feuille, "Natures", entete, ligne, COLONNES_DES_NATURES.size());
    }

    Row total = feuille.createRow(ligne + 1);
    texte(total, 0, "Total").setCellStyle(libelle);
    ecrisLesValeurs(total, rapport.temps(), rapport.cout().machine(), rapport.cout().mainDOeuvre(), rapport.cout().total(), true);
    feuille.createFreezePane(0, entete + 1);
  }

  private void ecrisLesValeurs(
    Row ligne,
    TempsPasse temps,
    MontantTotal machine,
    MontantTotal mainDOeuvre,
    MontantTotal total,
    boolean gras
  ) {
    CellStyle styleDesHeures = gras ? heuresDuTotal : heures;
    CellStyle styleDesEuros = gras ? eurosDuTotal : euros;
    nombre(ligne, 1, heures(temps.travail())).setCellStyle(styleDesHeures);
    nombre(ligne, 2, heures(temps.nonConformite())).setCellStyle(styleDesHeures);
    nombre(ligne, 3, heures(temps.total())).setCellStyle(styleDesHeures);
    nombre(ligne, 4, euros(machine)).setCellStyle(styleDesEuros);
    nombre(ligne, 5, euros(mainDOeuvre)).setCellStyle(styleDesEuros);
    nombre(ligne, 6, euros(total)).setCellStyle(styleDesEuros);
  }

  private static void tableau(XSSFSheet feuille, String nom, int entete, int derniere, int colonnes) {
    AreaReference zone = new AreaReference(
      new CellReference(entete, 0),
      new CellReference(derniere, colonnes - 1),
      feuille.getWorkbook().getSpreadsheetVersion()
    );
    XSSFTable tableau = feuille.createTable(zone);
    tableau.setName(nom);
    tableau.setDisplayName(nom);
    tableau.setStyleName(STYLE_DE_TABLEAU);
    tableau.getCTTable().addNewAutoFilter().setRef(zone.formatAsString());
    tableau.updateHeaders();
  }

  private Row enTete(Row ligne, String libelleDeLigne) {
    texte(ligne, 0, libelleDeLigne).setCellStyle(libelle);
    return ligne;
  }

  private static Cell texte(Row ligne, int colonne, String valeur) {
    var cellule = ligne.createCell(colonne);
    cellule.setCellValue(valeur);
    return cellule;
  }

  private static Cell nombre(Row ligne, int colonne, double valeur) {
    var cellule = ligne.createCell(colonne);
    cellule.setCellValue(valeur);
    return cellule;
  }

  private Cell instant(Row ligne, int colonne, Instant valeur) {
    var cellule = ligne.createCell(colonne);
    cellule.setCellValue(LocalDateTime.ofInstant(valeur, fuseau));
    return cellule;
  }

  private static String nature(LigneDeCout ligne) {
    return ligne.nature().map(NatureDOperation::value).orElse(SANS_POSTE);
  }

  private static double heures(DureeTotale duree) {
    return duree.valeur().toMillis() / (SECONDES_PAR_HEURE * 1000);
  }

  private static double euros(MontantTotal montant) {
    return montant.valeur().value().doubleValue();
  }

  private CellStyle style(Font police, String format) {
    CellStyle style = classeur.createCellStyle();
    if (police != null) {
      style.setFont(police);
    }
    if (format != null) {
      style.setDataFormat(classeur.createDataFormat().getFormat(format));
    }
    return style;
  }
}
