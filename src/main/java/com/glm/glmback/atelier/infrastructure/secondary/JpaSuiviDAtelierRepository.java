package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierCriteria;
import com.glm.glmback.atelier.domain.SuiviDAtelierDejaExistantException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
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
class JpaSuiviDAtelierRepository implements SuiviDAtelierRepository {

  private static final Sort PAR_DATE_D_ENGAGEMENT_DESCENDANTE = Sort.by(Sort.Order.desc("engagementDate"), Sort.Order.asc("id"));

  private final SpringDataSuiviDAtelierRepository suivis;

  JpaSuiviDAtelierRepository(SpringDataSuiviDAtelierRepository suivis) {
    this.suivis = suivis;
  }

  @Override
  public SuiviDAtelier create(SuiviDAtelier suivi) {
    if (suivis.existsById(suivi.id().uuid())) {
      throw new SuiviDAtelierDejaExistantException(suivi.id());
    }
    suivis.save(SuiviDAtelierEntity.from(suivi));

    return suivi;
  }

  @Override
  public SuiviDAtelier update(SuiviDAtelier suivi) {
    SuiviDAtelierEntity entity = suivis
      .findForUpdateById(suivi.id().uuid())
      .orElseThrow(() -> new SuiviDAtelierIntrouvableException(suivi.id()));

    if (entity.contientDesEvenementsAbsentsDe(suivi)) {
      throw new SaisieConcurrenteException(suivi.id());
    }
    entity.reconcilie(suivi);

    return suivi;
  }

  @Override
  public Optional<SuiviDAtelier> get(SuiviDAtelierId id) {
    return suivis.findById(id.uuid()).map(SuiviDAtelierEntity::toDomain);
  }

  @Override
  public Optional<SuiviDAtelier> getEnCoursPour(ElementEngageId element) {
    return suivis
      .findFirstByElementIdAndClotureDateDeSurvenueIsNullOrderByEngagementDateDescIdAsc(element.uuid())
      .map(SuiviDAtelierEntity::toDomain);
  }

  @Override
  public Page<SuiviDAtelier> list(SuiviDAtelierCriteria criteria, Pageable pageable) {
    var page = suivis.findAll(correspondA(criteria), PageRequest.of(pageable.page(), pageable.size(), PAR_DATE_D_ENGAGEMENT_DESCENDANTE));

    return Page.<SuiviDAtelier>builder()
      .content(page.getContent().stream().map(SuiviDAtelierEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }

  /**
   * Traduit en SQL les regles que {@link SuiviDAtelierCriteria#matches} porte pour le domaine.
   *
   * <p>
   * L'etat n'etant pas stocke mais projete, c'est la seule facon de filtrer sans ramener toute l'entreprise. Les deux
   * expressions de la meme regle sont confrontees par un test de parite plutot que laissees a se croire d'accord.
   * </p>
   */
  private static Specification<SuiviDAtelierEntity> correspondA(SuiviDAtelierCriteria criteria) {
    return (racine, requete, constructeur) -> {
      List<Predicate> predicats = new ArrayList<>();
      criteria
        .periode()
        .ifPresent(periode -> predicats.add(constructeur.between(racine.get("engagementDate"), periode.debut(), periode.fin())));

      if (!criteria.etats().isEmpty()) {
        predicats.add(racine.get("etat").in(criteria.etats()));
      }

      return constructeur.and(predicats.toArray(Predicate[]::new));
    };
  }
}
