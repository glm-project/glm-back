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
 * C'est aussi ici que la nature de l'operation est recopiee du profil de l'operateur. Elle n'est jamais verifiee : le
 * client ne demande aucun blocage, et un pointage refuse en atelier couterait plus cher qu'une ligne de synthese mal
 * rangee.
 * </p>
 */
public final class SuivisDAtelierService {

  private final SuiviDAtelierRepository repository;
  private final ElementsEngageables elements;
  private final FonctionsDesOperateurs fonctions;
  private final Clock clock;

  private SuivisDAtelierService(
    SuiviDAtelierRepository repository,
    ElementsEngageables elements,
    FonctionsDesOperateurs fonctions,
    Clock clock
  ) {
    this.repository = repository;
    this.elements = elements;
    this.fonctions = fonctions;
    this.clock = clock;
  }

  public static SuivisDAtelierServiceRepositoryBuilder builder() {
    return repository -> elements -> fonctions -> clock -> new SuivisDAtelierService(repository, elements, fonctions, clock);
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

  public SuiviDAtelier pointe(PointageAEnregistrer commande) {
    SuiviDAtelier suivi = get(commande.suivi());
    if (suivi.estCloture()) {
      throw new SuiviDAtelierClotureException(suivi.id());
    }

    Horodatage horodatage = Horodatage.saisiA(clock.now());

    return repository.update(
      suivi.enregistre(evenement(commande.type(), commande.operateur(), commande.poste(), auteurDe(commande.operateur()), horodatage))
    );
  }

  public SuiviDAtelier regularise(RegularisationAEnregistrer commande) {
    return repository.update(get(commande.suivi()).enregistre(regularisation(commande)));
  }

  public SuiviDAtelier annule(AnnulationAEnregistrer commande) {
    return repository.update(get(commande.suivi()).annule(commande.evenement(), annulation(commande.auteur(), commande.motif())));
  }

  public SuiviDAtelier corrige(CorrectionAEnregistrer commande) {
    RegularisationAEnregistrer remplacement = commande.remplacement();

    return repository.update(
      get(remplacement.suivi()).corrige(
        commande.evenement(),
        annulation(remplacement.auteur(), commande.motif()),
        regularisation(remplacement)
      )
    );
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

  private EvenementDAtelier regularisation(RegularisationAEnregistrer commande) {
    return evenement(
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
    TypeDEvenementDAtelier type,
    Operateur operateur,
    Optional<PosteDeTravail> poste,
    Auteur auteur,
    Horodatage horodatage
  ) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(type)
      .operateur(operateur)
      .poste(poste)
      .nature(fonctions.fonction(operateur))
      .auteur(auteur)
      .horodatage(horodatage);
  }

  private static Auteur auteurDe(Operateur operateur) {
    return new Auteur(operateur.value());
  }

  public interface SuivisDAtelierServiceRepositoryBuilder {
    SuivisDAtelierServiceElementsBuilder repository(SuiviDAtelierRepository repository);
  }

  public interface SuivisDAtelierServiceElementsBuilder {
    SuivisDAtelierServiceFonctionsBuilder elements(ElementsEngageables elements);
  }

  public interface SuivisDAtelierServiceFonctionsBuilder {
    SuivisDAtelierServiceClockBuilder fonctions(FonctionsDesOperateurs fonctions);
  }

  public interface SuivisDAtelierServiceClockBuilder {
    SuivisDAtelierService clock(Clock clock);
  }
}
