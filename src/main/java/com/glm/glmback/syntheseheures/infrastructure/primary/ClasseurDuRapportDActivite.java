package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.DescriptionDElement;
import com.glm.glmback.syntheseheures.domain.DureeTotale;
import com.glm.glmback.syntheseheures.domain.ElementDeLaSynthese;
import com.glm.glmback.syntheseheures.domain.JourDuRapport;
import com.glm.glmback.syntheseheures.domain.LigneDuJournal;
import com.glm.glmback.syntheseheures.domain.NatureDuRapport;
import com.glm.glmback.syntheseheures.domain.RapportDActivite;
import com.glm.glmback.syntheseheures.domain.ReferenceDElement;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
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
 * Le rapport d'activite en classeur Excel, construit avec Apache POI.
 *
 * <p>
 * Une feuille Synthese lisible telle quelle, et une feuille Pointages en tableau plat nomme, source des tableaux
 * croises et des macros. Les durees sont des heures decimales, les jours et les heures de vraies dates Excel dans le
 * fuseau de l'entreprise. Rien n'est recalcule : chaque total est celui du rapport.
 * </p>
 */
final class ClasseurDuRapportDActivite {

  static final MediaType TYPE = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private static final String STYLE_DE_TABLEAU = "TableStyleMedium7";
  private static final String FORMAT_HEURES = "0.00;-0.00;\"\"";
  private static final String FORMAT_JOUR = "dd/mm/yyyy";
  private static final String FORMAT_HEURE = "hh:mm";
  private static final String FORMAT_GENERATION = "dd/mm/yyyy hh:mm";
  private static final double SECONDES_PAR_HEURE = 3600;
  private static final int LARGEUR_D_UN_CARACTERE = 256;
  private static final short TAILLE_DU_TITRE = 16;
  private static final String TOTAL = "Total";
  private static final List<String> COLONNES_DES_JOURS = List.of("Date", "Jour", "Temps pointé (h)", "Dont NC (h)", "Mention");
  private static final List<String> COLONNES_DES_ELEMENTS = List.of(
    "Élément",
    "Référence",
    "Description",
    "Catégorie",
    "Temps (h)",
    "Dont NC (h)"
  );
  private static final List<String> COLONNES_DES_NATURES = List.of("Nature de travail", "Temps (h)");
  private static final List<Integer> LARGEURS_DE_LA_SYNTHESE = List.of(24, 26, 26, 18, 12, 12);
  private static final List<String> COLONNES_DES_POINTAGES = List.of(
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
  private static final List<Integer> LARGEURS_DES_POINTAGES = List.of(12, 7, 8, 8, 10, 18, 14, 24, 11, 18, 14, 16, 34);

  private final XSSFWorkbook classeur;
  private final RapportDActivite rapport;
  private final CellStyle titre;
  private final CellStyle libelle;
  private final CellStyle generation;
  private final CellStyle jour;
  private final CellStyle heure;
  private final CellStyle heures;
  private final CellStyle heuresEnGras;

  private ClasseurDuRapportDActivite(XSSFWorkbook classeur, RapportDActivite rapport) {
    this.classeur = classeur;
    this.rapport = rapport;
    Font police = classeur.createFont();
    police.setBold(true);
    Font grandTitre = classeur.createFont();
    grandTitre.setBold(true);
    grandTitre.setFontHeightInPoints(TAILLE_DU_TITRE);
    this.titre = style(grandTitre, null);
    this.libelle = style(police, null);
    this.generation = style(null, FORMAT_GENERATION);
    this.generation.setAlignment(HorizontalAlignment.LEFT);
    this.jour = style(null, FORMAT_JOUR);
    this.heure = style(null, FORMAT_HEURE);
    this.heures = style(null, FORMAT_HEURES);
    this.heuresEnGras = style(police, FORMAT_HEURES);
  }

  static byte[] de(RapportDActivite rapport) throws IOException {
    try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
      ClasseurDuRapportDActivite construction = new ClasseurDuRapportDActivite(classeur, rapport);
      construction.synthese();
      construction.pointages();
      classeur.write(sortie);
      return sortie.toByteArray();
    }
  }

  static String nomDeFichier(RapportDActivite rapport) {
    return LibellesDuRapportDActivite.nomDeFichier(rapport.operateur(), rapport.periode()) + ".xlsx";
  }

