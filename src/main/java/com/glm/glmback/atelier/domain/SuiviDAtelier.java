package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
 * en cours, les intervalles — se lit a un instant d'evaluation explicite, que l'appelant fournit : une activite que
 * rien n'a terminee y est terminee automatiquement des que son echeance est atteinte.
 * </p>
 *
 * <p>
 * Ses intervalles sont le temps effectif de l'element, que rend {@link TempsDAtelierService} : chaque activite est
 * bornee par ses faits et son echeance.
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
   * Refuse un geste qui vise une activite qu'aucun pointage de ce suivi n'a ouverte, ou celle d'un autre operateur ou
   * d'un autre poste. Le refus precede toute autre decision sur le geste, absorption comprise.
   */
  public void exigeLActiviteViseePar(EvenementDAtelier geste) {
    journal.exigeLActiviteViseePar(geste);
  }

  public List<Activite> activites() {
    return journal.activites(cloture.map(Cloture::dateDeSurvenue));
  }

  public List<SequenceEnConflit> conflits() {
    return journal.conflits(cloture.map(Cloture::dateDeSurvenue));
  }

  public List<IntervalleDActivite> intervalles(Instant evaluation) {
    return activites()
      .stream()
      .map(activite -> activite.a(evaluation))
      .toList();
  }

  public List<ActiviteEnCours> activitesEnCours(Instant evaluation) {
    return activites()
      .stream()
      .filter(activite -> activite.estEnCoursA(evaluation))
      .map(ActiviteEnCours::new)
      .toList();
  }

  /**
   * Vrai si le suivi est cloture avant la survenue de l'evenement : la cloture a deja termine ce que l'evenement
   * pretendrait terminer.
   */
  public boolean estClotureAvant(EvenementDAtelier evenement) {
    return cloture.filter(fin -> evenement.dateDeSurvenue().isAfter(fin.dateDeSurvenue())).isPresent();
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
