package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
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
 * Les intervalles produits ici sont bruts : ils ignorent les pauses et les departs de l'operateur, qui vivent dans sa
 * journee de travail. Le temps effectif se lit a l'intersection des deux, par {@link TempsDAtelierService}.
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

  static SuiviDAtelierIdBuilder builder() {
    return new SuiviDAtelierBuilder();
  }

  public SuiviDAtelier enregistre(EvenementDAtelier evenement) {
    return new SuiviDAtelier(id, element, engagement, journal.enregistre(evenement), cloture);
  }

  public SuiviDAtelier annule(EvenementDAtelierId evenement, Annulation annulation) {
    return new SuiviDAtelier(id, element, engagement, journal.annule(evenement, annulation), cloture);
  }

  /**
   * Annule un evenement et lui substitue sa version corrigee, en validant la seule sequence finale.
   *
   * <p>
   * Enchainer une annulation puis une insertion ferait passer le journal par un etat intermediaire que l'automate
   * refuserait a raison : annuler un debut y laisserait une fin orpheline. C'est ce qui justifie l'acte unique.
   * </p>
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

  public List<IntervalleDActivite> activites() {
    return journal.intervalles(cloture.map(Cloture::dateDeSurvenue));
  }

  public List<ActiviteEnCours> activitesEnCours() {
    return activites().stream().filter(IntervalleDActivite::estOuvert).map(ActiviteEnCours::of).toList();
  }

  public EtatDAtelier etat() {
    if (estCloture()) {
      return EtatDAtelier.CLOTURE;
    }

    if (!activitesEnCours().isEmpty()) {
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

  interface SuiviDAtelierIdBuilder {
    SuiviDAtelierElementBuilder id(SuiviDAtelierId id);
  }

  interface SuiviDAtelierElementBuilder {
    SuiviDAtelierEngagementBuilder element(ElementEngage element);
  }

  interface SuiviDAtelierEngagementBuilder {
    SuiviDAtelierJournalBuilder engagement(Engagement engagement);
  }

  interface SuiviDAtelierJournalBuilder {
    SuiviDAtelier journal(JournalDAtelier journal);
  }
}
