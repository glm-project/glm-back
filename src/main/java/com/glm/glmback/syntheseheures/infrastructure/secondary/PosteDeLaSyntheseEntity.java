package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.LibelleDePoste;
import com.glm.glmback.syntheseheures.domain.PosteConnu;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail, reduite a leur libelle.
 */
@Entity
@Immutable
@Table(name = "poste_de_travail")
class PosteDeLaSyntheseEntity {

  @Id
  private UUID id;

  private String libelle;

  protected PosteDeLaSyntheseEntity() {
    // Constructeur requis par JPA.
  }

  PosteConnu toDomain() {
    return new PosteConnu(new PosteDeTravailId(id), new LibelleDePoste(libelle));
  }
}
