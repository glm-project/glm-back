package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des elements de fabrication.
 *
 * <p>
 * Ce contexte lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext} : il n'en
 * retient que la categorie de chaque element, la seule chose dont il a besoin pour refuser une suppression.
 * </p>
 */
@Entity
@Immutable
@Table(name = "element_de_fabrication")
class ElementCategoriseEntity {

  @Id
  private UUID id;

  @Column(name = "type", length = 30)
  private String categorie;

  protected ElementCategoriseEntity() {
    // Constructeur requis par JPA.
  }
}
