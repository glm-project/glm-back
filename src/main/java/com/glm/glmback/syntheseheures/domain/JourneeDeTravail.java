package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Une venue de l'operateur, et les fenetres de presence qu'on en deduit.
 *
 * <p>
 * Une journee de travail n'est pas un jour du calendrier : elle va d'une arrivee a un depart, et peut passer minuit.
 * C'est le decoupage calendaire, plus tard, qui la ramene aux jours de la semaine ; ici, aucune date, seulement des
 * instants — exactement comme dans l'atelier d'ou ces evenements viennent.
 * </p>
 *
 * <p>
 * <strong>Le repli est tolerant, a la difference de son jumeau {@code feuilledetemps}</strong> : un pointage qui ne
 * s'enchaine pas selon l'automate de presence ne fait jamais echouer la lecture. Le releve doit rester genere quoi
 * qu'il arrive — c'est le gestionnaire qui corrigera le journal depuis l'ecran d'atelier, pas ce contexte qui
 * refusera de produire une synthese. Le pointage fautif reste visible dans {@link #pointages()}, marque invalide, et
 * ignore par {@link #fenetres()}.
 * </p>
 */
public record JourneeDeTravail(List<EvenementDePresence> journal) {
  private static final Comparator<EvenementDePresence> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDePresence::dateDeSurvenue);

  public JourneeDeTravail {
    Assert.field("journal", journal).notNull().noNullElement();
    journal = journal.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
  }

  /**
   * Tous les pointages du journal, valides ou non, dans l'ordre chronologique.
   */
  public List<Pointage> pointages() {
    return repli().pointages();
  }

  /**
   * Les intervalles ou l'operateur etait present et non en pause, dans l'ordre — construits uniquement a partir des
   * pointages valides.
   */
  public List<Plage> fenetres() {
    return repli().fenetres();
  }

  private Repli repli() {
    List<Pointage> pointages = new ArrayList<>();
    List<Plage> fenetres = new ArrayList<>();
    EtatDePresence etat = EtatDePresence.ABSENT;
    Instant debutFenetre = null;

    for (EvenementDePresence evenement : journal) {
      Optional<EtatDePresence> apres = etat.apres(evenement.type());
      pointages.add(new Pointage(evenement, apres.isPresent()));

      if (apres.isEmpty()) {
        continue;
      }

      EtatDePresence nouvelEtat = apres.get();
      // L'automate ne mappe jamais PRESENT sur PRESENT (voir EtatDePresence) : verifier l'etat d'arrivee suffit,
      // sans re-verifier l'etat de depart en plus — les deux conditions ne peuvent jamais etre vraies ensemble.
      if (nouvelEtat == EtatDePresence.PRESENT) {
        debutFenetre = evenement.dateDeSurvenue();
      } else if (etat == EtatDePresence.PRESENT) {
        fenetres.add(new Plage(debutFenetre, Optional.of(evenement.dateDeSurvenue())));
      }
      etat = nouvelEtat;
    }

    if (etat == EtatDePresence.PRESENT) {
      fenetres.add(new Plage(debutFenetre, Optional.empty()));
    }

    return new Repli(List.copyOf(pointages), List.copyOf(fenetres));
  }

  private record Repli(List<Pointage> pointages, List<Plage> fenetres) {}
}
