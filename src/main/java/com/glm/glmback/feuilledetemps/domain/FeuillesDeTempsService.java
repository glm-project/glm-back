package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assemble l'historique hebdomadaire d'un operateur a partir des journaux de l'atelier.
 *
 * <p>
 * Rien n'est stocke, tout est recalcule : les journees qui recouvrent la semaine sont repliees en fenetres de
 * presence, puis chaque fenetre est ramenee aux jours du calendrier qu'elle traverse. C'est la seule etape ou le
 * fuseau horaire intervient, et c'est ce qui separe ce contexte de l'atelier, qui ne compte que des instants.
 * </p>
 *
 * <p>
 * Le travail est lu sans condition de presence, puis coupe aux memes minuits.
 * </p>
 */
public final class FeuillesDeTempsService {

  private static final Comparator<PlageDUnJour> PAR_HEURE = Comparator.comparing(plage -> plage.plage().debut());

  /**
   * Deux debuts simultanes se departagent par l'element : l'ordre des suivis rendus par le port ne doit rien decider.
   */
  private static final Comparator<IntervalleDUnJour> PAR_DEBUT = Comparator.<IntervalleDUnJour, Instant>comparing(intervalle ->
    intervalle.intervalle().plage().debut()
  )
    .thenComparing(intervalle -> intervalle.intervalle().activite().element().uuid())
    .thenComparing(intervalle -> intervalle.intervalle().lecture().id().uuid());

  private final PresenceDeLOperateur presences;
  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;
  private final SeuilDAmplitude seuil;
  private final PointagesDAtelier pointages;
  private final ActivitesDeLOperateur activites;
  private final Clock clock;

  private FeuillesDeTempsService(
    PresenceDeLOperateur presences,
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    SeuilDAmplitude seuil,
    PointagesDAtelier pointages,
    ActivitesDeLOperateur activites,
    Clock clock
  ) {
    this.presences = presences;
    this.operateurs = operateurs;
    this.fuseau = fuseau;
    this.seuil = seuil;
    this.pointages = pointages;
    this.activites = activites;
    this.clock = clock;
  }

  public static FeuillesDeTempsServicePresencesBuilder builder() {
    return presences ->
      operateurs ->
        fuseau ->
          seuil ->
            pointages ->
              activites -> clock -> new FeuillesDeTempsService(presences, operateurs, fuseau, seuil, pointages, activites, clock);
  }

  public FeuilleDeTemps historique(OperateurId operateur, SemaineCalendaire semaine) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());
    Instant evaluation = clock.now();
    List<JourneeDeTravail> journees = journeesDeLaSemaine(operateur, decoupage, evaluation);

    return new FeuilleDeTemps(
      connu,
      semaine,
      jours(decoupage, presenceDeLaSemaine(journees, decoupage), travailDeLaSemaine(operateur, decoupage, evaluation))
    );
  }

  private List<JourneeDeTravail> journeesDeLaSemaine(OperateurId operateur, DecoupageCalendaire decoupage, Instant maintenant) {
    AmplitudeMaximale amplitude = seuil.amplitudeMaximale();

    return presences
      .journeesRecouvrant(operateur, decoupage.debut(), decoupage.finExclusive())
      .stream()
      .map(journee -> lue(operateur, journee, maintenant, amplitude))
      .toList();
  }

  private static List<PlageDUnJour> presenceDeLaSemaine(List<JourneeDeTravail> journees, DecoupageCalendaire decoupage) {
    return journees
      .stream()
      .flatMap(journee -> journee.fenetres().stream())
      .flatMap(fenetre -> decoupage.plages(fenetre).stream())
      .sorted(PAR_HEURE)
      .toList();
  }

  private List<IntervalleDUnJour> travailDeLaSemaine(OperateurId operateur, DecoupageCalendaire decoupage, Instant evaluation) {
    return activites
      .recouvrant(operateur, decoupage.debut(), decoupage.finExclusive())
      .stream()
      .map(activite -> activite.a(evaluation))
      .flatMap(intervalle -> decoupage.intervalles(intervalle).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }

  /**
   * La journee telle qu'on la lit maintenant : une journee abandonnee, ou fermee plus de 24 h apres son arrivee, est
   * fermee a sa fin presumee, et le dernier pointage d'OF de l'operateur n'est demande que pour elle.
   */
  private JourneeDeTravail lue(OperateurId operateur, JourneeDeTravail journee, Instant maintenant, AmplitudeMaximale amplitude) {
    if (!journee.estPresumeePour(maintenant, amplitude)) {
      return journee;
    }

    return journee.presumee(amplitude, pointages.dernierPointage(operateur, journee.fenetreDeRecherche(amplitude).orElseThrow()));
  }

  /**
   * Les sept jours, dans l'ordre, chacun portant ce qui lui revient — vide compris.
   */
  private static List<JourDeLaSemaine> jours(DecoupageCalendaire decoupage, List<PlageDUnJour> presence, List<IntervalleDUnJour> travail) {
    Map<LocalDate, List<Plage>> presenceParJour = presence
      .stream()
      .collect(Collectors.groupingBy(PlageDUnJour::jour, Collectors.mapping(PlageDUnJour::plage, Collectors.toList())));
    Map<LocalDate, List<IntervalleDActivite>> travailParJour = travail
      .stream()
      .collect(Collectors.groupingBy(IntervalleDUnJour::jour, Collectors.mapping(IntervalleDUnJour::intervalle, Collectors.toList())));

    return decoupage
      .jours()
      .stream()
      .map(jour -> new JourDeLaSemaine(jour, presenceParJour.getOrDefault(jour, List.of()), travailParJour.getOrDefault(jour, List.of())))
      .toList();
  }

  public interface FeuillesDeTempsServicePresencesBuilder {
    FeuillesDeTempsServiceOperateursBuilder presences(PresenceDeLOperateur presences);
  }

  public interface FeuillesDeTempsServiceOperateursBuilder {
    FeuillesDeTempsServiceFuseauBuilder operateurs(OperateursConnus operateurs);
  }

  public interface FeuillesDeTempsServiceFuseauBuilder {
    FeuillesDeTempsServiceSeuilBuilder fuseau(FuseauHoraireDeLEntreprise fuseau);
  }

  public interface FeuillesDeTempsServiceSeuilBuilder {
    FeuillesDeTempsServicePointagesBuilder seuil(SeuilDAmplitude seuil);
  }

  public interface FeuillesDeTempsServicePointagesBuilder {
    FeuillesDeTempsServiceTravailBuilder pointages(PointagesDAtelier pointages);
  }

  public interface FeuillesDeTempsServiceTravailBuilder {
    FeuillesDeTempsServiceClockBuilder activites(ActivitesDeLOperateur activites);
  }

  public interface FeuillesDeTempsServiceClockBuilder {
    FeuillesDeTempsService clock(Clock clock);
  }
}
