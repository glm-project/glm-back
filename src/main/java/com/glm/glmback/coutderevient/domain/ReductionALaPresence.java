package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le temps brut d'atelier ramene aux fenetres de presence de ses operateurs.
 *
 * <p>
 * Meme regle que celle de l'atelier, rejouee ici : chaque intervalle est intersecte avec les fenetres de
 * <strong>la journee ou il a commence</strong>. Une pause de midi le scinde, un depart referme ce que l'operateur a
 * oublie d'arreter, et une regularisation de depart corrige d'un coup tous les elements de la journee.
 * </p>
 *
 * <p>
 * Borner l'intervalle a sa journee est aussi ce qui empeche un travail jamais arrete de courir jusqu'au lendemain :
 * l'operateur reclique sur l'element a son retour. Un debut qui ne tombe dans aucune journee connue est rendu
 * <strong>intact</strong> — c'est la presence qui manque, et le domaine ne masque pas l'anomalie derriere un temps
 * ampute.
 * </p>
 */
public record ReductionALaPresence(Map<OperateurId, PresenceDUnOperateur> parOperateur) {
  public ReductionALaPresence {
    Assert.notNull("presence par operateur", parOperateur);
  }

  public static ReductionALaPresence de(List<PresenceDUnOperateur> presences) {
    return new ReductionALaPresence(presences.stream().collect(Collectors.toMap(PresenceDUnOperateur::operateur, Function.identity())));
  }

  public List<IntervalleDActivite> reduit(IntervalleDActivite intervalle) {
    return Optional.ofNullable(parOperateur.get(intervalle.operateur()))
      .flatMap(presence -> presence.journeeContenant(intervalle.plage().debut()))
      .map(journee -> reduit(intervalle, journee))
      .orElseGet(() -> List.of(intervalle));
  }

  private static List<IntervalleDActivite> reduit(IntervalleDActivite intervalle, JourneeDeTravail journee) {
    return journee
      .fenetres()
      .stream()
      .flatMap(fenetre -> intervalle.reduitA(fenetre).stream())
      .toList();
  }
}
