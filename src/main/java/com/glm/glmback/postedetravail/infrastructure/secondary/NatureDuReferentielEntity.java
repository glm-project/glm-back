package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table du referentiel des natures, que ce contexte lit sans jamais importer son code.
 */
@Entity
@Immutable
@Table(name = "nature_de_travail")
class NatureDuReferentielEntity {

  @Id
  private UUID id;

  @Column(length = 50)
  private String libelle;

  protected NatureDuReferentielEntity() {
    // Constructeur requis par JPA.
  }

  NatureDuPoste toDomain() {
    return new NatureDuPoste(new NatureDeTravailId(id), new NatureDeTravail(libelle));
  }
}
