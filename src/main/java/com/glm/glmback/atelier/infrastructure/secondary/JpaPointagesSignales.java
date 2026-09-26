package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.CriteresDePointageSignale;
import com.glm.glmback.atelier.domain.PointageSignale;
import com.glm.glmback.atelier.domain.PointageSignaleDejaExistantException;
import com.glm.glmback.atelier.domain.PointageSignaleId;
import com.glm.glmback.atelier.domain.PointageSignaleIntrouvableException;
import com.glm.glmback.atelier.domain.PointagesSignales;
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
class JpaPointagesSignales implements PointagesSignales {

  private static final Sort PLUS_RECENT_D_ABORD = Sort.by(Sort.Order.desc("dateDeSurvenue"), Sort.Order.asc("id"));

  private final SpringDataPointagesSignalesRepository signalements;

  JpaPointagesSignales(SpringDataPointagesSignalesRepository signalements) {
    this.signalements = signalements;
  }

  @Override
  public PointageSignale create(PointageSignale pointage) {
    if (signalements.existsById(pointage.id().uuid())) {
      throw new PointageSignaleDejaExistantException(pointage.id());
    }
    signalements.save(PointageSignaleEntity.from(pointage));

    return pointage;
  }

  @Override
  public PointageSignale update(PointageSignale pointage) {
    if (!signalements.existsById(pointage.id().uuid())) {
      throw new PointageSignaleIntrouvableException(pointage.id());
    }
    signalements.save(PointageSignaleEntity.from(pointage));

    return pointage;
  }

  @Override
  public Optional<PointageSignale> get(PointageSignaleId id) {
    return signalements.findById(id.uuid()).map(PointageSignaleEntity::toDomain);
  }

  @Override
  public Page<PointageSignale> list(CriteresDePointageSignale criteres, Pageable pageable) {
    var page = signalements.findAll(correspondA(criteres), PageRequest.of(pageable.page(), pageable.size(), PLUS_RECENT_D_ABORD));

    return Page.<PointageSignale>builder()
      .content(page.getContent().stream().map(PointageSignaleEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }

  /**
   * Traduit en SQL les regles que {@link CriteresDePointageSignale#matches} porte pour le domaine : non resolu,
   * operateur et motif facultatifs.
   */
  private static Specification<PointageSignaleEntity> correspondA(CriteresDePointageSignale criteres) {
    return (racine, requete, constructeur) -> {
      List<Predicate> predicats = new ArrayList<>();
      predicats.add(constructeur.isNull(racine.get("resolutionType")));
      criteres.operateur().ifPresent(operateur -> predicats.add(constructeur.equal(racine.get("operateurId"), operateur.uuid())));
      criteres.motif().ifPresent(motif -> predicats.add(constructeur.like(racine.get("motifs"), "%" + motif.name() + "%")));

      return constructeur.and(predicats.toArray(Predicate[]::new));
    };
  }
}
