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
 * rien n'est jamais redevine. La cloture, a defaut, ferme a son heure toute activite que rien n'a terminee,
 * quelle que soit sa cle : elle ne depend d'aucun rejeu.
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
      .forEach(fin -> sequence.termine(fin.activiteVisee(), activite -> activite.termineePar(fin)));
    gestes
      .stream()
      .filter(geste -> geste.activiteVisee().isEmpty() && geste.origine() == OrigineDuPointage.POINTAGE)
      .collect(Collectors.groupingBy(EvenementDAtelier::cle, LinkedHashMap::new, Collectors.toList()))
      .values()
      .forEach(sequence::rejoue);
    cloture.ifPresent(sequence::clot);

    return List.copyOf(sequence.activites.values());
  }

  /**
   * Rattache les fins sans cible d'une cle aux activites que la cle avait en cours : les pointages, rejoues dans l'ordre
   * du journal, celui des heures de geste.
   */
  private void rejoue(List<EvenementDAtelier> pointages) {
    courante = Optional.empty();
    pointages.forEach(this::lit);
  }

  private void lit(EvenementDAtelier pointage) {
    atteintLEcheance(pointage.dateDeSurvenue());
    if (pointage.type().ouvreUneActivite()) {
      courante = pointage.activite();
    } else {
      termine(courante, activite -> activite.termineePar(pointage));
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
   * La cloture ferme a son heure toute activite que rien n'a terminee, commencee avant elle et dont l'echeance n'est pas
   * atteinte : elle ne prolonge jamais une activite echue. Elle ne depend d'aucun rejeu, donc ni de la cle, ni des ouvertures
   * qui ont suivi une correction : la regle de reception ne laisse qu'une activite en cours par cle apres elles.
   */
  private void clot(Instant date) {
    activites.replaceAll((id, activite) ->
      activite.fin().isEmpty() && !activite.debut().isAfter(date) && !activite.echeance().estAtteinteA(date)
        ? activite.termineeA(date)
        : activite
    );
  }

  /**
   * Termine l'activite, sauf si une fin la ferme deja : la premiere fin posee reste la sienne.
   */
  private void termine(Optional<ActiviteId> activite, UnaryOperator<Activite> terminaison) {
    activite.ifPresent(identite ->
      activites.computeIfPresent(identite, (id, ouverte) -> ouverte.fin().isEmpty() ? terminaison.apply(ouverte) : ouverte)
    );
  }
}
