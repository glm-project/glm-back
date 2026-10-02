package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.LibelleDePoste;
import com.glm.glmback.coutderevient.domain.PosteDeTravailId;
import com.glm.glmback.coutderevient.domain.PosteNomme;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail, restreinte au libelle que le detail d'un pointage affiche.
 *
 * <p>
 * Ni nature ni cout horaire : les deux viennent du fait ouvrant, figes a la saisie.
 * </p>
 */
@Entity
@Immutable
@Table(name = "poste_de_travail")
class PosteDuCoutEntity {

  @Id
  private UUID id;

  private String libelle;

  protected PosteDuCoutEntity() {
    // Constructeur requis par JPA.
  }

  PosteNomme toDomain() {
    return new PosteNomme(new PosteDeTravailId(id), new LibelleDePoste(libelle));
  }
}
