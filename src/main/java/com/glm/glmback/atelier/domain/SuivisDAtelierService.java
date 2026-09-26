package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * La fabrique de domaine de l'atelier : c'est ici, et nulle part ailleurs, que se decide une date de survenue.
 *
 * <p>
 * Un pointage se date sur l'horloge, une regularisation sur la valeur fournie ; la date d'enregistrement vaut
 * l'instant present dans les deux cas. L'agregat, lui, ne voit qu'un evenement deja horodate, ce qui lui epargne de
 * distinguer les deux actes.
 * </p>
 *
 * <p>
 * C'est aussi ici que les ressources sont resolues : l'operateur et le poste doivent exister, l'operateur doit etre
 * habilite sur le poste, et la nature de l'operation est recopiee <b>du poste</b>. Un operateur polyvalent declenche
 * un pointage par poste, et chacun sait de quel metier il releve parce que le poste le dit.
 * </p>
 *
 * <p>
 * La verification vaut pour les trois ecritures du journal — pointage, regularisation et correction. Sans quoi le
 * back-office deviendrait un contournement de la regle que le pupitre applique.
 * </p>
 */
public final class SuivisDAtelierService {

  private final SuiviDAtelierRepository repository;
  private final ElementsEngageables elements;
  private final OperateursConnus operateurs;
  private final PostesConnus postes;
  private final Habilitations habilitations;
  private final RegistreDesSignalements signalements;
  private final Clock clock;

  private SuivisDAtelierService(
    SuiviDAtelierRepository repository,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    PointagesSignales signalements,
    Clock clock
  ) {
    this.repository = repository;
    this.elements = elements;
    this.operateurs = operateurs;
    this.postes = postes;
    this.habilitations = habilitations;
    this.signalements = new RegistreDesSignalements(signalements, clock);
    this.clock = clock;
  }

  public static SuivisDAtelierServiceRepositoryBuilder builder() {
    return repository ->
      elements ->
        operateurs ->
          postes ->
            habilitations ->
              signalements ->
                clock -> new SuivisDAtelierService(repository, elements, operateurs, postes, habilitations, signalements, clock);
  }

  /**
   * Met un element de fabrication en atelier.
   *
   * <p>
   * C'est un geste metier du back-office, distinct de la creation de l'element : tout ce qui est cree n'est pas
   * forcement a faire, et c'est cet acte qui fait apparaitre l'element sur l'ecran des operateurs.
   * </p>
   */
  public SuiviDAtelier engage(EngagementAEnregistrer commande) {
    ElementEngage element = elements
      .get(commande.element())
      .orElseThrow(() -> new ElementEngageableIntrouvableException(commande.element()));

    if (repository.getEnCoursPour(commande.element()).isPresent()) {
      throw new ElementDejaEngageException(commande.element());
    }

    return repository.create(
      SuiviDAtelier.builder()
        .id(SuiviDAtelierId.newId())
        .element(element)
        .engagement(new Engagement(commande.auteur(), clock.now()))
        .journal(JournalDAtelier.vide())
    );
  }

