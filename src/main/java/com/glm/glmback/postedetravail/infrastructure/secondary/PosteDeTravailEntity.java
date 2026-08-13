package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.Libelle;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "poste_de_travail")
class PosteDeTravailEntity {

  @Id
  private UUID id;

  @Column(length = 100)
  private String libelle;

  @Column(length = 50)
  private String nature;

  protected PosteDeTravailEntity() {
    // Constructeur requis par JPA.
  }

  private PosteDeTravailEntity(PosteDeTravail poste) {
    id = poste.id().uuid();
    libelle = poste.libelle().value();
    nature = poste.nature().value();
  }

  static PosteDeTravailEntity from(PosteDeTravail poste) {
    return new PosteDeTravailEntity(poste);
  }

  PosteDeTravail toDomain() {
    return new PosteDeTravail(new PosteDeTravailId(id), new Libelle(libelle), new NatureDeTravail(nature));
  }
}
