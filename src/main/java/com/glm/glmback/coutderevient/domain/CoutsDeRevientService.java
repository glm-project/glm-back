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
  private final ConflitsDuCout conflits;
  private final Clock clock;

  private CoutsDeRevientService(
    ElementsValorisables elements,
    TravailDeLElement travaux,
    OccupationDesOperateurs occupations,
    ConflitsDuCout conflits,
    Clock clock
  ) {
    this.elements = elements;
    this.travaux = travaux;
    this.occupations = occupations;
    this.conflits = conflits;
    this.clock = clock;
  }

  public static ElementsBuilder builder() {
    return elements ->
      travaux -> occupations -> conflits -> clock -> new CoutsDeRevientService(elements, travaux, occupations, conflits, clock);
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
          .filter(activite -> !activite.aResoudre() && activite.termineeA(evaluation).isEmpty())
          .count()
      )
    );
    List<ActiviteInterpretee> aResoudre = activites.stream().filter(ActiviteInterpretee::aResoudre).toList();
    List<SequenceEnConflit> propres = conflits.deLElement(id);
    if (tranches.isEmpty()) {
      return CoutDeRevient.builder()
        .element(element)
        .tranches(tranches)
        .aResoudre(aResoudre)
        .charges(ChargesDesOperateurs.de(List.of()))
        .lecture(lecture)
        .conflits(propres);
    }
    Set<OperateurId> operateurs = tranches.stream().map(TrancheDActivite::operateur).collect(Collectors.toSet());
    List<ActiviteInterpretee> occupation = occupations.activites(operateurs, couverture(tranches));
    List<TrancheDActivite> menees = terminees(occupation, evaluation);
    List<ZoneIncertaine> zones = Stream.concat(activites.stream(), occupation.stream())
      .flatMap(activite -> activite.zoneA(evaluation).stream())
      .toList();
    ChargesDesOperateurs charges = ChargesDesOperateurs.de(Stream.concat(tranches.stream(), menees.stream()).toList(), zones);
    Set<ActiviteId> responsables = charges.responsables(tranches);
    List<SequenceEnConflit> dependances = Stream.concat(
      propres.stream(),
      conflits
        .desOperateurs(operateurs)
        .stream()
        .filter(sequence -> sequence.concerne(responsables))
    )
      .distinct()
      .toList();
    return CoutDeRevient.builder()
      .element(element)
      .tranches(tranches)
      .aResoudre(aResoudre)
      .charges(charges)
      .lecture(lecture)
      .conflits(dependances);
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
    ConflitsBuilder occupations(OccupationDesOperateurs occupations);
  }

  public interface ConflitsBuilder {
    ClockBuilder conflits(ConflitsDuCout conflits);
  }

  public interface ClockBuilder {
    CoutsDeRevientService clock(Clock clock);
  }
}
