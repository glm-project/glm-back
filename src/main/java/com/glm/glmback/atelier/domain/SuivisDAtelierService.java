package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * La fabrique de domaine de l'atelier : c'est ici, et nulle part ailleurs, que se decide une date de survenue.
 *
 * <p>
 * Un pointage se date sur l'horloge, une regularisation sur la valeur fournie ; la date d'enregistrement vaut
 * l'instant present dans les deux cas. C'est aussi ici que l'evenement recoit son origine : un pointage, meme rejoue
 * hors ligne avec l'heure de son geste, ou une regularisation. L'agregat, lui, ne voit qu'un evenement deja horodate et qualifie.
 * </p>
 *
 * <p>
 * C'est encore ici qu'un pointage ouvrant recoit l'identite de l'activite qu'il ouvre : la sienne.
 * </p>
 *
 * <p>
 * C'est aussi ici que les ressources sont resolues : l'operateur et le poste doivent exister, l'operateur doit etre
 * habilite sur le poste, et la nature de l'operation est recopiee <b>du poste</b>. Un operateur polyvalent declenche
 * un pointage par poste, et chacun sait de quel metier il releve parce que le poste le dit.
 * </p>
 *
 * <p>
 * La verification vaut pour les deux ecritures du journal — pointage et regularisation. Sans quoi le
 * back-office deviendrait un contournement de la regle que le pupitre applique.
 * </p>
 */
public final class SuivisDAtelierService {

  private static final Duration DERIVE_D_HORLOGE_TOLEREE = Duration.ofMinutes(2);

  private final SuiviDAtelierRepository repository;
  private final ElementsEngageables elements;
  private final OperateursConnus operateurs;
  private final PostesConnus postes;
  private final Habilitations habilitations;
  private final PointagesIgnores pointagesIgnores;
  private final Clock clock;

  private SuivisDAtelierService(
    SuiviDAtelierRepository repository,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    PointagesIgnores pointagesIgnores,
    Clock clock
  ) {
    this.repository = repository;
    this.elements = elements;
    this.operateurs = operateurs;
    this.postes = postes;
    this.habilitations = habilitations;
    this.pointagesIgnores = pointagesIgnores;
    this.clock = clock;
  }

