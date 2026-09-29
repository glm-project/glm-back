package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Une semaine vue depuis le fuseau horaire de l'entreprise : ses sept jours, ses bornes en instants, et de quoi
 * ramener n'importe quelle plage aux jours qu'elle traverse.
 *
 * <p>
 * Toute la connaissance du calendrier tient ici. L'atelier ne sait que compter des instants — il n'a ni fuseau ni
 * date — et c'est precisement ce que ce contexte ajoute : minuit n'existe qu'une fois la zone connue, et c'est minuit
 * qui decide a quel jour appartient une heure de travail.
 * </p>
 */
public final class DecoupageCalendaire {

  private static final int JOURS_PAR_SEMAINE = 7;

  private final SemaineCalendaire semaine;
  private final ZoneId zone;

  public DecoupageCalendaire(SemaineCalendaire semaine, ZoneId zone) {
    Assert.notNull("semaine", semaine);
    Assert.notNull("fuseau horaire", zone);

    this.semaine = semaine;
    this.zone = zone;
  }

  public List<LocalDate> jours() {
    return IntStream.range(0, JOURS_PAR_SEMAINE)
      .mapToObj(rang -> semaine.lundi().plusDays(rang))
      .toList();
  }

  public Instant debut() {
    return semaine.lundi().atStartOfDay(zone).toInstant();
  }

  public Instant finExclusive() {
    return semaine.lundi().plusDays(JOURS_PAR_SEMAINE).atStartOfDay(zone).toInstant();
  }

  /**
   * La plage donnee, ramenee a la semaine et scindee a chaque minuit traverse.
   *
   * Une plage sans fin et sans instant d'evaluation n'indique que son jour de debut.
   */
  public List<PlageDUnJour> plages(Plage plage) {
    if (plage.estOuverte()) {
      return surSonSeulJour(plage);
    }

    return scindee(plage);
  }

  /**
   * Une activite terminee est scindee a minuit ; une activite en cours rend une indication sans fin sur chaque jour
   * atteint a l'instant d'evaluation, en conservant ses bornes entieres.
   */
  public List<IntervalleDUnJour> intervalles(IntervalleDActivite intervalle, Instant evaluation) {
    List<PlageDUnJour> portions =
      intervalle.lecture().etat() == EtatDActivite.EN_COURS
        ? ouverteSurChaqueJour(intervalle.plage(), evaluation)
        : plages(intervalle.plage());
    return portions
      .stream()
      .map(plage -> new IntervalleDUnJour(plage.jour(), intervalle.sur(plage.plage())))
      .toList();
  }

  private List<PlageDUnJour> ouverteSurChaqueJour(Plage plage, Instant evaluation) {
    LocalDate premierJour = jourDe(plage.debut());
    LocalDate dernierJour = jourDe(evaluation.isAfter(plage.debut()) ? evaluation : plage.debut());
    return jours()
      .stream()
      .filter(jour -> !jour.isBefore(premierJour) && !jour.isAfter(dernierJour))
      .map(jour ->
        new PlageDUnJour(jour, new Plage(jour.equals(premierJour) ? plage.debut() : jour.atStartOfDay(zone).toInstant(), Optional.empty()))
      )
      .toList();
  }

  private List<PlageDUnJour> surSonSeulJour(Plage plage) {
    LocalDate jour = jourDe(plage.debut());

    if (!jours().contains(jour)) {
      return List.of();
    }

    return List.of(new PlageDUnJour(jour, plage));
  }

  private List<PlageDUnJour> scindee(Plage plage) {
    Instant debutRetenu = plage.debut().isAfter(debut()) ? plage.debut() : debut();
    Instant finRetenue = plage.fin().filter(finExclusive()::isAfter).orElseGet(this::finExclusive);

    if (!finRetenue.isAfter(debutRetenu)) {
      return List.of();
    }

    List<PlageDUnJour> plages = new ArrayList<>();
    LocalDate jour = jourDe(debutRetenu);
    Instant curseur = debutRetenu;

    while (curseur.isBefore(finRetenue)) {
      Instant minuitSuivant = jour.plusDays(1).atStartOfDay(zone).toInstant();
      Instant borne = minuitSuivant.isBefore(finRetenue) ? minuitSuivant : finRetenue;
      plages.add(new PlageDUnJour(jour, new Plage(curseur, Optional.of(borne))));
      curseur = borne;
      jour = jour.plusDays(1);
    }

    return List.copyOf(plages);
  }

  private LocalDate jourDe(Instant instant) {
    return LocalDate.ofInstant(instant, zone);
  }
}
