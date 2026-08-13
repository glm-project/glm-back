package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.PostesConnus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les postes de travail declares par l'entreprise courante, tels que l'atelier les lit.
 */
@Repository
class PostesDeTravailDuReferentiel implements PostesConnus {

  private final SpringDataPostesConnusRepository postes;

  PostesDeTravailDuReferentiel(SpringDataPostesConnusRepository postes) {
    this.postes = postes;
  }

  @Override
  public Optional<PosteConnu> get(PosteDeTravailId id) {
    return postes.findById(id.uuid()).map(PosteConnuEntity::toDomain);
  }

  @Override
  public List<PosteConnu> parIds(Set<PosteDeTravailId> ids) {
    return postes
      .findByIdIn(ids.stream().map(PosteDeTravailId::uuid).collect(Collectors.toSet()))
      .stream()
      .map(PosteConnuEntity::toDomain)
      .toList();
  }
}
