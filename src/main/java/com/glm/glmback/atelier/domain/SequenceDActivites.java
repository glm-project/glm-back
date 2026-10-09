package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La lecture des faits d'une meme cle d'activite — un operateur sur un poste de travail, pour un element engage : la
 * seule interpretation du journal.
 *
 * <p>
 * Les faits arrivent dans l'ordre du journal. Cette lecture suppose qu'au plus une activite est en cours sur la cle :
 * c'est la regle de reception qui le garantit, en ignorant tout debut ou toute non conformite pointe pendant une
 * activite en cours. Un debut ou une non conformite ouvre une activite. Une fin pointee ferme l'activite en cours de la cle ; une fin regularisee ferme celle
 * qu'elle cible. La cloture, a defaut, ferme l'activite restee en cours.
 * </p>
 *
 * <p>
 * L'echeance se lit sans aucun instant de lecture, sur les seules heures metier : une activite que rien n'a terminee
 * cesse d'etre en cours des que l'heure d'un fait, ou de la cloture, atteint son echeance. Elle garde alors sa borne
 * automatique, que seule une fin regularisee peut deplacer ; ni la cloture ni une ouverture ne la prolongent. Un fait
 * pile a l'echeance la trouve donc deja atteinte : un debut a cette heure ouvre une activite apres une fin automatique.
 * </p>
 *
 * <p>
 * Rien n'est jamais refuse ici. La regle de reception n'a laisse entrer au journal que des faits qui s'accordent : un
 * fait sans activite a fermer, ou qui cible une activite inconnue de la cle, n'a simplement aucun effet.
 * </p>
 */
final class SequenceDActivites {

  private final Map<ActiviteId, Activite> activites = new LinkedHashMap<>();
  private Optional<ActiviteId> courante = Optional.empty();

  private SequenceDActivites() {}

  /**
   * Les activites que les faits de la cle donnent, dans l'ordre de leur ouverture.
   */
  static List<Activite> activites(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    SequenceDActivites sequence = new SequenceDActivites();
    faits.forEach(sequence::lit);
    cloture.ifPresent(sequence::clot);

    return List.copyOf(sequence.activites.values());
  }

  private void lit(EvenementDAtelier fait) {
    atteintLEcheance(fait.dateDeSurvenue());
    if (fait.type().ouvreUneActivite()) {
      Activite ouverte = Activite.ouvertePar(fait);
      activites.put(ouverte.id(), ouverte);
      courante = Optional.of(ouverte.id());
    } else {
      arrete(fait);
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
   * Une fin ferme a son heure l'activite qu'elle cible si elle en porte une — c'est la fin regularisee —, sinon
   * l'activite en cours de la cle.
   */
  private void arrete(EvenementDAtelier fin) {
    Optional<ActiviteId> terminee = fin.activiteVisee().or(() -> courante);
    termine(terminee, fin.dateDeSurvenue());
    if (courante.equals(terminee)) {
      courante = Optional.empty();
    }
  }

  /**
   * La cloture ferme a son heure l'activite encore vivante ; elle ne prolonge jamais une activite deja echue.
   */
  private void clot(Instant date) {
    atteintLEcheance(date);
    termine(courante, date);
  }

  private void termine(Optional<ActiviteId> activite, Instant heure) {
    activite.ifPresent(identite -> activites.computeIfPresent(identite, (id, ouverte) -> ouverte.termineeA(heure)));
  }
}
