package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ce que la presence et l'atelier font des pointages signales : les inscrire quand un geste inhabituel est enregistre
 * malgre tout, et les resoudre quand le gestionnaire annule ou corrige l'evenement qui les porte.
 */
final class RegistreDesSignalements {

  private final PointagesSignales signalements;
  private final Clock clock;

  RegistreDesSignalements(PointagesSignales signalements, Clock clock) {
    this.signalements = signalements;
    this.clock = clock;
  }

  void signale(UUID evenement, CibleDuSignalement cible, OperateurId operateur, DateRedressee date) {
    if (date.motifs().isEmpty()) {
      return;
    }

    signalements.create(
      PointageSignale.builder()
        .id(new PointageSignaleId(evenement))
        .cible(cible)
        .operateur(operateur)
        .motifs(date.motifs())
        .horodatage(date.horodatage())
        .dateDeclaree(date.dateDeclaree())
        .resolution(Optional.empty())
    );
  }

  void resout(UUID evenement, TypeDeResolution type, Auteur auteur) {
    signalements
      .get(new PointageSignaleId(evenement))
      .filter(signale -> !signale.estResolu())
      .ifPresent(signale -> signalements.update(signale.resolu(new Resolution(type, auteur, clock.now()))));
  }

  /**
   * L'heure retenue d'un geste du pupitre, et ce qu'il a fallu redresser pour l'enregistrer.
   */
  record DateRedressee(Horodatage horodatage, Set<MotifDeSignalement> motifs, Optional<Instant> dateDeclaree) {
    DateRedressee avec(MotifDeSignalement motif) {
      return new DateRedressee(horodatage, union(motif), dateDeclaree);
    }

    private Set<MotifDeSignalement> union(MotifDeSignalement motif) {
      EnumSet<MotifDeSignalement> tous = EnumSet.of(motif);
      tous.addAll(motifs);

      return Set.copyOf(tous);
    }
  }

  /**
   * Un geste date dans le futur, horloge du pupitre en avance, est ramene a sa reception ; un geste date avant la
   * borne donnee, l'engagement d'un OF, y est ramene. La date declaree reste tracee.
   */
  static DateRedressee redresse(Optional<Instant> declaree, Instant maintenant, Optional<Instant> borneBasse) {
    Instant retenue = declaree.orElse(maintenant);
    EnumSet<MotifDeSignalement> motifs = EnumSet.noneOf(MotifDeSignalement.class);

    if (retenue.isAfter(maintenant)) {
      retenue = maintenant;
      motifs.add(MotifDeSignalement.DATE_FUTURE);
    }
    if (borneBasse.filter(retenue::isBefore).isPresent()) {
      retenue = borneBasse.orElseThrow();
      motifs.add(MotifDeSignalement.DATE_ANTERIEURE_A_L_ENGAGEMENT);
    }

    return new DateRedressee(new Horodatage(retenue, maintenant), Set.copyOf(motifs), motifs.isEmpty() ? Optional.empty() : declaree);
  }
}
