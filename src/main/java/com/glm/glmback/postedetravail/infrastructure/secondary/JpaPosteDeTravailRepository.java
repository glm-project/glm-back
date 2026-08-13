package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.Libelle;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailCriteria;
import com.glm.glmback.postedetravail.domain.PosteDeTravailDejaExistantException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailIntrouvableException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailRepository;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaPosteDeTravailRepository implements PosteDeTravailRepository {

  private static final Sort PAR_LIBELLE = Sort.by(Sort.Order.asc("libelle"), Sort.Order.asc("id"));

  private final SpringDataPosteDeTravailRepository postes;

  JpaPosteDeTravailRepository(SpringDataPosteDeTravailRepository postes) {
    this.postes = postes;
  }

  @Override
  public PosteDeTravail create(PosteDeTravail poste) {
    if (postes.existsById(poste.id().uuid())) {
      throw new PosteDeTravailDejaExistantException(poste.id());
    }
    postes.save(PosteDeTravailEntity.from(poste));

    return poste;
  }

  @Override
  public PosteDeTravail update(PosteDeTravail poste) {
    if (!postes.existsById(poste.id().uuid())) {
      throw new PosteDeTravailIntrouvableException(poste.id());
    }
    postes.save(PosteDeTravailEntity.from(poste));

    return poste;
  }

  @Override
  public void delete(PosteDeTravailId id) {
    if (!postes.existsById(id.uuid())) {
      throw new PosteDeTravailIntrouvableException(id);
    }
    postes.deleteById(id.uuid());
  }

  @Override
  public Optional<PosteDeTravail> get(PosteDeTravailId id) {
    return postes.findById(id.uuid()).map(PosteDeTravailEntity::toDomain);
  }

  @Override
  public Optional<PosteDeTravailId> idPourLibelle(Libelle libelle) {
    return postes.findIdByLibelle(libelle.value()).map(PosteDeTravailId::new);
  }

  @Override
  public Page<PosteDeTravail> list(PosteDeTravailCriteria criteria, Pageable pageable) {
    var page = postes.findAll(correspondA(criteria), PageRequest.of(pageable.page(), pageable.size(), PAR_LIBELLE));

    return Page.<PosteDeTravail>builder()
      .content(page.getContent().stream().map(PosteDeTravailEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }

  /**
   * Traduit en SQL la regle que {@link PosteDeTravailCriteria#matches} porte pour le domaine.
   */
  private static Specification<PosteDeTravailEntity> correspondA(PosteDeTravailCriteria criteria) {
    return (racine, requete, constructeur) -> {
      List<Predicate> predicats = new ArrayList<>();
      criteria.nature().ifPresent(nature -> predicats.add(constructeur.equal(racine.get("nature"), nature.value())));

      return constructeur.and(predicats.toArray(Predicate[]::new));
    };
  }
}
