package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.CategorieDActivite;
import com.glm.glmback.syntheseheures.domain.DescriptionDElement;
import com.glm.glmback.syntheseheures.domain.ElementDeLaSynthese;
import com.glm.glmback.syntheseheures.domain.FormatDuLogo;
import com.glm.glmback.syntheseheures.domain.JourDuRapport;
import com.glm.glmback.syntheseheures.domain.LigneDuJournal;
import com.glm.glmback.syntheseheures.domain.LogoDeLEntreprise;
import com.glm.glmback.syntheseheures.domain.NatureDuRapport;
import com.glm.glmback.syntheseheures.domain.RapportDActivite;
import com.glm.glmback.syntheseheures.domain.ReferenceDElement;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Le rapport d'activite en PDF, A4 portrait, genere par le serveur comme le compte rendu du cout de revient.
 *
 * <p>
 * Un gabarit HTML Thymeleaf met en page des valeurs deja formatees ici ; OpenHTMLtoPDF le rend avec des polices
 * embarquees. Rien n'est recalcule : chaque duree est celle du rapport, et une duree nulle reste une case vide.
 * </p>
 */
final class DocumentDuRapportDActivite {

  static final MediaType TYPE = MediaType.APPLICATION_PDF;

  private static final String GABARIT = "rapport-d-activite";
  private static final String POLICE = "DejaVu Sans";
  private static final Locale FRANCAIS = Locale.FRENCH;
  private static final DateTimeFormatter GENERATION = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", FRANCAIS);
  private static final DateTimeFormatter DATE_COURTE = DateTimeFormatter.ofPattern("dd/MM", FRANCAIS);
  private static final DateTimeFormatter JOUR_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", FRANCAIS);
  private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", FRANCAIS);
  private static final int POURCENT = 100;
  private static final TemplateEngine MOTEUR = moteur();

  private DocumentDuRapportDActivite() {}

  static byte[] de(RapportDActivite rapport, VersionDuRapport version, Optional<LogoDeLEntreprise> logo) throws IOException {
    Context contexte = new Context(FRANCAIS);
    contexte.setVariable("document", vue(rapport, version));
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

  static String nomDeFichier(RapportDActivite rapport, VersionDuRapport version) {
    return "%s-%s.pdf".formatted(LibellesDuRapportDActivite.nomDeFichier(rapport.operateur(), rapport.periode()), version.code());
  }

  static Vue vue(RapportDActivite rapport, VersionDuRapport version) {
    Duration tempsPointe = rapport.tempsPointe().valeur();
    long pointages = rapport.journal().size();
    return new Vue(
      version.titre(),
      LibellesDuRapportDActivite.operateur(rapport.operateur()),
      LibellesDuRapportDActivite.periode(rapport.periode()),
      GENERATION.format(rapport.evaluation().atZone(rapport.fuseau())),
      dureeOuZero(tempsPointe),
      String.format(FRANCAIS, "%.2f h", heures(tempsPointe)),
      String.valueOf(rapport.joursAvecPointage()),
      pointages + (pointages > 1 ? " pointages" : " pointage"),
      rapport.natures().size() + (rapport.natures().size() > 1 ? " natures de travail" : " nature de travail"),
      dureeOuZero(rapport.dureeNonConformite().valeur()),
      part(rapport.dureeNonConformite().valeur(), tempsPointe) + " du temps pointé",
      semaines(rapport),
      rapport.elements().stream().map(ElementVue::de).toList(),
      rapport.journal().stream().anyMatch(LigneDuJournal::simultanee),
      rapport.natures().size() > 1
        ? rapport
            .natures()
            .stream()
            .map(nature -> NatureVue.de(nature, tempsPointe))
            .toList()
        : List.of(),
      version == VersionDuRapport.DETAIL ? journal(rapport) : List.of(),
      rapport.finsAutomatiques() > 0
    );
  }

  /** Les semaines du calendrier, du lundi au dimanche : les jours hors de la periode restent des cases vides. */
  private static List<SemaineVue> semaines(RapportDActivite rapport) {
    Map<LocalDate, JourDuRapport> jours = rapport.jours().stream().collect(Collectors.toMap(JourDuRapport::jour, Function.identity()));
    LocalDate premier = rapport.periode().premierJour().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate dernier = rapport.periode().dernierJour();
    List<SemaineVue> semaines = new ArrayList<>();
    for (LocalDate lundi = premier; !lundi.isAfter(dernier); lundi = lundi.plusWeeks(1)) {
      List<CaseVue> cases = Stream.iterate(lundi, jour -> jour.plusDays(1))
        .limit(DayOfWeek.values().length)
        .map(jour ->
          Optional.ofNullable(jours.get(jour))
            .map(CaseVue::de)
            .orElseGet(() -> CaseVue.horsPeriode(jour))
        )
        .toList();
      Duration total = Stream.iterate(lundi, jour -> jour.plusDays(1))
        .limit(DayOfWeek.values().length)
        .flatMap(jour -> Optional.ofNullable(jours.get(jour)).stream())
        .map(jour -> jour.duree().valeur())
        .reduce(Duration.ZERO, Duration::plus);
      semaines.add(new SemaineVue("S" + lundi.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), dureeOuZero(total), cases));
    }
    return List.copyOf(semaines);
  }

  private static List<JourVue> journal(RapportDActivite rapport) {
    return rapport
      .jours()
      .stream()
      .filter(jour -> !jour.lignes().isEmpty())
      .map(jour ->
        new JourVue(
          majuscule(JOUR_LONG.format(jour.jour())),
          duree(jour.duree().valeur()),
          jour
            .lignes()
            .stream()
            .map(ligne -> PointageVue.de(ligne, rapport.fuseau()))
            .toList()
        )
      )
      .toList();
  }