  private void synthese() {
    XSSFSheet feuille = classeur.createSheet("Synthèse");
    for (int colonne = 0; colonne < LARGEURS_DE_LA_SYNTHESE.size(); colonne++) {
      feuille.setColumnWidth(colonne, LARGEURS_DE_LA_SYNTHESE.get(colonne) * LARGEUR_D_UN_CARACTERE);
    }
    texte(feuille.createRow(0), 0, LibellesDuRapportDActivite.TITRE).setCellStyle(titre);
    texte(enTete(feuille.createRow(1), "Opérateur"), 1, LibellesDuRapportDActivite.operateur(rapport.operateur())).setCellStyle(libelle);
    texte(enTete(feuille.createRow(2), "Période"), 1, LibellesDuRapportDActivite.periode(rapport.periode()));
    instant(enTete(feuille.createRow(3), "Généré le"), 1, rapport.evaluation()).setCellStyle(generation);

    Row tempsPointe = enTete(feuille.createRow(5), "Temps pointé (h)");
    nombre(tempsPointe, 1, heures(rapport.tempsPointe())).setCellStyle(heuresEnGras);
    texte(tempsPointe, 2, "Jours avec pointage").setCellStyle(libelle);
    nombre(tempsPointe, 3, rapport.joursAvecPointage()).setCellStyle(libelle);
    Row nonConformite = enTete(feuille.createRow(6), "Dont non-conformité (h)");
    nombre(nonConformite, 1, heures(rapport.dureeNonConformite())).setCellStyle(heuresEnGras);
    texte(nonConformite, 2, "Éléments").setCellStyle(libelle);
    nombre(nonConformite, 3, rapport.elements().size()).setCellStyle(libelle);

    int ligne = tableau(feuille, 8, "Jours", COLONNES_DES_JOURS, joursAffiches(), this::jour);
    total(feuille.createRow(ligne + 1), 2, rapport.tempsPointe(), rapport.dureeNonConformite());

    ligne = tableau(feuille, ligne + 3, "Elements", COLONNES_DES_ELEMENTS, rapport.elements(), this::element);
    total(feuille.createRow(ligne + 1), 4, rapport.tempsPointe(), rapport.dureeNonConformite());

    if (rapport.natures().size() > 1) {
      ligne = tableau(feuille, ligne + 3, "Natures", COLONNES_DES_NATURES, rapport.natures(), this::nature);
    }
    texte(feuille.createRow(ligne + 2), 0, LibellesDuRapportDActivite.PRESENCE);
  }

  /** Un mois ne montre pas ses samedis et dimanches sans pointage ; une semaine montre ses sept jours. */
  private List<JourDuRapport> joursAffiches() {
    return rapport
      .jours()
      .stream()
      .filter(
        jourDuRapport -> rapport.periode() instanceof SemaineCalendaire || !jourDuRapport.lignes().isEmpty() || !weekEnd(jourDuRapport)
      )
      .toList();
  }

  private static boolean weekEnd(JourDuRapport jourDuRapport) {
    return jourDuRapport.jour().getDayOfWeek().compareTo(DayOfWeek.SATURDAY) >= 0;
  }

  private void jour(Row valeurs, JourDuRapport jourDuRapport) {
    valeurs.createCell(0).setCellValue(jourDuRapport.jour());
    valeurs.getCell(0).setCellStyle(jour);
    texte(valeurs, 1, LibellesDuRapportDActivite.jourCourt(jourDuRapport.jour()));
    nombre(valeurs, 2, heures(jourDuRapport.duree())).setCellStyle(heures);
    nombre(valeurs, 3, heures(jourDuRapport.dureeNonConformite())).setCellStyle(heures);
    texte(valeurs, 4, jourDuRapport.finAutomatique() ? LibellesDuRapportDActivite.FIN_AUTOMATIQUE : "");
  }

  private void element(Row valeurs, ElementDeLaSynthese element) {
    texte(valeurs, 0, element.element().nom().value());
    texte(valeurs, 1, element.reference().map(ReferenceDElement::value).orElse(""));
    texte(valeurs, 2, element.description().map(DescriptionDElement::value).orElse(""));
    texte(valeurs, 3, element.element().categorie().value());
    nombre(valeurs, 4, heures(element.duree())).setCellStyle(heures);
    nombre(valeurs, 5, heures(element.dureeNonConformite())).setCellStyle(heures);
  }

  private void nature(Row valeurs, NatureDuRapport nature) {
    texte(valeurs, 0, LibellesDuRapportDActivite.nature(nature.nature()));
    nombre(valeurs, 1, heures(nature.duree())).setCellStyle(heures);
  }

