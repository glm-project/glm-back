package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.CategorieDActivite;
import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.DureeTotale;
import com.glm.glmback.coutderevient.domain.FormatDuLogo;
import com.glm.glmback.coutderevient.domain.LogoDeLEntreprise;
import com.glm.glmback.coutderevient.domain.MontantTotal;
import com.glm.glmback.coutderevient.domain.PointageValorise;
import com.glm.glmback.coutderevient.domain.TempsPasse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Le compte rendu du cout de revient en PDF, A4 paysage, genere par le serveur pour un rendu identique partout.
 *
 * <p>
 * Un gabarit HTML Thymeleaf met en page des valeurs deja formatees ici ; OpenHTMLtoPDF le rend avec des polices
 * embarquees. Rien n'est recalcule : chaque montant est celui du rapport, et un cout nul reste une case vide.
 * </p>
 */
final class DocumentDuCoutDeRevient {

  static final MediaType TYPE = MediaType.APPLICATION_PDF;

  private static final String GABARIT = "compte-rendu";
  private static final String POLICE = "DejaVu Sans";
  private static final DateTimeFormatter GENERATION = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH);
  private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);
  private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
  private static final TemplateEngine MOTEUR = moteur();

  private DocumentDuCoutDeRevient() {}

  static byte[] de(CompteRenduDuCout compteRendu, VersionDuDocument version, Optional<LogoDeLEntreprise> logo) throws IOException {
    Context contexte = new Context(Locale.FRENCH);
    contexte.setVariable("document", vue(compteRendu, version));
    contexte.setVariable("logo", logo.isPresent() ? LogoVue.de(logo.orElseThrow()).orElse(null) : null);
    String html = MOTEUR.process(GABARIT, contexte);

    try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
      PdfRendererBuilder rendu = new PdfRendererBuilder();
      rendu.useFastMode();
      rendu.useFont(() -> police("DejaVuSans.ttf"), POLICE, 400, PdfRendererBuilder.FontStyle.NORMAL, true);
      rendu.useFont(() -> police("DejaVuSans-Bold.ttf"), POLICE, 700, PdfRendererBuilder.FontStyle.NORMAL, true);
      rendu.withHtmlContent(html, null);
      rendu.toStream(sortie);
      rendu.run();
      return sortie.toByteArray();
    }
  }

  static String nomDeFichier(CompteRenduDuCout compteRendu, VersionDuDocument version) {
    return "cout-de-revient-%s-%s.pdf".formatted(compteRendu.rapport().element().nom().value(), version.code());
  }

  private static Vue vue(CompteRenduDuCout compteRendu, VersionDuDocument version) {
    CoutDeRevient rapport = compteRendu.rapport();
    ZoneId fuseau = compteRendu.fuseau();
    return new Vue(
      version.titre(),
      "%s · %s".formatted(rapport.element().categorie().value(), rapport.element().nom().value()),
      LibellesDuCoutDeRevient.statut(compteRendu.statut(), fuseau),
      GENERATION.format(rapport.lecture().evaluation().atZone(fuseau)),
      rapport
        .lignes()
        .stream()
        .map(ligne ->
          LigneVue.de(
            LibellesDuCoutDeRevient.nature(ligne),
            ligne.temps(),
            ligne.cout().machine(),
            ligne.cout().mainDOeuvre(),
            ligne.cout().total()
          )
        )
        .toList(),
      LigneVue.de("Total", rapport.temps(), rapport.cout().machine(), rapport.cout().mainDOeuvre(), rapport.cout().total()),
      version == VersionDuDocument.DETAIL ? sections(rapport, fuseau) : List.of()
    );
  }

  /** Une section par nature : ses pointages dans l'ordre ou ils ont commence, puis le total de la nature. */
  private static List<SectionVue> sections(CoutDeRevient rapport, ZoneId fuseau) {
    return rapport
      .lignes()
      .stream()
      .map(ligne -> {
        String nature = LibellesDuCoutDeRevient.nature(ligne);
        int nombre = ligne.pointages().size();
        return new SectionVue(
          "%s — %d %s".formatted(nature, nombre, nombre > 1 ? "pointages" : "pointage"),
          ligne
            .pointages()
            .stream()
            .map(pointage -> PointageVue.de(pointage, rapport.annuaire(), fuseau))
            .toList(),
          new PointageVue(
            "Total " + nature,
            false,
            "",
            "",
            duree(ligne.temps().total().valeur()),
            euros(ligne.cout().machine().valeur().value()),
            euros(ligne.cout().mainDOeuvre().valeur().value()),
            euros(ligne.cout().total().valeur().value())
          )
        );
      })
      .toList();
  }

  /** Le jour n'est repete a la fin que si le pointage a passe minuit. */
  static String periode(Instant debut, Instant fin, ZoneId fuseau) {
    ZonedDateTime depuis = debut.atZone(fuseau);
    ZonedDateTime jusqua = fin.atZone(fuseau);
    String fermeture = depuis.toLocalDate().equals(jusqua.toLocalDate())
      ? HEURE.format(jusqua)
      : JOUR.format(jusqua) + " · " + HEURE.format(jusqua);
    return JOUR.format(depuis) + " · " + HEURE.format(depuis) + " → " + fermeture;
  }

  static String duree(Duration duree) {
    long minutes = duree.toMinutes();
    return "%d h %02d".formatted(minutes / 60, minutes % 60);
  }

  /** Un cout nul n'est pas affiche : la case reste vide. */
  static String euros(BigDecimal montant) {
    if (montant.signum() == 0) {
      return "";
    }
    return String.format(Locale.FRENCH, "%,.2f €", montant);
  }

  private static InputStream police(String fichier) {
    return DocumentDuCoutDeRevient.class.getResourceAsStream("/fonts/" + fichier);
  }

  private static TemplateEngine moteur() {
    ClassLoaderTemplateResolver resolveur = new ClassLoaderTemplateResolver();
    resolveur.setPrefix("templates/coutderevient/");
    resolveur.setSuffix(".html");
    resolveur.setTemplateMode(TemplateMode.HTML);
    resolveur.setCharacterEncoding("UTF-8");
    TemplateEngine moteur = new TemplateEngine();
    moteur.setTemplateResolver(resolveur);
    return moteur;
  }

  /**
   * Le logo en data URI, a la taille qui le fait tenir dans sa case de l'en-tete sans le deformer : la hauteur de la
   * case, sauf pour un logo trop large, borne alors a la largeur de la case. Une image illisible n'est pas affichee.
   */
  public record LogoVue(String source, String largeur, String hauteur) {
    private static final double LARGEUR_DE_LA_CASE = 48;
    private static final double HAUTEUR_DE_LA_CASE = 16;

    static Optional<LogoVue> de(LogoDeLEntreprise logo) throws IOException {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(logo.contenu()));
      if (image == null) {
        return Optional.empty();
      }
      double proportion = (double) image.getWidth() / image.getHeight();
      double hauteur = Math.min(HAUTEUR_DE_LA_CASE, LARGEUR_DE_LA_CASE / proportion);
      return Optional.of(
        new LogoVue(
          "data:%s;base64,%s".formatted(
            logo.format() == FormatDuLogo.PNG ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE,
            Base64.getEncoder().encodeToString(logo.contenu())
          ),
          millimetres(hauteur * proportion),
          millimetres(hauteur)
        )
      );
    }

    private static String millimetres(double valeur) {
      return String.format(Locale.ROOT, "%.2fmm", valeur);
    }
  }

  /** Ce que le gabarit affiche, deja formate : il ne fait que placer des textes. */
  public record Vue(
    String titre,
    String element,
    String statut,
    String generation,
    List<LigneVue> natures,
    LigneVue total,
    List<SectionVue> sections
  ) {}

  public record SectionVue(String titre, List<PointageVue> pointages, PointageVue total) {}

  public record PointageVue(
    String poste,
    boolean nonConformite,
    String operateur,
    String periode,
    String duree,
    String machine,
    String mainDOeuvre,
    String total
  ) {
    static PointageVue de(PointageValorise pointage, AnnuaireDuCout annuaire, ZoneId fuseau) {
      return new PointageVue(
        LibellesDuCoutDeRevient.poste(pointage.activite(), annuaire),
        pointage.activite().categorie() == CategorieDActivite.NON_CONFORMITE,
        LibellesDuCoutDeRevient.operateur(pointage.activite(), annuaire),
        DocumentDuCoutDeRevient.periode(pointage.debut(), pointage.fin(), fuseau),
        DocumentDuCoutDeRevient.duree(pointage.duree().valeur()),
        DocumentDuCoutDeRevient.euros(pointage.cout().machine().valeur().value()),
        DocumentDuCoutDeRevient.euros(pointage.cout().mainDOeuvre().valeur().value()),
        DocumentDuCoutDeRevient.euros(pointage.cout().total().valeur().value())
      );
    }
  }

  public record LigneVue(
    String libelle,
    String travail,
    String nonConformite,
    String temps,
    String machine,
    String mainDOeuvre,
    String total
  ) {
    static LigneVue de(String libelle, TempsPasse temps, MontantTotal machine, MontantTotal mainDOeuvre, MontantTotal total) {
      return new LigneVue(
        libelle,
        duree(temps.travail()),
        duree(temps.nonConformite()),
        duree(temps.total()),
        euros(machine),
        euros(mainDOeuvre),
        euros(total)
      );
    }

    private static String duree(DureeTotale duree) {
      return DocumentDuCoutDeRevient.duree(duree.valeur());
    }

    private static String euros(MontantTotal montant) {
      return DocumentDuCoutDeRevient.euros(montant.valeur().value());
    }
  }
}
