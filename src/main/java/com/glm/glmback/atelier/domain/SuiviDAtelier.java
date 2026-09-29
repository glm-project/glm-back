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
 * que par la provenance de la date de survenue, decidee en amont. C'est ce qui rend la correction sure, elle repasse
 * par exactement les memes invariants.
 * </p>
 *
 * <p>
 * Ses activites ne dependent que du journal. Tout ce qui depend de l'heure a laquelle on lit — l'etat, les activites
 * en cours, les intervalles — se lit a un instant d'evaluation explicite, que l'appelant fournit : une activite que
 * rien n'a terminee y est terminee automatiquement des que son echeance est atteinte.
 * </p>
 *
 * <p>
 * Ses intervalles sont le temps effectif de l'element, que rend {@link TempsDAtelierService} : aucune presence ne les
 * borne.
 * </p>
 */
public record SuiviDAtelier(
  SuiviDAtelierId id,
  ElementEngage element,
  Engagement engagement,
  JournalDAtelier journal,
  Optional<Cloture> cloture
) {
  public SuiviDAtelier {
    Assert.notNull("id", id);
    Assert.notNull("element", element);
    Assert.notNull("engagement", engagement);
    Assert.notNull("journal", journal);
    Assert.notNull("cloture", cloture);
    valide(engagement, journal, cloture);
  }

  private SuiviDAtelier(SuiviDAtelierBuilder builder) {
    this(builder.id, builder.element, builder.engagement, builder.journal, Optional.empty());
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}. La creation, elle, reste l'affaire de ce domaine.
   */
  public static SuiviDAtelierIdBuilder builder() {
    return new SuiviDAtelierBuilder();
  }

  public SuiviDAtelier enregistre(EvenementDAtelier evenement) {
    return new SuiviDAtelier(id, element, engagement, journal.enregistre(evenement), cloture);
  }

  public SuiviDAtelier annule(EvenementDAtelierId evenement, Annulation annulation) {
    return new SuiviDAtelier(id, element, engagement, journal.annule(evenement, annulation), cloture);
  }

  /**
   * Annule un evenement et lui substitue sa version corrigee, en un seul acte : le remplacant d'un ouvrant garde
   * l'activite qu'il ouvrait, et les gestes qui la visent y restent rattaches.
   */
  public SuiviDAtelier corrige(EvenementDAtelierId evenement, Annulation annulation, EvenementDAtelier remplacant) {
    return new SuiviDAtelier(id, element, engagement, journal.corrige(evenement, annulation, remplacant), cloture);
  }

  public SuiviDAtelier cloture(Cloture cloture) {
    return new SuiviDAtelier(id, element, engagement, journal, Optional.of(cloture));
  }

  public SuiviDAtelier annuleLaCloture() {
    return new SuiviDAtelier(id, element, engagement, journal, Optional.empty());
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

    return journal.actifs().isEmpty() ? EtatDAtelier.EN_ATTENTE : EtatDAtelier.INTERROMPU;
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

    private SuiviDAtelierId id;
    private ElementEngage element;
    private Engagement engagement;
    private JournalDAtelier journal;

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
