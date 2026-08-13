package com.glm.glmback.operateur.infrastructure.secondary;

import com.glm.glmback.operateur.domain.Matricule;
import com.glm.glmback.operateur.domain.Nom;
import com.glm.glmback.operateur.domain.Operateur;
import com.glm.glmback.operateur.domain.OperateurCriteria;
import com.glm.glmback.operateur.domain.OperateurDejaExistantException;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateurIntrouvableException;
import com.glm.glmback.operateur.domain.OperateurRepository;
import com.glm.glmback.operateur.domain.Prenom;
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
class JpaOperateurRepository implements OperateurRepository {

  private static final Sort PAR_IDENTITE = Sort.by(Sort.Order.asc("nom"), Sort.Order.asc("prenom"), Sort.Order.asc("id"));

  private final SpringDataOperateurRepository operateurs;

  JpaOperateurRepository(SpringDataOperateurRepository operateurs) {
    this.operateurs = operateurs;
  }

  @Override
  public Operateur create(Operateur operateur) {
    if (operateurs.existsById(operateur.id().uuid())) {
      throw new OperateurDejaExistantException(operateur.id());
    }
    operateurs.save(OperateurEntity.from(operateur));

    return operateur;
  }

  @Override
  public Operateur update(Operateur operateur) {
    if (!operateurs.existsById(operateur.id().uuid())) {
      throw new OperateurIntrouvableException(operateur.id());
    }
    operateurs.save(OperateurEntity.from(operateur));

    return operateur;
  }

  @Override
  public void delete(OperateurId id) {
    if (!operateurs.existsById(id.uuid())) {
      throw new OperateurIntrouvableException(id);
    }
    operateurs.deleteById(id.uuid());
  }

  @Override
  public Optional<Operateur> get(OperateurId id) {
    return operateurs.findById(id.uuid()).map(OperateurEntity::toDomain);
  }

  @Override
  public Optional<OperateurId> idPourIdentite(Nom nom, Prenom prenom) {
    return operateurs.findIdByIdentite(nom.value(), prenom.value()).map(OperateurId::new);
  }

  @Override
  public Optional<OperateurId> idPourMatricule(Matricule matricule) {
    return operateurs.findIdByMatricule(matricule.value()).map(OperateurId::new);
  }

  @Override
  public Page<Operateur> list(OperateurCriteria criteria, Pageable pageable) {
    var page = operateurs.findAll(correspondA(criteria), PageRequest.of(pageable.page(), pageable.size(), PAR_IDENTITE));

    return Page.<Operateur>builder()
      .content(page.getContent().stream().map(OperateurEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }

  /**
   * Traduit en SQL la regle que OperateurCriteria porte pour le domaine.
   */
  private static Specification<OperateurEntity> correspondA(OperateurCriteria criteria) {
    return (racine, requete, constructeur) -> {
      List<Predicate> predicats = new ArrayList<>();
      criteria.poste().ifPresent(poste -> predicats.add(constructeur.isMember(poste.uuid(), racine.get("postes"))));

      return constructeur.and(predicats.toArray(Predicate[]::new));
    };
  }
}
