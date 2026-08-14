package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.LibelleDePoste;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des postes de travail.
 *
 * <p>
 * C'est d'ici que viennent la nature et le cout horaire recopies sur l'evenement : le referentiel les porte sur le
 * poste, et le poste seul sait de quel metier releve le temps qu'on y passe et a quel cout.
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

  @Column(name = "cout_horaire", precision = 10, scale = 2)
  private BigDecimal coutHoraire;

  protected PosteConnuEntity() {
    // Constructeur requis par JPA.
  }

  PosteConnu toDomain() {
    return PosteConnu.builder()
      .id(new PosteDeTravailId(id))
      .libelle(new LibelleDePoste(libelle))
      .nature(new NatureDOperation(nature))
      .coutHoraire(coutHoraire);
  }
}
