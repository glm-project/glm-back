package com.glm.glmback.operateur.infrastructure.secondary;

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
 * retient que l'operateur dont le temps est affecte.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class PointageDOperateurEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  protected PointageDOperateurEntity() {
    // Constructeur requis par JPA.
  }
}