  private void total(Row ligne, int colonne, DureeTotale temps, DureeTotale nonConformite) {
    texte(ligne, 0, TOTAL).setCellStyle(libelle);
    nombre(ligne, colonne, heures(temps)).setCellStyle(heuresEnGras);
    nombre(ligne, colonne + 1, heures(nonConformite)).setCellStyle(heuresEnGras);
  }

  /**
   * Un tableau plat, source des tableaux croises et des macros : une ligne par pointage, aucune ligne intercalaire ni
   * cellule fusionnee. Additionnees, ses durees donnent le temps pointe de la synthese.
   */
  private void pointages() {
    XSSFSheet feuille = classeur.createSheet("Pointages");
    for (int colonne = 0; colonne < LARGEURS_DES_POINTAGES.size(); colonne++) {
      feuille.setColumnWidth(colonne, LARGEURS_DES_POINTAGES.get(colonne) * LARGEUR_D_UN_CARACTERE);
    }
    List<LignePointee> lignes = rapport
      .jours()
      .stream()
      .flatMap(jourDuRapport ->
        jourDuRapport
          .lignes()
          .stream()
          .map(ligne -> new LignePointee(jourDuRapport, ligne))
      )
      .toList();
    tableau(feuille, 0, "Pointages", COLONNES_DES_POINTAGES, lignes, this::pointage);
    feuille.createFreezePane(0, 1);
  }

  private void pointage(Row valeurs, LignePointee pointee) {
    LigneDuJournal ligne = pointee.ligne();
    valeurs.createCell(0).setCellValue(pointee.jour().jour());
    valeurs.getCell(0).setCellStyle(jour);
    texte(valeurs, 1, LibellesDuRapportDActivite.jourCourt(pointee.jour().jour()));
    instant(valeurs, 2, ligne.plage().debut()).setCellStyle(heure);
    ligne
      .plage()
      .fin()
      .ifPresentOrElse(fin -> instant(valeurs, 3, fin).setCellStyle(heure), () -> valeurs.createCell(3));
    nombre(valeurs, 4, heures(ligne.duree())).setCellStyle(heures);
    ElementDeLaSynthese element = rapport
      .elements()
      .stream()
      .filter(candidat -> candidat.element().equals(ligne.element()))
      .findFirst()
      .orElseThrow();
    texte(valeurs, 5, element.element().nom().value());
    texte(valeurs, 6, element.reference().map(ReferenceDElement::value).orElse(""));
    texte(valeurs, 7, element.description().map(DescriptionDElement::value).orElse(""));
    texte(valeurs, 8, element.element().categorie().value());
    texte(valeurs, 9, LibellesDuRapportDActivite.poste(ligne.poste()));
    texte(valeurs, 10, LibellesDuRapportDActivite.nature(ligne.nature()));
    texte(valeurs, 11, LibellesDuRapportDActivite.categorie(ligne.categorie()));
    texte(valeurs, 12, String.join(", ", LibellesDuRapportDActivite.mentions(ligne)));
  }

  private record LignePointee(JourDuRapport jour, LigneDuJournal ligne) {}

  /** L'en-tete a la ligne donnee, puis une ligne par valeur ; rend l'indice de la derniere ligne ecrite. */
  private <T> int tableau(XSSFSheet feuille, int entete, String nom, List<String> colonnes, List<T> valeurs, BiConsumer<Row, T> ecris) {
    Row ligneDEntete = feuille.createRow(entete);
    for (int colonne = 0; colonne < colonnes.size(); colonne++) {
      texte(ligneDEntete, colonne, colonnes.get(colonne));
    }
    int ligne = entete;
    for (T valeur : valeurs) {
      ligne++;
      ecris.accept(feuille.createRow(ligne), valeur);
    }
    if (ligne > entete) {
      nomme(feuille, nom, entete, ligne, colonnes.size());
    }
    return ligne;
  }

  private static void nomme(XSSFSheet feuille, String nom, int entete, int derniere, int colonnes) {
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
    Cell cellule = ligne.createCell(colonne);
    cellule.setCellValue(valeur);
    return cellule;
  }

  private static Cell nombre(Row ligne, int colonne, double valeur) {
    Cell cellule = ligne.createCell(colonne);
    cellule.setCellValue(valeur);
    return cellule;
  }

  private Cell instant(Row ligne, int colonne, Instant valeur) {
    Cell cellule = ligne.createCell(colonne);
    cellule.setCellValue(LocalDateTime.ofInstant(valeur, rapport.fuseau()));
    return cellule;
  }

  private static double heures(DureeTotale duree) {
    return duree.valeur().toMillis() / (SECONDES_PAR_HEURE * 1000);
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
