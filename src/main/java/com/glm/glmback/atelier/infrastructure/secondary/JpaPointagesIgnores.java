package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.PointageIgnore;
import com.glm.glmback.atelier.domain.PointagesIgnores;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/**
 * L'audit en base. Il s'ecrit par {@code persist} : jamais de lecture prealable ni de fusion, qui reecrirait la ligne
 * d'un renvoi au lieu d'en ajouter une, la table n'ayant aucune contrainte d'unicite.
 */
@Repository
class JpaPointagesIgnores implements PointagesIgnores {

  private final EntityManager entities;

  JpaPointagesIgnores(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  public void enregistre(PointageIgnore pointage) {
    entities.persist(PointageIgnoreEntity.from(pointage));
  }

  @Override
  public boolean contient(EvenementDAtelierId pointage) {
    return entities
      .createQuery("select count(p) > 0 from PointageIgnoreEntity p where p.id = :id", Boolean.class)
      .setParameter("id", pointage.uuid())
      .getSingleResult();
  }
}
