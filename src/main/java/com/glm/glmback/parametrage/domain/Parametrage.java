package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Le parametrage d'une entreprise : une seule instance par entreprise, semee en base avec ses valeurs par defaut.
 *
 * <p>
 * Seule la derniere modification est tracee. Un parametrage jamais modifie n'en porte aucune : il est tel que la
 * creation de l'entreprise l'a seme.
 * </p>
 */
public record Parametrage(AmplitudeMaximale amplitudeMaximale, Optional<Modification> derniereModification) {
  public Parametrage {
    Assert.notNull("amplitude maximale", amplitudeMaximale);
    Assert.notNull("derniere modification", derniereModification);
  }

  /**
   * Une modification ne precede jamais celle qu'elle remplace : l'histoire ne se reecrit pas. Refixer la meme valeur
   * reste une modification, tracee comme telle.
   */
  public Parametrage fixeLAmplitudeMaximale(AmplitudeMaximale amplitude, Modification modification) {
    Assert.notNull("modification", modification);
    derniereModification.ifPresent(precedente -> Assert.field("date de modification", modification.date()).afterOrAt(precedente.date()));

    return new Parametrage(amplitude, Optional.of(modification));
  }
}