  /**
   * Un pointage d'atelier n'est jamais refuse a l'operateur, a une exception pres (lot 8a de la strategie « bornes de
   * fin de journee ») : demarrer ou pointer une non conformite sur un OF cloture, qui n'est plus pointable.
   *
   * <p>
   * Arreter une activite qui n'est pas en cours, ou un OF que la cloture a deja arrete, ne change rien : le geste est
   * absorbe. Une fin rejouee dans le desordre, datee avant le dernier fait de son activite, est refusee ici :
   * {@link PointagesEnAttenteService} la met en attente (lot 8c).
   * </p>
   */
  public PointageDAtelierTraite pointe(PointageAEnregistrer commande) {
    SuiviDAtelier suivi = get(commande.suivi());
    if (suivi.estCloture()) {
      if (commande.type() == TypeDEvenementDAtelier.FIN) {
        return new PointageDAtelierTraite(suivi, true);
      }
      throw new SuiviDAtelierClotureException(suivi.id());
    }

    RegistreDesSignalements.DateRedressee date = RegistreDesSignalements.redresse(
      commande.dateDeSurvenue(),
      clock.now(),
      Optional.of(suivi.engagement().date())
    );
    OperateurConnu operateurConnu = operateurConnu(commande.operateur());
    Optional<PosteConnu> posteConnu = commande.poste().map(this::posteConnu);
    if (
      commande
        .poste()
        .filter(poste -> !habilitations.estHabilite(commande.operateur(), poste))
        .isPresent()
    ) {
      date = date.avec(MotifDeSignalement.OPERATEUR_NON_HABILITE);
    }
    EvenementDAtelier evenement = evenement(
      commande.evenement(),
      commande.type(),
      commande.operateur(),
      commande.poste(),
      commande.auteur(),
      date.horodatage(),
      operateurConnu,
      posteConnu
    );

    if (suivi.arreteUneActiviteAbsente(evenement)) {
      return new PointageDAtelierTraite(suivi, true);
    }

    SuiviDAtelier pointe = repository.update(suivi.enregistre(evenement));
    signalements.signale(
      commande.evenement().uuid(),
      new CibleDuSignalement(TypeDeCible.SUIVI_D_ATELIER, suivi.id().uuid()),
      commande.operateur(),
      date
    );

    return new PointageDAtelierTraite(pointe, false);
  }

  public SuiviDAtelier regularise(RegularisationAEnregistrer commande) {
    return regularise(commande, EvenementDAtelierId.newId());
  }

  public SuiviDAtelier regularise(RegularisationAEnregistrer commande, EvenementDAtelierId evenement) {
    return repository.update(get(commande.suivi()).enregistre(regularisation(commande, evenement)));
  }

  public SuiviDAtelier annule(AnnulationAEnregistrer commande) {
    SuiviDAtelier annule = repository.update(
      get(commande.suivi()).annule(commande.evenement(), annulation(commande.auteur(), commande.motif()))
    );
    signalements.resout(commande.evenement().uuid(), TypeDeResolution.ANNULE, commande.auteur());

    return annule;
  }

  public SuiviDAtelier corrige(CorrectionAEnregistrer commande) {
    return corrige(commande, EvenementDAtelierId.newId());
  }

  public SuiviDAtelier corrige(CorrectionAEnregistrer commande, EvenementDAtelierId remplacementId) {
    RegularisationAEnregistrer remplacement = commande.remplacement();

    SuiviDAtelier corrige = repository.update(
      get(remplacement.suivi()).corrige(
        commande.evenement(),
        annulation(remplacement.auteur(), commande.motif()),
        regularisation(remplacement, remplacementId)
      )
    );
    signalements.resout(commande.evenement().uuid(), TypeDeResolution.CORRIGE, remplacement.auteur());

    return corrige;
  }

  public SuiviDAtelier cloture(ClotureAEnregistrer commande) {
    Instant maintenant = clock.now();
    Horodatage horodatage = new Horodatage(commande.dateDeSurvenue().orElse(maintenant), maintenant);

    return repository.update(get(commande.suivi()).cloture(new Cloture(commande.auteur(), horodatage)));
  }

  public SuiviDAtelier annuleLaCloture(SuiviDAtelierId id) {
    return repository.update(get(id).annuleLaCloture());
  }

  public SuiviDAtelier get(SuiviDAtelierId id) {
    return repository.get(id).orElseThrow(() -> new SuiviDAtelierIntrouvableException(id));
  }

  public Page<SuiviDAtelier> list(Optional<Periode> periode, Set<EtatDAtelier> etats, Pageable pageable) {
    return repository.list(new SuiviDAtelierCriteria(periode, etats), pageable);
  }

