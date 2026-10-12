package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * La premiere lecture du journal : les activites que les gestes donnent, et la fin de chacune.
 *
 * <p>
 * Un debut ou une non conformite ouvre une activite. Une fin <b>cible</b> l'activite qu'elle ferme quand le gestionnaire
 * l'a ecrite (regularisation d'une fin automatique, fin d'un ajout) : elle la ferme a son heure, quelle que soit la cle
 * sur laquelle elle est ecrite. Une fin <b>pointee</b> ne designe rien : elle ferme l'activite que la cle avait en cours
 * quand le serveur l'a recue. La cloture, a defaut, ferme l'activite restee en cours.
 * </p>
 *
 * <p>
 * Pour rattacher une fin pointee a son activite, les pointages d'une meme cle se rejouent dans l'ordre ou le serveur les
 * a recus (date d'enregistrement, puis l'ordre du journal), et non dans l'ordre des heures de geste : la regle de reception
 * juge chaque pointage sur le journal tel qu'il etait, et un retrait ou un changement de poste libere une cle sur laquelle
 * un pointage peut ensuite tomber dans la plage d'une activite qui n'y compte plus. Rejoue par heure de geste, ce pointage
 * volerait la fin pointee de cette activite. Les activites retirees ou deplacees restent donc dans cette lecture : elle
 * ne sert qu'a lier les fins, les corrections s'appliquent apres elle (voir {@link JournalDAtelier#activites}). Seuls
 * les pointages y entrent ; l'ouverture d'un ajout n'est jamais une activite en cours de sa cle.
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
 * Rien n'est jamais refuse ici. Un fait sans activite a fermer, ou qui cible une activite inconnue, n'a simplement aucun
 * effet.
 * </p>
 */
final class SequenceDActivites {

  private static final Comparator<EvenementDAtelier> PAR_ORDRE_DE_RECEPTION = Comparator.comparing(
    EvenementDAtelier::dateDEnregistrement
  ).thenComparing(JournalDAtelier.PAR_ORDRE_CHRONOLOGIQUE);

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
    pointages.stream().sorted(PAR_ORDRE_DE_RECEPTION).forEach(this::lit);
    cloture.ifPresent(this::clot);
  }

  private void lit(EvenementDAtelier pointage) {
    atteintLEcheance(pointage.dateDeSurvenue());
    if (pointage.type().ouvreUneActivite()) {
      courante = pointage.activite();
    } else {
      termine(courante, pointage);
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
    courante.ifPresent(identite ->
      activites.computeIfPresent(identite, (id, ouverte) -> ouverte.fin().isEmpty() ? ouverte.termineeA(date) : ouverte)
    );
  }

  private void termine(Optional<ActiviteId> activite, EvenementDAtelier fin) {
    activite.ifPresent(identite -> activites.computeIfPresent(identite, (id, ouverte) -> ouverte.termineePar(fin)));
  }
}
