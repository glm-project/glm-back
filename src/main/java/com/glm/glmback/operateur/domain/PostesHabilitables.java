package com.glm.glmback.operateur.domain;

import java.util.List;
import java.util.Set;

/**
 * Ce que l'operateur sait des postes de travail, sans jamais dependre de leur contexte.
 *
 * <p>
 * Une seule methode, qui resout un ensemble d'un coup : une page entiere d'operateurs se lit en une requete, la ou un
 * acces unitaire en couterait autant que d'habilitations.
 * </p>
 */
public interface PostesHabilitables {
  List<PosteHabilitable> parIds(Set<PosteHabilitableId> ids);
}
