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
 * Les intervalles produits ici sont bruts : ils ignorent le depart de l'operateur, qui vit dans sa journee de
 * travail. Le temps effectif se lit a l'intersection des deux, par {@link TempsDAtelierService}.
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
   * Annule un evenement et lui substitue sa version corrigee, en validant la seule sequence finale.
   *
   * <p>
   * Enchainer une annulation puis une insertion ferait passer le journal par un etat intermediaire que l'interpretation
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
   * Vrai si l'evenement arrete une activite deja arretee par une fin reelle, sans etre date avant le dernier fait de
   * son poste, et sans qu'aucune activite n'y soit en cours a son heure : le double appui sur « arreter », qui ne
   * change rien. Date avant, ce serait un geste rejoue dans le desordre. Une fin qui vise une activite echue, elle, est
   * conservee : elle n'arrete rien, mais c'est un fait.
   */
  public boolean arreteUneActiviteAbsente(EvenementDAtelier evenement) {
    Instant heure = evenement.dateDeSurvenue();
    List<Activite> activitesDuPoste = activites()
      .stream()
      .filter(activite -> activite.cle().equals(evenement.cle()))
      .toList();

    return (
      evenement.intention() == IntentionDePointage.FIN
      && activitesDuPoste
        .stream()
        .anyMatch(activite -> evenement.activiteVisee().filter(activite.id()::equals).isPresent() && activite.fin().isPresent())
      && activitesDuPoste.stream().noneMatch(activite -> activite.estEnCoursA(heure))
      && journal
        .actifs()
        .stream()
        .filter(fait -> fait.cle().equals(evenement.cle()))
        .noneMatch(fait -> heure.isBefore(fait.dateDeSurvenue()))
    );
  }

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
