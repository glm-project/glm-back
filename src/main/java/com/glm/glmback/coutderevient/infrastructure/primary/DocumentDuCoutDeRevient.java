package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.DureeTotale;
import com.glm.glmback.coutderevient.domain.MontantTotal;
import com.glm.glmback.coutderevient.domain.TempsPasse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
  private static final TemplateEngine MOTEUR = moteur();

  private DocumentDuCoutDeRevient() {}

  static byte[] de(CompteRenduDuCout compteRendu, VersionDuDocument version) throws IOException {
    Context contexte = new Context(Locale.FRENCH);
    contexte.setVariable("document", vue(compteRendu, version));
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
      LigneVue.de("Total", rapport.temps(), rapport.cout().machine(), rapport.cout().mainDOeuvre(), rapport.cout().total())
    );
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

  /** Ce que le gabarit affiche, deja formate : il ne fait que placer des textes. */
  public record Vue(String titre, String element, String statut, String generation, List<LigneVue> natures, LigneVue total) {}

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
