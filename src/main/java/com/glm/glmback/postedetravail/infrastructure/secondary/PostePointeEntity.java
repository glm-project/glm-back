package com.glm.glmback.postedetravail.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du journal d'atelier.
 *
 * <p>
 * Ce contexte lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext} : il n'en
 * retient que le poste pointe, la seule chose dont il a besoin pour refuser une suppression.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class PostePointeEntity {

  @Id
  private UUID id;

  @Column(name = "poste_id")
  private UUID posteId;

  protected PostePointeEntity() {
    // Constructeur requis par JPA.
  }
}
