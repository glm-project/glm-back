package com.glm.glmback.atelier.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ce que l'atelier sait des postes de travail, sans jamais dependre de leur contexte.
 */
public interface PostesConnus {
  Optional<PosteConnu> get(PosteDeTravailId id);

  List<PosteConnu> parIds(Set<PosteDeTravailId> ids);
}