  public static SuivisDAtelierServiceRepositoryBuilder builder() {
    return repository ->
      elements ->
        operateurs ->
          postes ->
            habilitations ->
              pointagesIgnores ->
                clock -> new SuivisDAtelierService(repository, elements, operateurs, postes, habilitations, pointagesIgnores, clock);
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
   * Juge un pointage de l'operateur selon la regle de reception : les controles existants d'abord (OF cloture,
   * operateur ou poste introuvable, habilitation), puis le suivi decide s'il est accepte ou ignore.
   *
   * <p>
   * Le suivi est verrouille des l'entree : un pointage ignore ecrit lui aussi son audit, et deux pointages simultanes
   * du meme suivi se jugent l'un apres l'autre. Un identifiant deja present dans la table des evenements est un renvoi,
   * et un identifiant deja present dans l'audit est refuse de nouveau sans nouvelle ligne : l'un et l'autre avant
   * toute regle. Demarrer ou pointer une non conformite sur un OF cloture reste refuse, comme un operateur ou un poste
   * inconnu ; une fin posterieure a la cloture est ignoree, la cloture ayant termine l'activite.
   * </p>
   *
   * <p>
   * Un pointage ignore n'entre pas au journal : il est ecrit dans l'audit, et le suivi rendu est inchange. Accepte, il
   * entre au journal comme une ouverture, ou comme la fin de l'activite en cours de sa cle.
   * </p>
   */
  public PointageDAtelierTraite pointe(PointageAEnregistrer commande) {
    SuiviDAtelier suivi = repository
      .getForUpdate(commande.suivi())
      .orElseThrow(() -> new SuiviDAtelierIntrouvableException(commande.suivi()));
    if (repository.contientEvenement(commande.evenement())) {
      return new PointageDAtelierTraite(suivi, IssueDePointage.REJOUE);
    }
    if (pointagesIgnores.contient(commande.evenement())) {
      return new PointageDAtelierTraite(suivi, IssueDePointage.IGNORE);
    }
    if (suivi.estCloture() && commande.type().ouvreUneActivite()) {
      throw new SuiviDAtelierClotureException(suivi.id());
    }

    Instant maintenant = clock.now();
    Horodatage horodatage = new Horodatage(survenue(commande.dateDeSurvenue(), maintenant), maintenant);
    Ressources ressources = ressources(commande.operateur(), commande.poste());

    return switch (suivi.juge(new CleDActivite(commande.operateur(), commande.poste()), commande.type(), horodatage.dateDeSurvenue())) {
      case VerdictDeReception.Ignore ignore -> {
        pointagesIgnores.enregistre(new PointageIgnore(commande, horodatage, ignore));
        yield new PointageDAtelierTraite(suivi, IssueDePointage.IGNORE);
      }
      case VerdictDeReception.Accepte _ -> {
        EvenementDAtelier evenement = evenement(
          commande.evenement(),
          commande.type(),
          Optional.empty(),
          ressources,
          commande.auteur(),
          OrigineDuPointage.POINTAGE,
          horodatage
        );
        yield new PointageDAtelierTraite(repository.update(suivi.enregistre(evenement)), IssueDePointage.ACCEPTE);
      }
    };
  }

  /**
   * Regularise la fin d'une activite echue, sans passer par la regle de reception des pointages.
   *
   * <p>
   * Un evenement deja present dans la table des evenements, de ce suivi ou d'un autre, est un renvoi : il repond comme
   * un succes et n'ecrit rien, avant toute regle. Sinon la
   * fin est datee sur la valeur fournie ; l'operateur et le poste sont ceux de l'activite, et la nature de l'operation,
   * le cout et le taux horaires sont figes comme pour un pointage.
   * </p>
   */
  public RegularisationTraitee regularise(RegularisationAEnregistrer commande) {
    SuiviDAtelier suivi = get(commande.suivi());
    if (repository.contientEvenement(commande.evenement())) {
      return new RegularisationTraitee(suivi, true);
    }

    Instant maintenant = clock.now();
    Activite activite = suivi.exigeUneFinRegularisable(commande.activite(), commande.dateDeSurvenue(), maintenant);
    EvenementDAtelier fin = evenement(
      commande.evenement(),
      TypeDEvenementDAtelier.FIN,
      Optional.of(commande.activite()),
      ressources(activite.ouvrant().operateur(), activite.ouvrant().poste()),
      commande.auteur(),
      OrigineDuPointage.REGULARISATION,
      new Horodatage(commande.dateDeSurvenue(), maintenant)
    );

    return new RegularisationTraitee(repository.update(suivi.enregistre(fin)), false);
  }

  public SuiviDAtelier cloture(ClotureAEnregistrer commande) {
    Instant maintenant = clock.now();
    Horodatage horodatage = new Horodatage(survenue(commande.dateDeSurvenue(), maintenant), maintenant);

    return repository.update(get(commande.suivi()).cloture(new Cloture(commande.auteur(), horodatage)));
  }

  public SuiviDAtelier annuleLaCloture(SuiviDAtelierId id) {
    return repository.update(get(id).annuleLaCloture());
  }

  public SuiviDAtelier get(SuiviDAtelierId id) {
    return repository.get(id).orElseThrow(() -> new SuiviDAtelierIntrouvableException(id));
  }

  /**
   * Les suivis dont l'etat, a l'instant d'evaluation fourni, est l'un de ceux demandes.
   */
  public Page<SuiviDAtelier> list(Optional<Periode> periode, Set<EtatDAtelier> etats, Instant evaluation, Pageable pageable) {
    return repository.list(new SuiviDAtelierCriteria(periode, etats, evaluation), pageable);
  }

  /**
   * La date de survenue d'un geste horodate par le pupitre, lue sur l'horloge du serveur.
   *
   * <p>
   * Le poste date le geste avec sa propre horloge, qui peut avancer un peu sur celle du serveur : une avance
   * jusqu'a {@link #DERIVE_D_HORLOGE_TOLEREE} est ramenee a l'instant courant, au-dela la date est refusee.
   * </p>
   */
  private static Instant survenue(Optional<Instant> dateDeSurvenue, Instant maintenant) {
    if (dateDeSurvenue.filter(date -> date.isAfter(maintenant.plus(DERIVE_D_HORLOGE_TOLEREE))).isPresent()) {
      throw new DateDeSurvenueFutureException(dateDeSurvenue.orElseThrow());
    }

    return dateDeSurvenue.filter(date -> date.isBefore(maintenant)).orElse(maintenant);
  }

  private static EvenementDAtelier evenement(
    EvenementDAtelierId evenement,
    TypeDEvenementDAtelier type,
    Optional<ActiviteId> cible,
    Ressources ressources,
    Auteur auteur,
    OrigineDuPointage origine,
    Horodatage horodatage
  ) {
    return EvenementDAtelier.builder()
      .id(evenement)
      .type(type)
      .activite(type.ouvreUneActivite() ? Optional.of(ActiviteId.ouvertePar(evenement)) : Optional.empty())
      .activiteVisee(cible)
      .operateur(ressources.operateur().id())
      .poste(ressources.poste().map(PosteConnu::id))
      .nature(ressources.poste().map(PosteConnu::nature))
      .coutHoraire(ressources.poste().flatMap(PosteConnu::coutHoraire))
      .tauxHoraire(ressources.operateur().tauxHoraire())
      .auteur(auteur)
      .origine(origine)
      .horodatage(horodatage);
  }

  /**
   * L'operateur et le poste d'un geste, une fois etabli qu'ils existent et que l'operateur est habilite sur le poste.
   */
  private Ressources ressources(OperateurId operateur, Optional<PosteDeTravailId> poste) {
    return new Ressources(operateurConnu(operateur), poste.map(id -> posteHabilite(operateur, id)));
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
  private PosteConnu posteHabilite(OperateurId operateur, PosteDeTravailId poste) {
    PosteConnu connu = postes.get(poste).orElseThrow(() -> new PosteDAtelierIntrouvableException(poste));

    if (!habilitations.estHabilite(operateur, poste)) {
      throw new OperateurNonHabiliteException(operateur, poste);
    }

    return connu;
  }

  private record Ressources(OperateurConnu operateur, Optional<PosteConnu> poste) {}

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
    SuivisDAtelierServicePointagesIgnoresBuilder habilitations(Habilitations habilitations);
  }

  public interface SuivisDAtelierServicePointagesIgnoresBuilder {
    SuivisDAtelierServiceClockBuilder pointagesIgnores(PointagesIgnores pointagesIgnores);
  }

  public interface SuivisDAtelierServiceClockBuilder {
    SuivisDAtelierService clock(Clock clock);
  }
}
