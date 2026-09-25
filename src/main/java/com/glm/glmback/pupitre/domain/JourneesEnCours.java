package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * La journee en cours de chaque operateur qui en a une, avant qu'on sache si elle est abandonnee.
 */
public record JourneesEnCours(Map<OperateurId, JourneeEnCours> journees) {
  public JourneesEnCours {
    Assert.notNull("journees", journees);
    journees = Map.copyOf(journees);
  }

  public PresencesDesOperateurs a(Instant maintenant, AmplitudeMaximale seuil) {
    return new PresencesDesOperateurs(
      journees.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, journee -> journee.getValue().a(maintenant, seuil)))
    );
  }
}
