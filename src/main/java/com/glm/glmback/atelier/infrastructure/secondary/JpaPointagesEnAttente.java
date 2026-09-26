package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.CriteresDePointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttenteDejaExistantException;
import com.glm.glmback.atelier.domain.PointageEnAttenteId;
import com.glm.glmback.atelier.domain.PointageEnAttenteIntrouvableException;
import com.glm.glmback.atelier.domain.PointagesEnAttente;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaPointagesEnAttente implements PointagesEnAttente {

  private static final Sort PLUS_RECENT_D_ABORD = Sort.by(Sort.Order.desc("dateDeReception"), Sort.Order.asc("id"));

  private final SpringDataPointagesEnAttenteRepository pointages;

  JpaPointagesEnAttente(SpringDataPointagesEnAttenteRepository pointages) {
    this.pointages = pointages;
  }

  @Override
  public PointageEnAttente create(PointageEnAttente pointage) {
    if (pointages.existsById(pointage.id().uuid())) {
      throw new PointageEnAttenteDejaExistantException(pointage.id());
    }
    pointages.save(PointageEnAttenteEntity.from(pointage));

    return pointage;
  }

  @Override
  public PointageEnAttente update(PointageEnAttente pointage) {
    if (!pointages.existsById(pointage.id().uuid())) {
      throw new PointageEnAttenteIntrouvableException(pointage.id());
    }
    pointages.save(PointageEnAttenteEntity.from(pointage));

    return pointage;
  }

  @Override
  public Optional<PointageEnAttente> get(PointageEnAttenteId id) {
    return pointages.findById(id.uuid()).map(PointageEnAttenteEntity::toDomain);
  }

  @Override
  public List<PointageEnAttente> parEvenementDuPupitre(UUID evenement) {
    return pointages.findByEvenementDuPupitre(evenement).stream().map(PointageEnAttenteEntity::toDomain).toList();
  }

  @Override
  public Page<PointageEnAttente> list(CriteresDePointageEnAttente criteres, Pageable pageable) {
    var page = pointages.findAll(correspondA(criteres), PageRequest.of(pageable.page(), pageable.size(), PLUS_RECENT_D_ABORD));

    return Page.<PointageEnAttente>builder()
      .content(page.getContent().stream().map(PointageEnAttenteEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }

  /**
   * Traduit en SQL les regles que {@link CriteresDePointageEnAttente#matches} porte pour le domaine : non traite,
   * operateur et motif facultatifs.
   */
  private static Specification<PointageEnAttenteEntity> correspondA(CriteresDePointageEnAttente criteres) {
    return (racine, requete, constructeur) -> {
      List<Predicate> predicats = new ArrayList<>();
      predicats.add(constructeur.isNull(racine.get("traitementType")));
      criteres.operateur().ifPresent(operateur -> predicats.add(constructeur.equal(racine.get("operateurId"), operateur.uuid())));
      criteres.motif().ifPresent(motif -> predicats.add(constructeur.equal(racine.get("motif"), motif)));

      return constructeur.and(predicats.toArray(Predicate[]::new));
    };
  }
}
