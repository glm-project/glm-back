package com.glm.glmback.naturedetravail.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail.
 *
 * <p>
 * Ce contexte lit la table du contexte voisin sans jamais importer son code : il n'en retient que la nature de chaque
 * poste, la seule chose dont il a besoin pour refuser une suppression.
 * </p>
 */
@Entity
@Immutable
@Table(name = "poste_de_travail")
class PosteDeLaNatureEntity {

  @Id
  private UUID id;

  @Column(name = "nature_id")
  private UUID natureId;

  protected PosteDeLaNatureEntity() {
    // Constructeur requis par JPA.
  }
}
