package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.LibelleDePoste;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import com.glm.glmback.pupitre.domain.PosteHabilite;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail, restreinte a ce qu'un bouton de pupitre affiche.
 *
 * <p>
 * Ni nature ni cout horaire : le pupitre propose le poste, il ne le qualifie ni ne le valorise. Nommee
 * {@code ...DuPupitreEntity} pour ne pas entrer en conflit avec les beans JPA homonymes des autres contextes qui
 * lisent la meme table.
 * </p>
 */
@Entity
@Immutable
@Table(name = "poste_de_travail")
class PosteDuPupitreEntity {

  @Id
  private UUID id;

  private String libelle;

  protected PosteDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  PosteHabilite toDomain() {
    return new PosteHabilite(new PosteDeTravailId(id), new LibelleDePoste(libelle));
  }
}
