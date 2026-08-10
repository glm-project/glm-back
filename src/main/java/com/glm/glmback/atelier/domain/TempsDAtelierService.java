package com.glm.glmback.atelier.domain;

import java.util.List;
import java.util.Optional;

/**
 * Le temps reellement passe sur un element : ses intervalles bruts, ramenes aux fenetres de presence des operateurs.
 *
 * <p>
 * Une pause de midi scinde le travail en deux, un depart referme ce que l'operateur a oublie d'arreter, et une
 * regularisation de depart corrige d'un coup tous les elements de la journee. Aucun de ces trois faits n'a eu besoin
 * d'etre recopie dans le journal de l'element : c'est ce que le croisement des deux journaux fait gratuitement, et ce
 * qu'une pause dupliquee element par element n'aurait jamais su rattraper apres coup.
 * </p>
 */
public final class TempsDAtelierService {

  private final SuiviDAtelierRepository suivis;
  private final JourneeDeTravailRepository journees;

  public TempsDAtelierService(SuiviDAtelierRepository suivis, JourneeDeTravailRepository journees) {
    this.suivis = suivis;
    this.journees = journees;
  }

  public List<IntervalleDActivite> tempsEffectif(SuiviDAtelierId id) {
    SuiviDAtelier suivi = suivis.get(id).orElseThrow(() -> new SuiviDAtelierIntrouvableException(id));

    return suivi
      .activites()
      .stream()
      .flatMap(intervalle -> effectif(intervalle).stream())
      .toList();
  }

  /**
   * Un intervalle est borne par la journee ou il a commence : c'est ce qui empeche un travail jamais arrete de courir
   * jusqu'au lendemain, l'operateur devant recliquer sur l'element a son retour.
   *
   * <p>
   * Un debut qui ne tombe dans aucune journee connue est rendu intact : c'est la presence qui manque, et le domaine ne
   * masque pas l'anomalie derriere un temps ampute.
   * </p>
   */
  private List<IntervalleDActivite> effectif(IntervalleDActivite intervalle) {
    Optional<JourneeDeTravail> journee = journees.journeeContenant(intervalle.operateur(), intervalle.debut());
    if (journee.isEmpty()) {
      return List.of(intervalle);
    }

    return journee
      .orElseThrow()
      .fenetres()
      .stream()
      .flatMap(fenetre -> intervalle.reduitA(fenetre).stream())
      .toList();
  }
}
