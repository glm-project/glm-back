package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ce sur quoi un operateur pointe : une machine, un etabli, un four, une salle.
 *
 * <p>
 * Le terme est volontairement generique, comme dans l'atelier. La nature, en revanche, est obligatoire ici : un poste
 * n'est declare que pour dire quel travail s'y fait.
 * </p>
 */
public record PosteDeTravail(PosteDeTravailId id, Libelle libelle, NatureDeTravail nature, Optional<CoutHoraire> coutHoraire) {
  public PosteDeTravail {
    Assert.notNull("id", id);
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature de travail", nature);
    Assert.notNull("cout horaire", coutHoraire);
  }

  public static PosteDeTravailIdBuilder builder() {
    return id -> libelle -> nature -> coutHoraire -> new PosteDeTravail(id, libelle, nature, CoutHoraire.of(coutHoraire));
  }

  public PosteDeTravail revise(Libelle libelle, NatureDeTravail nature, Optional<CoutHoraire> coutHoraire) {
    return new PosteDeTravail(id, libelle, nature, coutHoraire);
  }

  public interface PosteDeTravailIdBuilder {
    PosteDeTravailLibelleBuilder id(PosteDeTravailId id);
  }

  public interface PosteDeTravailLibelleBuilder {
    PosteDeTravailNatureBuilder libelle(Libelle libelle);
  }

  public interface PosteDeTravailNatureBuilder {
    PosteDeTravailCoutHoraireBuilder nature(NatureDeTravail nature);
  }

  public interface PosteDeTravailCoutHoraireBuilder {
    PosteDeTravail coutHoraire(BigDecimal coutHoraire);
  }
}