  private EvenementDAtelier regularisation(RegularisationAEnregistrer commande, EvenementDAtelierId evenement) {
    return evenement(
      evenement,
      commande.type(),
      commande.operateur(),
      commande.poste(),
      commande.auteur(),
      new Horodatage(commande.dateDeSurvenue(), clock.now())
    );
  }

  private Annulation annulation(Auteur auteur, MotifDAnnulation motif) {
    return new Annulation(auteur, clock.now(), motif);
  }

  private EvenementDAtelier evenement(
    EvenementDAtelierId evenement,
    TypeDEvenementDAtelier type,
    OperateurId operateur,
    Optional<PosteDeTravailId> poste,
    Auteur auteur,
    Horodatage horodatage
  ) {
    return evenement(
      evenement,
      type,
      operateur,
      poste,
      auteur,
      horodatage,
      operateurConnu(operateur),
      poste.map(id -> posteHabilite(operateur, id))
    );
  }

  private static EvenementDAtelier evenement(
    EvenementDAtelierId evenement,
    TypeDEvenementDAtelier type,
    OperateurId operateur,
    Optional<PosteDeTravailId> poste,
    Auteur auteur,
    Horodatage horodatage,
    OperateurConnu operateurConnu,
    Optional<PosteConnu> posteConnu
  ) {
    return EvenementDAtelier.builder()
      .id(evenement)
      .type(type)
      .operateur(operateur)
      .poste(poste)
      .nature(posteConnu.map(PosteConnu::nature))
      .coutHoraire(posteConnu.flatMap(PosteConnu::coutHoraire))
      .tauxHoraire(operateurConnu.tauxHoraire())
      .auteur(auteur)
      .horodatage(horodatage);
  }

  private OperateurConnu operateurConnu(OperateurId operateur) {
    return operateurs.get(operateur).orElseThrow(() -> new OperateurDAtelierIntrouvableException(operateur));
  }

  /**
   * Le poste pointe, une fois etabli qu'il existe et que l'operateur y est habilite.
   *
   * <p>
   * L'habilitation est la seule regle dure de ce contexte : la nature, elle, ne bloque toujours rien. La regle ne joue
   * que lorsqu'un poste est fourni, une entreprise sans parc machine n'ayant aucune habilitation a declarer.
   * </p>
   */
  private PosteConnu posteConnu(PosteDeTravailId poste) {
    return postes.get(poste).orElseThrow(() -> new PosteDAtelierIntrouvableException(poste));
  }

  private PosteConnu posteHabilite(OperateurId operateur, PosteDeTravailId poste) {
    PosteConnu connu = posteConnu(poste);

    if (!habilitations.estHabilite(operateur, poste)) {
      throw new OperateurNonHabiliteException(operateur, poste);
    }

    return connu;
  }

  public interface SuivisDAtelierServiceRepositoryBuilder {
    SuivisDAtelierServiceElementsBuilder repository(SuiviDAtelierRepository repository);
  }

  public interface SuivisDAtelierServiceElementsBuilder {
    SuivisDAtelierServiceOperateursBuilder elements(ElementsEngageables elements);
  }

  public interface SuivisDAtelierServiceOperateursBuilder {
    SuivisDAtelierServicePostesBuilder operateurs(OperateursConnus operateurs);
  }

  public interface SuivisDAtelierServicePostesBuilder {
    SuivisDAtelierServiceHabilitationsBuilder postes(PostesConnus postes);
  }

  public interface SuivisDAtelierServiceHabilitationsBuilder {
    SuivisDAtelierServiceSignalementsBuilder habilitations(Habilitations habilitations);
  }

  public interface SuivisDAtelierServiceSignalementsBuilder {
    SuivisDAtelierServiceClockBuilder signalements(PointagesSignales signalements);
  }

  public interface SuivisDAtelierServiceClockBuilder {
    SuivisDAtelierService clock(Clock clock);
  }
}
