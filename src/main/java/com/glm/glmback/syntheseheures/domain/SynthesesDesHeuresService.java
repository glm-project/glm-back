package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assemble le releve hebdomadaire d'un operateur a partir des journaux de l'atelier.
 *
 * <p>
 * Rien n'est stocke, tout est recalcule : les journees qui recouvrent la semaine sont repliees en pointages et
 * fenetres de presence, puis chacun est ramene aux jours du calendrier qu'il traverse. Un pointage fautif n'empeche
 * jamais la lecture — voir {@link JourneeDeTravail}, dont le repli est tolerant.
 * </p>
 */
public final class SynthesesDesHeuresService {

  private static final Comparator<EvenementDePresence> PAR_HEURE = Comparator.comparing(EvenementDePresence::dateDeSurvenue).thenComparing(
    EvenementDePresence::type
  );

  private final PresenceDeLOperateur presences;
  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;
  private final SeuilDAmplitude seuil;
  private final PointagesDAtelier pointages;
  private final Clock clock;

  private SynthesesDesHeuresService(
    PresenceDeLOperateur presences,
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    SeuilDAmplitude seuil,
    PointagesDAtelier pointages,
    Clock clock
  ) {
    this.presences = presences;
    this.operateurs = operateurs;
    this.fuseau = fuseau;
    this.seuil = seuil;
    this.pointages = pointages;
    this.clock = clock;
  }

  public static SynthesesDesHeuresServicePresencesBuilder builder() {
    return presences ->
      operateurs ->
        fuseau -> seuil -> pointages -> clock -> new SynthesesDesHeuresService(presences, operateurs, fuseau, seuil, pointages, clock);
  }

  public SyntheseDesHeures synthese(OperateurId operateur, SemaineCalendaire semaine) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());
    Instant maintenant = clock.now();
    AmplitudeMaximale amplitude = seuil.amplitudeMaximale();
    List<JourneeDeTravail> journees = presences
      .journeesRecouvrant(operateur, decoupage.debut(), decoupage.finExclusive())
      .stream()
      .map(journee -> lue(operateur, journee, maintenant, amplitude))
      .toList();

    Map<LocalDate, List<EvenementDePresence>> pointagesParJour = pointagesParJour(journees, decoupage);
    Map<LocalDate, Duration> dureeParJour = dureeParJour(journees, decoupage, false);
    Map<LocalDate, Duration> dureePresumeeParJour = dureeParJour(journees, decoupage, true);

    return new SyntheseDesHeures(connu, semaine, jours(decoupage, pointagesParJour, dureeParJour, dureePresumeeParJour));
  }

  private Map<LocalDate, List<EvenementDePresence>> pointagesParJour(List<JourneeDeTravail> journees, DecoupageCalendaire decoupage) {
    return journees
      .stream()
      .flatMap(journee -> journee.pointages().stream())
      .filter(evenement -> decoupage.jours().contains(jourDe(evenement)))
      .sorted(PAR_HEURE)
      .collect(Collectors.groupingBy(this::jourDe));
  }

  /**
   * La duree pointee ou presumee de chaque jour : seules les plages closes comptent, une journee encore en cours ne
   * contribuant rien tant qu'elle n'est ni fermee ni abandonnee.
   */
  private Map<LocalDate, Duration> dureeParJour(List<JourneeDeTravail> journees, DecoupageCalendaire decoupage, boolean presumee) {
    return journees
      .stream()
      .flatMap(journee -> journee.fenetres().stream())
      .flatMap(fenetre -> decoupage.plages(fenetre).stream())
      .filter(plageDUnJour -> !plageDUnJour.plage().estOuverte())
      .filter(plageDUnJour -> plageDUnJour.plage().presumee() == presumee)
      .collect(Collectors.groupingBy(PlageDUnJour::jour, Collectors.reducing(Duration.ZERO, this::duree, Duration::plus)));
  }

  private Duration duree(PlageDUnJour plageDUnJour) {
    return Duration.between(plageDUnJour.plage().debut(), plageDUnJour.plage().fin().orElseThrow());
  }

  private LocalDate jourDe(EvenementDePresence evenement) {
    return LocalDate.ofInstant(evenement.dateDeSurvenue(), fuseau.zone());
  }

  /**
   * La journee telle qu'on la lit maintenant : une journee abandonnee est fermee a sa fin presumee, et le dernier
   * pointage d'OF de l'operateur n'est demande que pour elle.
   */
  private JourneeDeTravail lue(OperateurId operateur, JourneeDeTravail journee, Instant maintenant, AmplitudeMaximale amplitude) {
    if (!journee.estAbandonneePour(maintenant, amplitude)) {
      return journee;
    }

    return journee.presumee(amplitude, pointages.dernierPointage(operateur, journee.fenetreDeRecherche(amplitude).orElseThrow()));
  }

  /**
   * Les sept jours, dans l'ordre, chacun portant ce qui lui revient — vide compris.
   */
  private static List<JourDeSynthese> jours(
    DecoupageCalendaire decoupage,
    Map<LocalDate, List<EvenementDePresence>> pointagesParJour,
    Map<LocalDate, Duration> dureeParJour,
    Map<LocalDate, Duration> dureePresumeeParJour
  ) {
    return decoupage
      .jours()
      .stream()
      .map(jour ->
        new JourDeSynthese(
          jour,
          pointagesParJour.getOrDefault(jour, List.of()),
          dureeParJour.getOrDefault(jour, Duration.ZERO),
          dureePresumeeParJour.getOrDefault(jour, Duration.ZERO)
        )
      )
      .toList();
  }

  public interface SynthesesDesHeuresServicePresencesBuilder {
    SynthesesDesHeuresServiceOperateursBuilder presences(PresenceDeLOperateur presences);
  }

  public interface SynthesesDesHeuresServiceOperateursBuilder {
    SynthesesDesHeuresServiceFuseauBuilder operateurs(OperateursConnus operateurs);
  }

  public interface SynthesesDesHeuresServiceFuseauBuilder {
    SynthesesDesHeuresServiceSeuilBuilder fuseau(FuseauHoraireDeLEntreprise fuseau);
  }

  public interface SynthesesDesHeuresServiceSeuilBuilder {
    SynthesesDesHeuresServicePointagesBuilder seuil(SeuilDAmplitude seuil);
  }

  public interface SynthesesDesHeuresServicePointagesBuilder {
    SynthesesDesHeuresServiceClockBuilder pointages(PointagesDAtelier pointages);
  }

  public interface SynthesesDesHeuresServiceClockBuilder {
    SynthesesDesHeuresService clock(Clock clock);
  }
}
