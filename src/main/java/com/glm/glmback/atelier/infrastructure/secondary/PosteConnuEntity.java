package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.LibelleDePoste;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail.
 *
 * <p>
 * C'est d'ici que vient la nature recopiee sur l'evenement : le referentiel la porte sur le poste, et le poste seul
 * sait de quel metier releve le temps qu'on y passe.
 * </p>
 */
@Entity
@Immutable
@Table(name = "poste_de_travail")
class PosteConnuEntity {

  @Id
  private UUID id;

  private String libelle;

  private String nature;

  protected PosteConnuEntity() {
    // Constructeur requis par JPA.
  }

  PosteConnu toDomain() {
    return new PosteConnu(new PosteDeTravailId(id), new LibelleDePoste(libelle), new NatureDOperation(nature));
  }
}
