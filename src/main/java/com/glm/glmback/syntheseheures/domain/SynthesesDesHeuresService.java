package com.glm.glmback.syntheseheures.domain;

import java.time.Duration;
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

  private static final Comparator<EvenementDePresence> PAR_HEURE = Comparator.comparing(EvenementDePresence::dateDeSurvenue);

  private final PresenceDeLOperateur presences;
  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;

  public SynthesesDesHeuresService(PresenceDeLOperateur presences, OperateursConnus operateurs, FuseauHoraireDeLEntreprise fuseau) {
    this.presences = presences;
    this.operateurs = operateurs;
    this.fuseau = fuseau;
  }

  public SyntheseDesHeures synthese(OperateurId operateur, SemaineCalendaire semaine) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());
    List<JourneeDeTravail> journees = presences.journeesRecouvrant(operateur, decoupage.debut(), decoupage.finExclusive());

    Map<LocalDate, List<EvenementDePresence>> pointagesParJour = pointagesParJour(journees, decoupage);
    Map<LocalDate, Duration> dureeParJour = dureeParJour(journees, decoupage);

    return new SyntheseDesHeures(connu, semaine, jours(decoupage, pointagesParJour, dureeParJour));
  }

  private Map<LocalDate, List<EvenementDePresence>> pointagesParJour(List<JourneeDeTravail> journees, DecoupageCalendaire decoupage) {
    return journees
      .stream()
      .flatMap(journee -> journee.pointages().stream())
      .filter(evenement -> decoupage.jours().contains(jourDe(evenement)))
      .sorted(PAR_HEURE)
      .collect(Collectors.groupingBy(this::jourDe));
  }

  private Map<LocalDate, Duration> dureeParJour(List<JourneeDeTravail> journees, DecoupageCalendaire decoupage) {
    return journees
      .stream()
      .flatMap(journee -> journee.fenetres().stream())
      .flatMap(fenetre -> decoupage.plages(fenetre).stream())
      .filter(plageDUnJour -> !plageDUnJour.plage().estOuverte())
      .collect(
        Collectors.groupingBy(
          PlageDUnJour::jour,
          Collectors.reducing(Duration.ZERO, this::duree, Duration::plus)
        )
      );
  }

  private Duration duree(PlageDUnJour plageDUnJour) {
    return Duration.between(plageDUnJour.plage().debut(), plageDUnJour.plage().fin().orElseThrow());
  }

  private LocalDate jourDe(EvenementDePresence evenement) {
    return LocalDate.ofInstant(evenement.dateDeSurvenue(), fuseau.zone());
  }

  /**
   * Les sept jours, dans l'ordre, chacun portant ce qui lui revient — vide compris.
   */
  private static List<JourDeSynthese> jours(
    DecoupageCalendaire decoupage,
    Map<LocalDate, List<EvenementDePresence>> pointagesParJour,
    Map<LocalDate, Duration> dureeParJour
  ) {
    return decoupage
      .jours()
      .stream()
      .map(jour -> new JourDeSynthese(jour, pointagesParJour.getOrDefault(jour, List.of()), dureeParJour.getOrDefault(jour, Duration.ZERO)))
      .toList();
  }
}
