package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Regles de selection de l'ecran d'atelier.
 *
 * <p>
 * La correspondance vit ici, dans le domaine, pour que le double en memoire et l'adapter de persistance ne puissent pas
 * diverger. Un ensemble d'etats vide ne filtre rien.
 * </p>
 *
 * <p>
 * La periode est facultative : l'ecran des operateurs veut tous les elements actifs d'un coup, sans rien qui defile,
 * et n'a aucune notion de date. Elle ne sert qu'aux ecrans de back-office.
 * </p>
 *
 * <p>
 * L'etat se juge a l'instant d'evaluation de la lecture, le meme que celui auquel chaque ligne rendue se lit : un
 * element dont l'activite a atteint son echeance n'est plus en cours.
 * </p>
 */
public record SuiviDAtelierCriteria(Optional<Periode> periode, Set<EtatDAtelier> etats, Instant evaluation) {
  public SuiviDAtelierCriteria {
    Assert.notNull("periode", periode);
    Assert.field("etats", etats).notNull().noNullElement();
    Assert.notNull("evaluation", evaluation);
    etats = Set.copyOf(etats);
  }

  public boolean matches(SuiviDAtelier suivi) {
    return correspondALaPeriode(suivi) && (etats.isEmpty() || etats.contains(suivi.etat(evaluation)));
  }

  private boolean correspondALaPeriode(SuiviDAtelier suivi) {
    return periode.map(bornes -> bornes.contains(suivi.engagement().date())).orElse(true);
  }
}
