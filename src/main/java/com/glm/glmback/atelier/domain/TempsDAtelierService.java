package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Le temps reellement passe sur un element : ses intervalles bruts, ramenes aux fenetres de presence des operateurs.
 *
 * <p>
 * Un depart referme ce que l'operateur a oublie d'arreter, et une regularisation de depart corrige d'un coup tous les
 * elements de la journee. Aucun de ces deux faits n'a eu besoin d'etre recopie dans le journal de l'element : c'est
 * ce que le croisement des deux journaux fait gratuitement. La pause de midi, elle, se lit dans le journal de
 * l'element, ou le pupitre l'a pointee par une fin et un debut.
 * </p>
 *
 * <p>
 * L'instant d'evaluation vient de l'appelant : ce service n'a pas d'horloge, et tout ce qui depend de l'heure de la
 * lecture se juge sur cet instant-la.
 * </p>
 */
public final class TempsDAtelierService {

  private final SuiviDAtelierRepository suivis;
  private final JourneeDeTravailRepository journees;
  private final SeuilDAmplitude seuil;

  private TempsDAtelierService(SuiviDAtelierRepository suivis, JourneeDeTravailRepository journees, SeuilDAmplitude seuil) {
    this.suivis = suivis;
    this.journees = journees;
    this.seuil = seuil;
  }

  public static TempsDAtelierServiceSuivisBuilder builder() {
    return suivis -> journees -> seuil -> new TempsDAtelierService(suivis, journees, seuil);
  }

  public List<IntervalleDActivite> tempsEffectif(SuiviDAtelierId id, Instant evaluation) {
    SuiviDAtelier suivi = suivis.get(id).orElseThrow(() -> new SuiviDAtelierIntrouvableException(id));

    return suivi
      .activites()
      .stream()
      .flatMap(intervalle -> effectif(intervalle, evaluation).stream())
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
  private List<IntervalleDActivite> effectif(IntervalleDActivite intervalle, Instant evaluation) {
    Optional<JourneeDeTravail> journee = journees.journeeContenant(intervalle.operateur(), intervalle.debut());
    if (journee.isEmpty()) {
      return List.of(intervalle);
    }

    return fenetres(journee.orElseThrow(), evaluation)
      .stream()
      .flatMap(fenetre -> intervalle.reduitA(fenetre).stream())
      .toList();
  }

  /**
   * Le dernier pointage d'OF de l'operateur n'est cherche que pour une journee abandonnee, ou fermee plus de 24 h apres
   * son arrivee : les seules qui aient besoin d'une fin presumee.
   */
  private List<FenetreDePresence> fenetres(JourneeDeTravail journee, Instant evaluation) {
    AmplitudeMaximale amplitude = seuil.amplitudeMaximale();

    if (!journee.estPresumeePour(evaluation, amplitude)) {
      return journee.fenetres();
    }

    Optional<Instant> dernierPointage = journee
      .fenetreDeRecherche(amplitude)
      .flatMap(recherche -> suivis.dernierPointageDe(journee.operateur(), recherche));

    return journee.fenetresA(evaluation, amplitude, dernierPointage);
  }

  public interface TempsDAtelierServiceSuivisBuilder {
    TempsDAtelierServiceJourneesBuilder suivis(SuiviDAtelierRepository suivis);
  }

  public interface TempsDAtelierServiceJourneesBuilder {
    TempsDAtelierServiceSeuilBuilder journees(JourneeDeTravailRepository journees);
  }

  public interface TempsDAtelierServiceSeuilBuilder {
    TempsDAtelierService seuil(SeuilDAmplitude seuil);
  }
}
