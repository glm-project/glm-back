package com.glm.glmback.operateur.infrastructure.secondary;

import com.glm.glmback.operateur.domain.LibelleDePoste;
import com.glm.glmback.operateur.domain.NatureDeTravail;
import com.glm.glmback.operateur.domain.PosteHabilitable;
import com.glm.glmback.operateur.domain.PosteHabilitableId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail.
 *
 * <p>
 * Ce contexte lit la table du contexte voisin sans jamais importer son code, annote {@code BusinessContext}. Rien n'est
 * copie sur l'operateur : le libelle et la nature sont relus a chaque lecture, pour qu'un poste renomme s'affiche
 * renomme partout.
 * </p>
 */
@Entity
@Immutable
@Table(name = "poste_de_travail")
class PosteHabilitableEntity {

  @Id
  private UUID id;

  private String libelle;

  private String nature;

  protected PosteHabilitableEntity() {
    // Constructeur requis par JPA.
  }

  PosteHabilitable toDomain() {
    return new PosteHabilitable(new PosteHabilitableId(id), new LibelleDePoste(libelle), new NatureDeTravail(nature));
  }
}
