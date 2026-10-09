package com.glm.glmback.naturedetravail.infrastructure.secondary;

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
 * Ce contexte n'en retient que la nature recopiee sur chaque pointage, la seule chose dont il a besoin pour refuser
 * definitivement une suppression. Le journal ne porte aucune cle etrangere vers le referentiel : la regle est la seule
 * garde.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class EvenementDeLaNatureEntity {

  @Id
  private UUID id;

  @Column(name = "nature_id")
  private UUID natureId;

  protected EvenementDeLaNatureEntity() {
    // Constructeur requis par JPA.
  }
}
