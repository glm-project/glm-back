package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Le suivi en atelier d'un element engage : son journal, et ce que ce journal permet de deduire.
 *
 * <p>
 * Pointage et regularisation sont le meme acte du domaine, {@link #enregistre(EvenementDAtelier)} : ils ne different
 * que par la provenance de la date de survenue, decidee en amont.
 * </p>
 *
 * <p>
 * Ses activites ne dependent que du journal. Tout ce qui depend de l'heure a laquelle on lit — l'etat, les activites
 * en cours — se lit a un instant d'evaluation explicite, que l'appelant fournit : une activite que
 * rien n'a terminee y est terminee automatiquement des que son echeance est atteinte.
 * </p>
 */
public record SuiviDAtelier(
  SuiviDAtelierId id,
  ElementEngage element,
  Engagement engagement,
  JournalDAtelier journal,
  Optional<Cloture> cloture,
  RevisionDuSuivi revision
) {
  public SuiviDAtelier {
    Assert.notNull("id", id);
    Assert.notNull("element", element);
    Assert.notNull("engagement", engagement);
    Assert.notNull("journal", journal);
    Assert.notNull("cloture", cloture);
    Assert.notNull("revision", revision);
    valide(engagement, journal, cloture);
  }

  private SuiviDAtelier(SuiviDAtelierBuilder builder) {
    this(builder.id, builder.element, builder.engagement, builder.journal, Optional.empty(), builder.revision);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}. La creation, elle, reste l'affaire de ce domaine.
   */
  public static SuiviDAtelierIdBuilder builder() {
    return relectureBuilder(new RevisionDuSuivi(0));
  }

  public static SuiviDAtelierIdBuilder relectureBuilder(RevisionDuSuivi revision) {
    return new SuiviDAtelierBuilder(revision);
  }

  public SuiviDAtelier enregistre(EvenementDAtelier evenement) {
    return new SuiviDAtelier(id, element, engagement, journal.enregistre(evenement), cloture, revision);
  }

  public SuiviDAtelier cloture(Cloture cloture) {
    return new SuiviDAtelier(id, element, engagement, journal, Optional.of(cloture), revision);
  }

  public SuiviDAtelier annuleLaCloture() {
    return new SuiviDAtelier(id, element, engagement, journal, Optional.empty(), revision);
  }

  /**
   * Juge un pointage de la cle donnee, survenu a cette heure, selon la regle de reception : anterieur au dernier
   * accepte de la cle, echeance de sa derniere activite, puis le tableau. Il est accepte, ou ignore pour l'une des
   * quatre raisons de {@link RaisonDePointageIgnore} ; le suivi ne change pas.
   */
  public VerdictDeReception juge(CleDActivite cle, TypeDEvenementDAtelier type, Instant survenue) {
    return RegleDeReception.juge(journal, cloture.map(Cloture::dateDeSurvenue), cle, type, survenue);
  }

  /**
   * L'activite dont le gestionnaire peut regulariser la fin, a l'heure de fin donnee et a l'instant present.
   *
   * <p>
   * Elle doit exister dans ce suivi, ne pas etre deja regularisee, et etre une fin automatique : sans fin reelle et
   * echue a l'instant present. La fin ne vient pas du futur, est posterieure au debut de l'activite (aucune activite de duree nulle) et ne depasse pas sa
   * borne. Elle peut en revanche depasser l'echeance : c'est ce que la regularisation a de particulier.
   * </p>
   */
  public Activite exigeUneFinRegularisable(ActiviteId id, Instant fin, Instant maintenant) {
    Activite activite = activites()
      .stream()
      .filter(candidate -> candidate.id().equals(id))
      .findFirst()
      .orElseThrow(() -> new ActiviteViseeIntrouvableException(id));
    if (estRegularisee(id)) {
      throw new ActiviteDejaRegulariseeException(id);
    }
    if (!activite.a(maintenant).finAutomatique()) {
      throw new ActiviteNonEchueException(id);
    }
    if (fin.isAfter(maintenant)) {
      throw new DateDeSurvenueFutureException(fin);
    }
    if (!fin.isAfter(activite.debut())) {
      throw new FinAvantDebutException(id, fin, activite.debut());
    }
    borne(activite)
      .filter(fin::isAfter)
      .ifPresent(borne -> {
        throw new FinApresBorneException(id, fin, borne);
      });

    return activite;
  }

  /**
   * Le plus tot de l'ouverture suivante sur la cle de l'activite et de la cloture, ou rien : la fin de l'activite ne
   * peut pas les depasser.
   */
  public Optional<Instant> borneDeFin(ActiviteId id) {
    return borne(
      activites()
        .stream()
        .filter(candidate -> candidate.id().equals(id))
        .findFirst()
        .orElseThrow()
    );
  }

  /**
   * Les gestes qui bornent les activites de la cle, dans l'ordre du journal : ce que la cle a reellement vecu, apres les
   * corrections. Les gestes d'une activite retiree n'y sont plus, ceux d'une activite dont le poste a change y sont
   * sur son nouveau poste, et une correction n'y est jamais.
   */
  public List<EvenementDAtelier> gestesDeLaCle(CleDActivite cle) {
    return activites()
      .stream()
      .filter(activite -> activite.cle().equals(cle))
      .flatMap(Activite::gestes)
      .sorted(JournalDAtelier.PAR_ORDRE_CHRONOLOGIQUE)
      .toList();
  }

  public List<Activite> activites() {
    return journal.activites(cloture.map(Cloture::dateDeSurvenue));
  }

  public List<ActiviteEnCours> activitesEnCours(Instant evaluation) {
    return activites()
      .stream()
      .filter(activite -> activite.estEnCoursA(evaluation))
      .map(ActiviteEnCours::new)
      .toList();
  }

  /**
   * L'etat a l'instant d'evaluation : cloture, en cours si une activite l'est, interrompu si le journal porte un fait,
   * en attente sinon.
   */
  public EtatDAtelier etat(Instant evaluation) {
    if (estCloture()) {
      return EtatDAtelier.CLOTURE;
    }

    if (!activitesEnCours(evaluation).isEmpty()) {
      return EtatDAtelier.EN_COURS;
    }

    return journal.evenements().isEmpty() ? EtatDAtelier.EN_ATTENTE : EtatDAtelier.INTERROMPU;
  }

  private boolean estRegularisee(ActiviteId id) {
    return journal
      .evenements()
      .stream()
      .anyMatch(evenement -> evenement.type() == TypeDEvenementDAtelier.FIN && evenement.activiteVisee().filter(id::equals).isPresent());
  }

  /**
   * L'activite que le gestionnaire peut retirer ou dont il peut changer le poste : une activite de ce suivi, telle que le
   * journal la lit apres les corrections deja faites, et terminee a l'instant present. Une activite retiree est donc
   * introuvable, et une activite que rien n'a terminee avant son echeance est en cours.
   */
  public Activite exigeUneActiviteCorrigeable(ActiviteId id, Instant maintenant) {
    Activite activite = activites()
      .stream()
      .filter(candidate -> candidate.id().equals(id))
      .findFirst()
      .orElseThrow(() -> new ActiviteViseeIntrouvableException(id));
    if (activite.estEnCoursA(maintenant)) {
      throw new ActiviteEnCoursException(id);
    }

    return activite;
  }

  /**
   * Refuse la plage qu'une autre activite de la cle recoupe. Les extremites qui se touchent ne se chevauchent pas. Une
   * activite que rien n'a terminee s'etend jusqu'a son echeance si elle est echue, et sans fin sinon ; celle qu'on
   * deplace, donnee en {@code sauf}, ne se recoupe pas elle-meme.
   */
  private void exigeUnePlageLibre(CleDActivite cle, Instant debut, Instant fin, Optional<ActiviteId> sauf, Instant maintenant) {
    boolean occupee = activites()
      .stream()
      .filter(autre -> autre.cle().equals(cle))
      .filter(autre -> sauf.filter(autre.id()::equals).isEmpty())
      .map(autre -> autre.a(maintenant))
      .anyMatch(autre -> autre.debut().isBefore(fin) && autre.fin().map(debut::isBefore).orElse(true));
    if (occupee) {
      throw new ChevauchementException(cle, debut, fin);
    }
  }

  /**
   * Ajoute au journal une activite oubliee : son ouverture et sa fin, deux faits du gestionnaire sur la meme cle, la fin
   * visant l'activite qu'ouvre l'ouverture (un ecart est un defaut de l'appelant). Verifie dans cet ordre que la fin est
   * posterieure au debut (aucune activite n'a une duree nulle), qu'elle ne vient pas du futur, que l'ouverture ne precede
   * pas l'engagement, que la fin ne suit pas la cloture, puis que la plage est libre sur la cle.
   */
  public SuiviDAtelier ajoute(EvenementDAtelier ouverture, EvenementDAtelier fin, Instant maintenant) {
    exigeUnAjoutCoherent(ouverture, fin);
    ActiviteId activite = ouverture.activite().orElseThrow();
    if (!fin.dateDeSurvenue().isAfter(ouverture.dateDeSurvenue())) {
      throw new FinAvantDebutException(activite, fin.dateDeSurvenue(), ouverture.dateDeSurvenue());
    }
    if (fin.dateDeSurvenue().isAfter(maintenant)) {
      throw new DateDeSurvenueFutureException(fin.dateDeSurvenue());
    }
    exigeApresLEngagement(ouverture);
    exigeAvantLaCloture(fin);
    exigeUnePlageLibre(ouverture.cle(), ouverture.dateDeSurvenue(), fin.dateDeSurvenue(), Optional.empty(), maintenant);

    return enregistre(ouverture).enregistre(fin);
  }

  private static void exigeUnAjoutCoherent(EvenementDAtelier ouverture, EvenementDAtelier fin) {
    boolean coherent =
      ouverture.type().ouvreUneActivite()
      && fin.type() == TypeDEvenementDAtelier.FIN
      && ouverture.activite().equals(fin.activiteVisee())
      && ouverture.cle().equals(fin.cle());
    if (!coherent) {
      throw new EvenementDAtelierIncoherentException(fin.type(), fin.origine());
    }
  }

  /**
   * Retire une activite : un fait de plus au journal, qui ne modifie aucun fait, et qui ne suit pas la cloture.
   */
  public SuiviDAtelier retire(EvenementDAtelier retrait) {
    exigeAvantLaCloture(retrait);

    return enregistre(retrait);
  }

  /**
   * Change le poste de l'activite que vise le changement : un fait de plus au journal, qui ne modifie aucun fait. La
   * plage de l'activite doit etre libre sur le nouveau poste, puis le changement, date du moment de la correction, ne suit
   * pas la cloture.
   */
  public SuiviDAtelier changeLePoste(EvenementDAtelier changement, Instant maintenant) {
    ActiviteId visee = changement.activiteVisee().orElseThrow();
    IntervalleDActivite intervalle = exigeUneActiviteCorrigeable(visee, maintenant).a(maintenant);
    exigeUnePlageLibre(changement.cle(), intervalle.debut(), intervalle.fin().orElseThrow(), Optional.of(visee), maintenant);
    exigeAvantLaCloture(changement);

    return enregistre(changement);
  }

  private void exigeApresLEngagement(EvenementDAtelier evenement) {
    if (evenement.dateDeSurvenue().isBefore(engagement.date())) {
      throw new EvenementAvantEngagementException(evenement);
    }
  }

  private void exigeAvantLaCloture(EvenementDAtelier evenement) {
    if (cloture.filter(fin -> evenement.dateDeSurvenue().isAfter(fin.dateDeSurvenue())).isPresent()) {
      throw new EvenementApresClotureException(evenement);
    }
  }

  private Optional<Instant> borne(Activite activite) {
    Optional<Instant> debutSuivant = activites()
      .stream()
      .filter(candidate -> candidate.cle().equals(activite.cle()))
      .dropWhile(candidate -> !candidate.id().equals(activite.id()))
      .skip(1)
      .findFirst()
      .map(Activite::debut);

    return Stream.concat(debutSuivant.stream(), cloture.map(Cloture::dateDeSurvenue).stream()).min(Comparator.naturalOrder());
  }

  public boolean estCloture() {
    return cloture.isPresent();
  }

  private static void valide(Engagement engagement, JournalDAtelier journal, Optional<Cloture> cloture) {
    journal.evenements().forEach(evenement -> valide(evenement, engagement, cloture));
  }

  private static void valide(EvenementDAtelier evenement, Engagement engagement, Optional<Cloture> cloture) {
    if (evenement.dateDeSurvenue().isBefore(engagement.date())) {
      throw new EvenementAvantEngagementException(evenement);
    }

    if (cloture.filter(fin -> evenement.dateDeSurvenue().isAfter(fin.dateDeSurvenue())).isPresent()) {
      throw new SuiviDAtelierClotureException(evenement);
    }
  }

  private static final class SuiviDAtelierBuilder
    implements SuiviDAtelierIdBuilder, SuiviDAtelierElementBuilder, SuiviDAtelierEngagementBuilder, SuiviDAtelierJournalBuilder
  {

    private final RevisionDuSuivi revision;
    private SuiviDAtelierId id;
    private ElementEngage element;
    private Engagement engagement;
    private JournalDAtelier journal;

    private SuiviDAtelierBuilder(RevisionDuSuivi revision) {
      this.revision = revision;
    }

    @Override
    public SuiviDAtelierElementBuilder id(SuiviDAtelierId id) {
      this.id = id;

      return this;
    }

    @Override
    public SuiviDAtelierEngagementBuilder element(ElementEngage element) {
      this.element = element;

      return this;
    }

    @Override
    public SuiviDAtelierJournalBuilder engagement(Engagement engagement) {
      this.engagement = engagement;

      return this;
    }

    @Override
    public SuiviDAtelier journal(JournalDAtelier journal) {
      this.journal = journal;

      return new SuiviDAtelier(this);
    }
  }

  public interface SuiviDAtelierIdBuilder {
    SuiviDAtelierElementBuilder id(SuiviDAtelierId id);
  }

  public interface SuiviDAtelierElementBuilder {
    SuiviDAtelierEngagementBuilder element(ElementEngage element);
  }

  public interface SuiviDAtelierEngagementBuilder {
    SuiviDAtelierJournalBuilder engagement(Engagement engagement);
  }

  public interface SuiviDAtelierJournalBuilder {
    SuiviDAtelier journal(JournalDAtelier journal);
  }
}
