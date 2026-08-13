package com.glm.glmback.operateur.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Doublure de test du port vers le contexte des postes de travail, que celui des operateurs ne connait que par la
 * donnee.
 */
final class PostesHabilitablesEnMemoire implements PostesHabilitables {

  private final Map<PosteHabilitableId, PosteHabilitable> postes = new LinkedHashMap<>();

  void declare(PosteHabilitable poste) {
    postes.put(poste.id(), poste);
  }

  @Override
  public List<PosteHabilitable> parIds(Set<PosteHabilitableId> ids) {
    return ids.stream().map(postes::get).filter(java.util.Objects::nonNull).toList();
  }
}
