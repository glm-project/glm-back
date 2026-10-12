package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * La premiere lecture du journal : les activites que les gestes donnent, et la fin de chacune.
 *
 * <p>
 * Un debut ou une non conformite ouvre une activite. Une fin <b>cible</b> l'activite qu'elle ferme : la fin d'une
 * regularisation ou d'un ajout, et depuis que le serveur la lie a la reception, la fin pointee, qui porte l'activite que sa
 * cle avait en cours (voir {@link RegleDeReception#activiteQueFermeUneFin}). Une fin ciblee ferme son activite a son heure,
 * quelle que soit la cle sur laquelle elle est ecrite et quelles que soient les ouvertures ou les corrections qui suivent :
 * rien n'est jamais redevine. La cloture, a defaut, ferme l'activite restee en cours.
 * </p>
 *
 * <p>
 * Reste une seule lecture par la cle : celle d'une fin pointee <b>sans cible</b>, ecrite avant que le serveur ne la lie. Elle
 * ferme l'activite en cours de sa cle, les pointages de la cle etant rejoues dans l'ordre du journal, celui des heures de
 * geste, comme toujours. Un journal qui n'a jamais porte de correction est lu ainsi sans aucun ecart ; l'ouverture d'un
 * ajout n'y entre jamais, puisque sa fin est ciblee.
 * </p>
 *
 * <p>
 * L'echeance se lit sans aucun instant de lecture, sur les seules heures metier : une activite que rien n'a terminee cesse
 * d'etre en cours des que l'heure d'un fait, ou de la cloture, atteint son echeance. Elle garde alors sa borne
 * automatique, que seule une fin regularisee peut deplacer ; ni la cloture ni une ouverture ne la prolongent. Un fait
 * pile a l'echeance la trouve donc deja atteinte : un debut a cette heure ouvre une activite apres une fin automatique.
 * </p>
 *
 * <p>
 * Rien n'est jamais refuse ici, et une fin ne remplace jamais une fin deja posee. Un fait sans activite a fermer, ou qui
 * cible une activite inconnue, n'a simplement aucun effet.
 * </p>
 */
final class SequenceDActivites {

  private final Map<ActiviteId, Activite> activites = new LinkedHashMap<>();
  private Optional<ActiviteId> courante = Optional.empty();

  private SequenceDActivites() {}

  /**
   * Les activites que les gestes donnent, dans l'ordre de leur ouverture au journal. Les gestes arrivent dans l'ordre du
   * journal.
   */
  static List<Activite> activites(List<EvenementDAtelier> gestes, Optional<Instant> cloture) {
    SequenceDActivites sequence = new SequenceDActivites();
    gestes
      .stream()
      .filter(geste -> geste.type().ouvreUneActivite())
      .forEach(ouverture -> sequence.activites.put(ouverture.activite().orElseThrow(), Activite.ouvertePar(ouverture)));
    gestes
      .stream()
      .filter(geste -> geste.activiteVisee().isPresent())
      .forEach(fin -> sequence.termine(fin.activiteVisee(), fin));
    gestes
      .stream()
      .filter(geste -> geste.activiteVisee().isEmpty() && geste.origine() == OrigineDuPointage.POINTAGE)
      .collect(Collectors.groupingBy(EvenementDAtelier::cle, LinkedHashMap::new, Collectors.toList()))
      .values()
      .forEach(pointages -> sequence.rejoue(pointages, cloture));

    return List.copyOf(sequence.activites.values());
  }

  private void rejoue(List<EvenementDAtelier> pointages, Optional<Instant> cloture) {
    courante = Optional.empty();
    pointages.forEach(this::lit);
    cloture.ifPresent(this::clot);
  }

  private void lit(EvenementDAtelier pointage) {
    atteintLEcheance(pointage.dateDeSurvenue());
    if (pointage.type().ouvreUneActivite()) {
      courante = pointage.activite();
    } else {
      termineSiEnCours(courante, termineePar(pointage));
      courante = Optional.empty();
    }
  }

  /**
   * L'activite en cours dont l'echeance est atteinte a cette heure n'est plus vivante : elle garde sa borne
   * automatique, sans fin reelle.
   */
  private void atteintLEcheance(Instant heure) {
    courante.filter(activite -> activites.get(activite).echeance().estAtteinteA(heure)).ifPresent(activite -> courante = Optional.empty());
  }

  /**
   * La cloture ferme a son heure l'activite encore vivante ; elle ne prolonge jamais une activite deja echue.
   */
  private void clot(Instant date) {
    atteintLEcheance(date);
    termineSiEnCours(courante, activite -> activite.termineeA(date));
  }

  private void termine(Optional<ActiviteId> activite, EvenementDAtelier fin) {
    activite.ifPresent(identite -> activites.computeIfPresent(identite, (id, ouverte) -> ouverte.termineePar(fin)));
  }

  /**
   * Termine l'activite que la cle avait en cours, sauf si une fin la ferme deja : la premiere fin posee reste la sienne.
   */
  private void termineSiEnCours(Optional<ActiviteId> activite, UnaryOperator<Activite> terminaison) {
    activite.ifPresent(identite ->
      activites.computeIfPresent(identite, (id, ouverte) -> ouverte.fin().isEmpty() ? terminaison.apply(ouverte) : ouverte)
    );
  }

  private static UnaryOperator<Activite> termineePar(EvenementDAtelier fin) {
    return activite -> activite.termineePar(fin);
  }
}
