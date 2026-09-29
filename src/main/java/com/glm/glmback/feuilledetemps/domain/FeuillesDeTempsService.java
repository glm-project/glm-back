package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Assemble les sept jours de la semaine depuis les activites interpretees par atelier.
 * L'instant d'evaluation est releve une seule fois ; seul le decoupage connait le calendrier de l'entreprise.
 */
public final class FeuillesDeTempsService {

  private static final Comparator<IntervalleDUnJour> PAR_DEBUT = Comparator.<IntervalleDUnJour, Instant>comparing(intervalle ->
    intervalle.intervalle().plage().debut()
  )
    .thenComparing(intervalle -> intervalle.intervalle().activite().element().uuid())
    .thenComparing(intervalle -> intervalle.intervalle().lecture().id().uuid());

  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;
  private final ActivitesDeLOperateur activites;
  private final Clock clock;

  private FeuillesDeTempsService(
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    ActivitesDeLOperateur activites,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.fuseau = fuseau;
    this.activites = activites;
    this.clock = clock;
  }

  public static FeuillesDeTempsServiceOperateursBuilder builder() {
    return operateurs -> fuseau -> activites -> clock -> new FeuillesDeTempsService(operateurs, fuseau, activites, clock);
  }

  public FeuilleDeTemps historique(OperateurId operateur, SemaineCalendaire semaine) {
    return historique(operateur, semaine, Optional.empty());
  }

  public FeuilleDeTemps historique(OperateurId operateur, SemaineCalendaire semaine, Optional<Instant> evaluationDemandee) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());
    Instant evaluation = evaluationDemandee.orElseGet(clock::now);

    return FeuilleDeTemps.builder()
      .operateur(connu)
      .semaine(semaine)
      .evaluation(evaluation)
      .jours(jours(decoupage, travailDeLaSemaine(operateur, decoupage, evaluation)));
  }

  private List<IntervalleDUnJour> travailDeLaSemaine(OperateurId operateur, DecoupageCalendaire decoupage, Instant evaluation) {
    return activites
      .recouvrant(operateur, decoupage.debut(), decoupage.finExclusive())
      .stream()
      .map(activite -> activite.a(evaluation))
      .flatMap(intervalle -> decoupage.intervalles(intervalle, evaluation).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }

  private static List<JourDeLaSemaine> jours(DecoupageCalendaire decoupage, List<IntervalleDUnJour> travail) {
    Map<LocalDate, List<IntervalleDActivite>> travailParJour = travail
      .stream()
      .collect(Collectors.groupingBy(IntervalleDUnJour::jour, Collectors.mapping(IntervalleDUnJour::intervalle, Collectors.toList())));

    return decoupage
      .jours()
      .stream()
      .map(jour -> new JourDeLaSemaine(jour, travailParJour.getOrDefault(jour, List.of())))
      .toList();
  }

  public interface FeuillesDeTempsServiceOperateursBuilder {
    FeuillesDeTempsServiceFuseauBuilder operateurs(OperateursConnus operateurs);
  }

  public interface FeuillesDeTempsServiceFuseauBuilder {
    FeuillesDeTempsServiceActivitesBuilder fuseau(FuseauHoraireDeLEntreprise fuseau);
  }

  public interface FeuillesDeTempsServiceActivitesBuilder {
    FeuillesDeTempsServiceClockBuilder activites(ActivitesDeLOperateur activites);
  }

  public interface FeuillesDeTempsServiceClockBuilder {
    FeuillesDeTempsService clock(Clock clock);
  }
}
