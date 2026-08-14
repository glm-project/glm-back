package com.glm.glmback.feuilledetemps.domain;

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
 */
public final class FeuillesDeTempsService {

  private static final Comparator<PlageDUnJour> PAR_HEURE = Comparator.comparing(plage -> plage.plage().debut());

  private final PresenceDeLOperateur presences;
  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;

  public FeuillesDeTempsService(PresenceDeLOperateur presences, OperateursConnus operateurs, FuseauHoraireDeLEntreprise fuseau) {
    this.presences = presences;
    this.operateurs = operateurs;
    this.fuseau = fuseau;
  }

  public FeuilleDeTemps historique(OperateurId operateur, SemaineCalendaire semaine) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());

    return new FeuilleDeTemps(connu, semaine, jours(decoupage, presenceDeLaSemaine(operateur, decoupage)));
  }

  private List<PlageDUnJour> presenceDeLaSemaine(OperateurId operateur, DecoupageCalendaire decoupage) {
    return presences
      .journeesRecouvrant(operateur, decoupage.debut(), decoupage.finExclusive())
      .stream()
      .flatMap(journee -> journee.fenetres().stream())
      .flatMap(fenetre -> decoupage.plages(fenetre).stream())
      .sorted(PAR_HEURE)
      .toList();
  }

  /**
   * Les sept jours, dans l'ordre, chacun portant ce qui lui revient — vide compris.
   */
  private static List<JourDeLaSemaine> jours(DecoupageCalendaire decoupage, List<PlageDUnJour> presence) {
    Map<LocalDate, List<Plage>> parJour = presence
      .stream()
      .collect(Collectors.groupingBy(PlageDUnJour::jour, Collectors.mapping(PlageDUnJour::plage, Collectors.toList())));

    return decoupage
      .jours()
      .stream()
      .map(jour -> new JourDeLaSemaine(jour, parJour.getOrDefault(jour, List.of())))
      .toList();
  }
}
