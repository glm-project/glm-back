package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.PosteConnu;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import com.glm.glmback.syntheseheures.domain.PostesDeTravail;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les libelles des postes, lus dans la table du referentiel sans importer son code, en une seule requete.
 */
@Repository
class PostesDeLaSynthese implements PostesDeTravail {

  private final SpringDataPostesDeLaSyntheseRepository postes;

  PostesDeLaSynthese(SpringDataPostesDeLaSyntheseRepository postes) {
    this.postes = postes;
  }

  @Override
  public List<PosteConnu> parIds(Set<PosteDeTravailId> ids) {
    return postes
      .findByIdIn(ids.stream().map(PosteDeTravailId::uuid).collect(Collectors.toSet()))
      .stream()
      .map(PosteDeLaSyntheseEntity::toDomain)
      .toList();
  }
}
