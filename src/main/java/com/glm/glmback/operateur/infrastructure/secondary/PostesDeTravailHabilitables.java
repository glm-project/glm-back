package com.glm.glmback.operateur.infrastructure.secondary;

import com.glm.glmback.operateur.domain.PosteHabilitable;
import com.glm.glmback.operateur.domain.PosteHabilitableId;
import com.glm.glmback.operateur.domain.PostesHabilitables;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les postes de travail declares par l'entreprise courante, sur lesquels un operateur peut etre habilite.
 */
@Repository
class PostesDeTravailHabilitables implements PostesHabilitables {

  private final SpringDataPostesHabilitablesRepository postes;

  PostesDeTravailHabilitables(SpringDataPostesHabilitablesRepository postes) {
    this.postes = postes;
  }

  @Override
  public List<PosteHabilitable> parIds(Set<PosteHabilitableId> ids) {
    return postes
      .findByIdIn(ids.stream().map(PosteHabilitableId::uuid).collect(Collectors.toSet()))
      .stream()
      .map(PosteHabilitableEntity::toDomain)
      .toList();
  }
}
