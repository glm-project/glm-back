package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.CoutHoraire;
import com.glm.glmback.postedetravail.domain.Libelle;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "poste_de_travail")
class PosteDeTravailEntity {

  static final String LIBELLE_DE_LA_NATURE =
    "(select referentiel.libelle from nature_de_travail referentiel where referentiel.id = nature_id)";

  @Id
  private UUID id;

  @Column(length = 100)
  private String libelle;

  @Column(name = "nature_id")
  private UUID natureId;

  /**
   * Le libelle courant de la nature, relu a chaque lecture : le poste n'en persiste que l'identifiant.
   */
  @Formula(LIBELLE_DE_LA_NATURE)
  private String nature;

  @Column(name = "cout_horaire", precision = 10, scale = 2)
  private BigDecimal coutHoraire;

  protected PosteDeTravailEntity() {
    // Constructeur requis par JPA.
  }

  private PosteDeTravailEntity(PosteDeTravail poste) {
    id = poste.id().uuid();
    libelle = poste.libelle().value();
    natureId = poste.nature().id().uuid();
    nature = poste.nature().libelle().value();
    coutHoraire = poste.coutHoraire().map(CoutHoraire::value).orElse(null);
  }

  static PosteDeTravailEntity from(PosteDeTravail poste) {
    return new PosteDeTravailEntity(poste);
  }

  PosteDeTravail toDomain() {
    return PosteDeTravail.builder()
      .id(new PosteDeTravailId(id))
      .libelle(new Libelle(libelle))
      .nature(new NatureDuPoste(new NatureDeTravailId(natureId), new NatureDeTravail(nature)))
      .coutHoraire(coutHoraire);
  }
}
