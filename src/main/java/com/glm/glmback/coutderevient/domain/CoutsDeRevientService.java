package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Collections;
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
  private final OperateursNommes operateursNommes;
  private final PostesNommes postesNommes;
  private final Clock clock;

  private CoutsDeRevientService(
    ElementsValorisables elements,
    TravailDeLElement travaux,
    OccupationDesOperateurs occupations,
    OperateursNommes operateursNommes,
    PostesNommes postesNommes,
    Clock clock
  ) {
    this.elements = elements;
    this.travaux = travaux;
    this.occupations = occupations;
    this.operateursNommes = operateursNommes;
    this.postesNommes = postesNommes;
    this.clock = clock;
  }

  public static ElementsBuilder builder() {
    return elements ->
      travaux ->
        occupations ->
          operateursNommes ->
            postesNommes -> clock -> new CoutsDeRevientService(elements, travaux, occupations, operateursNommes, postesNommes, clock);
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
      return CoutDeRevient.builder()
        .element(element)
        .tranches(tranches)
        .charges(ChargesDesOperateurs.de(List.of()))
        .lecture(lecture)
        .annuaire(annuaire(activites));
    }
    Set<OperateurId> operateurs = tranches.stream().map(TrancheDActivite::operateur).collect(Collectors.toSet());
    List<ActiviteInterpretee> occupation = occupationDesFenetres(operateurs, tranches, evaluation);
    List<TrancheDActivite> menees = terminees(occupation, evaluation);
    ChargesDesOperateurs charges = ChargesDesOperateurs.de(Stream.concat(tranches.stream(), menees.stream()).toList());
    return CoutDeRevient.builder()
      .element(element)
      .tranches(tranches)
      .charges(charges)
      .lecture(lecture)
      .annuaire(annuaire(Stream.concat(activites.stream(), occupation.stream()).toList()));
  }

  private List<ActiviteInterpretee> occupationDesFenetres(
    Set<OperateurId> operateurs,
    List<TrancheDActivite> tranches,
    Instant evaluation
  ) {
    Periode periode = couverture(tranches);
    while (true) {
      List<ActiviteInterpretee> occupation = occupations.activites(operateurs, periode);
      Periode acquise = couverture(Stream.concat(tranches.stream(), terminees(occupation, evaluation).stream()).toList());
      Periode etendue = new Periode(
        Collections.min(List.of(periode.debut(), acquise.debut())),
        Collections.max(List.of(periode.fin(), acquise.fin()))
      );
      if (etendue.equals(periode)) {
        return occupation;
      }
      periode = etendue;
    }
  }

  /**
   * Les noms de tout ce que les activites lues citent, celles de l'element comme celles menees de front ailleurs :
   * c'est la que le detail trouve l'autre element d'un temps partage.
   */
  private AnnuaireDuCout annuaire(List<ActiviteInterpretee> lues) {
    List<Activite> activites = lues.stream().map(ActiviteInterpretee::activite).toList();
    return new AnnuaireDuCout(
      operateursNommes.operateurs(activites.stream().map(Activite::operateur).collect(Collectors.toSet())),
      postesNommes.postes(
        activites
          .stream()
          .flatMap(activite -> activite.poste().stream())
          .collect(Collectors.toSet())
      ),
      elements.tous(activites.stream().map(Activite::element).collect(Collectors.toSet()))
    );
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
    OperateursNommesBuilder occupations(OccupationDesOperateurs occupations);
  }

  public interface OperateursNommesBuilder {
    PostesNommesBuilder operateursNommes(OperateursNommes operateursNommes);
  }

  public interface PostesNommesBuilder {
    ClockBuilder postesNommes(PostesNommes postesNommes);
  }

  public interface ClockBuilder {
    CoutsDeRevientService clock(Clock clock);
  }
}
