package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Map;

/**
 * L'etat de presence des operateurs de l'entreprise, releve d'un seul coup.
 *
 * <p>
 * Seuls les operateurs dont une journee est encore ouverte y figurent. La correspondance vit ici, et non dans
 * l'adapter : la regle — qui n'est nomme par aucune journee en cours est absent — est une regle de lecture du
 * referentiel, qui s'enonce et se teste dans le domaine.
 * </p>
 */
public record PresencesDesOperateurs(Map<OperateurId, EtatDePresence> presences) {
  public PresencesDesOperateurs {
    Assert.notNull("presences", presences);
    presences = Map.copyOf(presences);
  }

  public EtatDePresence de(OperateurId operateur) {
    return presences.getOrDefault(operateur, EtatDePresence.ABSENT);
  }
}
