package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Valorise les seules activites terminees, interpretees par atelier. */
public final class CoutsDeRevientService {

  private final ElementsValorisables elements;
  private final TravailDeLElement travaux;
  private final OccupationDesOperateurs occupations;
  private final Clock clock;

  private CoutsDeRevientService(
    ElementsValorisables elements,
    TravailDeLElement travaux,
    OccupationDesOperateurs occupations,
    Clock clock
  ) {
    this.elements = elements;
    this.travaux = travaux;
    this.occupations = occupations;
    this.clock = clock;
  }

  public static ElementsBuilder builder() {
    return elements -> travaux -> occupations -> clock -> new CoutsDeRevientService(elements, travaux, occupations, clock);
  }

  public CoutDeRevient rapport(ElementId id) {
    ElementValorise element = elements.get(id).orElseThrow(() -> new ElementInconnuException(id));
    Instant evaluation = clock.now();
    List<ActiviteInterpretee> activites = travaux.activites(id);
    List<TrancheDActivite> tranches = terminees(activites, evaluation);
    EvaluationDuCout lecture = new EvaluationDuCout(
      evaluation,
      Math.toIntExact(
        activites
          .stream()
          .filter(activite -> activite.termineeA(evaluation).isEmpty())
          .count()
      )
    );
    if (tranches.isEmpty()) {
      return new CoutDeRevient(element, List.of(), lecture);
    }
    Set<OperateurId> operateurs = tranches.stream().map(TrancheDActivite::operateur).collect(Collectors.toSet());
    List<TrancheDActivite> menees = terminees(occupations.activites(operateurs, couverture(tranches)), evaluation);
    return CoutDeRevient.builder()
      .element(element)
      .tranches(tranches)
      .charges(ChargesDesOperateurs.de(Stream.concat(tranches.stream(), menees.stream()).toList()))
      .lecture(lecture);
  }

  private static List<TrancheDActivite> terminees(List<ActiviteInterpretee> activites, Instant evaluation) {
    return activites
      .stream()
      .flatMap(activite -> activite.termineeA(evaluation).stream())
      .toList();
  }

  private static Periode couverture(List<TrancheDActivite> tranches) {
    Instant debut = tranches
      .stream()
      .map(tranche -> tranche.periode().debut())
      .min(Comparator.naturalOrder())
      .orElseThrow();
    Instant fin = tranches
      .stream()
      .map(tranche -> tranche.periode().fin())
      .max(Comparator.naturalOrder())
      .orElseThrow();
    return new Periode(debut, fin);
  }

  public interface ElementsBuilder {
    TravauxBuilder elements(ElementsValorisables elements);
  }

  public interface TravauxBuilder {
    OccupationsBuilder travaux(TravailDeLElement travaux);
  }

  public interface OccupationsBuilder {
    ClockBuilder occupations(OccupationDesOperateurs occupations);
  }

  public interface ClockBuilder {
    CoutsDeRevientService clock(Clock clock);
  }
}