  /** Une duree nulle n'est pas affichee : la case reste vide. */
  static String duree(Duration duree) {
    if (duree.isZero()) {
      return "";
    }
    return dureeOuZero(duree);
  }

  private static String dureeOuZero(Duration duree) {
    long minutes = duree.toMinutes();
    return "%d h %02d".formatted(minutes / 60, minutes % 60);
  }

  private static double heures(Duration duree) {
    return duree.toMinutes() / 60.0;
  }

  private static String part(Duration partie, Duration total) {
    if (total.isZero()) {
      return "0 %";
    }
    return Math.round((partie.toSeconds() * (double) POURCENT) / total.toSeconds()) + " %";
  }

  private static String majuscule(String texte) {
    return texte.substring(0, 1).toUpperCase(FRANCAIS) + texte.substring(1);
  }

  private static InputStream police(String fichier) {
    return DocumentDuRapportDActivite.class.getResourceAsStream("/fonts/" + fichier);
  }

  private static TemplateEngine moteur() {
    ClassLoaderTemplateResolver resolveur = new ClassLoaderTemplateResolver();
    resolveur.setPrefix("templates/syntheseheures/");
    resolveur.setSuffix(".html");
    resolveur.setTemplateMode(TemplateMode.HTML);
    resolveur.setCharacterEncoding("UTF-8");
    TemplateEngine moteur = new TemplateEngine();
    moteur.setTemplateResolver(resolveur);
    return moteur;
  }

  /** Ce que le gabarit affiche, deja formate : il ne fait que placer des textes. */
  public record Vue(
    String titre,
    String operateur,
    String periode,
    String generation,
    String tempsPointe,
    String tempsPointeEnHeures,
    String joursAvecPointage,
    String pointages,
    String nombreDeNatures,
    String nonConformite,
    String partDeNonConformite,
    List<SemaineVue> semaines,
    List<ElementVue> elements,
    boolean simultanee,
    List<NatureVue> natures,
    List<JourVue> journal,
    boolean finAutomatique
  ) {}

  public record SemaineVue(String libelle, String total, List<CaseVue> cases) {}

  public record CaseVue(
    String date,
    String temps,
    boolean vide,
    String nonConformite,
    boolean finAutomatique,
    boolean simultanee,
    boolean weekEnd,
    boolean horsPeriode
  ) {
    static CaseVue de(JourDuRapport jour) {
      Duration duree = jour.duree().valeur();
      Duration nonConformite = jour.dureeNonConformite().valeur();
      return new CaseVue(
        DATE_COURTE.format(jour.jour()),
        duree.isZero() ? "—" : dureeOuZero(duree),
        duree.isZero(),
        nonConformite.isZero() ? "" : "dont NC " + dureeOuZero(nonConformite),
        jour.finAutomatique(),
        jour.simultanee(),
        weekEnd(jour.jour()),
        false
      );
    }

    static CaseVue horsPeriode(LocalDate jour) {
      return new CaseVue(DATE_COURTE.format(jour), "", true, "", false, false, weekEnd(jour), true);
    }

    private static boolean weekEnd(LocalDate jour) {
      return jour.getDayOfWeek().compareTo(DayOfWeek.SATURDAY) >= 0;
    }
  }

  public record ElementVue(
    String nom,
    String reference,
    String description,
    String categorie,
    String postes,
    String nonConformite,
    String temps
  ) {
    static ElementVue de(ElementDeLaSynthese element) {
      return new ElementVue(
        element.element().nom().value(),
        element.reference().map(ReferenceDElement::value).orElse(""),
        element.description().map(DescriptionDElement::value).orElse(""),
        element.element().categorie().value(),
        element
          .postes()
          .stream()
          .map(poste -> poste.poste().libelle().value())
          .distinct()
          .collect(Collectors.joining(", ")),
        duree(element.dureeNonConformite().valeur()),
        dureeOuZero(element.duree().valeur())
      );
    }
  }

  public record NatureVue(String libelle, String temps, String part) {
    static NatureVue de(NatureDuRapport nature, Duration tempsPointe) {
      return new NatureVue(
        LibellesDuRapportDActivite.nature(nature.nature()),
        dureeOuZero(nature.duree().valeur()),
        DocumentDuRapportDActivite.part(nature.duree().valeur(), tempsPointe)
      );
    }
  }

  public record JourVue(String libelle, String total, List<PointageVue> pointages) {}

  public record PointageVue(
    String debut,
    String fin,
    String element,
    String poste,
    String nature,
    String duree,
    boolean nonConformite,
    List<String> mentions
  ) {
    static PointageVue de(LigneDuJournal ligne, ZoneId fuseau) {
      return new PointageVue(
        heure(ligne.plage().debut(), fuseau),
        ligne
          .plage()
          .fin()
          .map(fin -> heure(fin, fuseau))
          .orElse(""),
        ligne.element().nom().value(),
        LibellesDuRapportDActivite.poste(ligne.poste()),
        LibellesDuRapportDActivite.nature(ligne.nature()),
        DocumentDuRapportDActivite.duree(ligne.duree().valeur()),
        ligne.categorie() == CategorieDActivite.NON_CONFORMITE,
        LibellesDuRapportDActivite.mentions(ligne)
      );
    }

    private static String heure(Instant instant, ZoneId fuseau) {
      return HEURE.format(instant.atZone(fuseau));
    }
  }

  /**
   * Le logo en data URI, a la taille qui le fait tenir dans sa case de l'en-tete sans le deformer. Une image illisible
   * n'est pas affichee.
   */
  public record LogoVue(String source, String largeur, String hauteur) {
    private static final double LARGEUR_DE_LA_CASE = 46;
    private static final double HAUTEUR_DE_LA_CASE = 14;

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
}
