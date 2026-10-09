package com.glm.glmback.naturedetravail.infrastructure.secondary;

import com.glm.glmback.naturedetravail.domain.LibelleDeNature;
import com.glm.glmback.naturedetravail.domain.NatureDeTravail;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "nature_de_travail")
class NatureDeTravailEntity {

  @Id
  private UUID id;

  @Column(length = 50)
  private String libelle;

  @Column(length = 50)
  private String cle;

  protected NatureDeTravailEntity() {
    // Constructeur requis par JPA.
  }

  private NatureDeTravailEntity(NatureDeTravail nature) {
    id = nature.id().uuid();
    libelle = nature.libelle().value();
    cle = nature.libelle().cle().value();
  }

  static NatureDeTravailEntity from(NatureDeTravail nature) {
    return new NatureDeTravailEntity(nature);
  }

  NatureDeTravail toDomain() {
    return new NatureDeTravail(new NatureDeTravailId(id), new LibelleDeNature(libelle));
  }
}
