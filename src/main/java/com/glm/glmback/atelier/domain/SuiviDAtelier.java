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
   * echue a l'instant present. La fin ne vient pas du futur, ne precede pas le debut de l'activite et ne depasse pas sa
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
    if (fin.isBefore(activite.debut())) {
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

  public List<Activite> activites() {
    return journal.activites(cloture.map(Cloture::dateDeSurvenue));
  }

  public List<SequenceEnConflit> conflits() {
    return journal.conflits(cloture.map(Cloture::dateDeSurvenue));
  }

  public List<ActiviteEnCours> activitesEnCours(Instant evaluation) {
    return activites()
      .stream()
      .filter(activite -> activite.estEnCoursA(evaluation))
      .map(ActiviteEnCours::new)
      .toList();
  }

  /**
   * L'etat a l'instant d'evaluation, juge sur les seules activites interpretables : une activite a resoudre n'est pas
   * en cours, et la sequence en conflit se lit a part, sans etat qui lui soit propre.
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
      .anyMatch(evenement -> evenement.estUneRegularisation() && evenement.activiteVisee().filter(id::equals).isPresent());
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
