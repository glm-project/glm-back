package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assemble le cout de revient d'un element de fabrication a partir des journaux de l'atelier.
 *
 * <p>
 * Rien n'est stocke, tout est recalcule. Six etapes : resoudre l'element, replier ses suivis, ramener le temps aux
 * fenetres de presence, arreter le temps a l'instant present, deduire le diviseur de chaque operateur de
 * <strong>tout</strong> ce qu'il menait de front, puis valoriser et grouper par nature.
 * </p>
 *
 * <p>
 * Ce contexte porte une horloge, contrairement a la feuille de temps qui s'en interdit une : deux appels identiques
 * ne rendent donc pas forcement la meme chose. C'est assume — le cout de revient d'un element en cours n'a de sens
 * qu'arrete a maintenant, et un travail non termine n'a pas de duree.
 * </p>
 */
public final class CoutsDeRevientService {

  private final ElementsValorisables elements;
  private final TravailDeLElement travaux;
  private final OccupationDesOperateurs occupations;
  private final PresenceDesOperateurs presences;
  private final Clock clock;

  private CoutsDeRevientService(
    ElementsValorisables elements,
    TravailDeLElement travaux,
    OccupationDesOperateurs occupations,
    PresenceDesOperateurs presences,
    Clock clock
  ) {
    this.elements = elements;
    this.travaux = travaux;
    this.occupations = occupations;
    this.presences = presences;
    this.clock = clock;
  }

  public static CoutsDeRevientServiceElementsBuilder builder() {
    return elements ->
      travaux -> occupations -> presences -> clock -> new CoutsDeRevientService(elements, travaux, occupations, presences, clock);
  }

  public CoutDeRevient rapport(ElementId id) {
    ElementValorise element = elements.get(id).orElseThrow(() -> new ElementInconnuException(id));
    List<IntervalleDActivite> bruts = intervalles(travaux.suivis(id));

    if (bruts.isEmpty()) {
      return new CoutDeRevient(element, List.of());
    }

    Instant maintenant = clock.now();
    Set<OperateurId> operateurs = operateurs(bruts);
    List<IntervalleDActivite> occupation = intervalles(occupations.suivis(operateurs, finDe(bruts, maintenant)));

    ReductionALaPresence reduction = reduction(operateurs, Stream.concat(bruts.stream(), occupation.stream()).toList(), maintenant);
    List<TrancheDActivite> tranches = tranches(reduction, bruts, maintenant);
    List<TrancheDActivite> menees = tranches(reduction, occupation, maintenant);

    return CoutDeRevient.de(element, tranches, ChargesDesOperateurs.de(Stream.concat(tranches.stream(), menees.stream()).toList()));
  }

  /**
   * Les charges sont baties sur l'union de ce qui est valorise et de ce qui a ete lu : c'est ce qui garantit qu'aucune
   * tranche de l'element ne se retrouve sans diviseur, sans avoir a supposer que la lecture d'occupation la recouvre.
   */
  private ReductionALaPresence reduction(Set<OperateurId> operateurs, List<IntervalleDActivite> tout, Instant maintenant) {
    return ReductionALaPresence.de(presences.presences(operateurs, couverture(tout, maintenant)));
  }

  private static List<IntervalleDActivite> intervalles(List<SuiviDuTravail> suivis) {
    return suivis
      .stream()
      .flatMap(suivi -> suivi.intervalles().stream())
      .toList();
  }

  private static Set<OperateurId> operateurs(List<IntervalleDActivite> intervalles) {
    return intervalles.stream().map(IntervalleDActivite::operateur).collect(Collectors.toSet());
  }

  private static List<TrancheDActivite> tranches(
    ReductionALaPresence reduction,
    List<IntervalleDActivite> intervalles,
    Instant maintenant
  ) {
    return intervalles
      .stream()
      .flatMap(intervalle -> reduction.reduit(intervalle).stream())
      .map(intervalle -> intervalle.ferme(maintenant))
      .toList();
  }

  private static Periode couverture(List<IntervalleDActivite> intervalles, Instant maintenant) {
    Instant debut = intervalles
      .stream()
      .map(intervalle -> intervalle.plage().debut())
      .min(Comparator.naturalOrder())
      .orElse(maintenant);

    return new Periode(debut, finDe(intervalles, maintenant));
  }

  private static Instant finDe(List<IntervalleDActivite> intervalles, Instant maintenant) {
    return intervalles
      .stream()
      .map(intervalle -> intervalle.plage().fin().orElse(maintenant))
      .max(Comparator.naturalOrder())
      .orElse(maintenant);
  }

  public interface CoutsDeRevientServiceElementsBuilder {
    CoutsDeRevientServiceTravauxBuilder elements(ElementsValorisables elements);
  }

  public interface CoutsDeRevientServiceTravauxBuilder {
    CoutsDeRevientServiceOccupationsBuilder travaux(TravailDeLElement travaux);
  }

  public interface CoutsDeRevientServiceOccupationsBuilder {
    CoutsDeRevientServicePresencesBuilder occupations(OccupationDesOperateurs occupations);
  }

  public interface CoutsDeRevientServicePresencesBuilder {
    CoutsDeRevientServiceClockBuilder presences(PresenceDesOperateurs presences);
  }

  public interface CoutsDeRevientServiceClockBuilder {
    CoutsDeRevientService clock(Clock clock);
  }
}
