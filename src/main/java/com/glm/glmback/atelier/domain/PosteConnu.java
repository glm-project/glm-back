package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ce que l'atelier retient d'un poste du referentiel : son identite, son libelle, la nature du travail qui s'y fait
 * et son cout horaire.
 *
 * <p>
 * C'est d'ici que viennent la {@link NatureDOperation} et le {@link CoutHoraire} recopies sur l'evenement, et non du
 * profil de la personne : un operateur polyvalent declenche deux pointages, un par poste, et chacun sait de quel
 * metier il releve parce que le poste le dit.
 * </p>
 */
public record PosteConnu(PosteDeTravailId id, LibelleDePoste libelle, NatureDOperation nature, Optional<CoutHoraire> coutHoraire) {
  public PosteConnu {
    Assert.notNull("id du poste de travail", id);
    Assert.notNull("libelle du poste", libelle);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("cout horaire", coutHoraire);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}. La resolution depuis le referentiel, elle, reste l'affaire de ce domaine.
   */
  public static PosteConnuIdBuilder builder() {
    return id -> libelle -> nature -> coutHoraire -> new PosteConnu(id, libelle, nature, CoutHoraire.of(coutHoraire));
  }

  public interface PosteConnuIdBuilder {
    PosteConnuLibelleBuilder id(PosteDeTravailId id);
  }

  public interface PosteConnuLibelleBuilder {
    PosteConnuNatureBuilder libelle(LibelleDePoste libelle);
  }

  public interface PosteConnuNatureBuilder {
    PosteConnuCoutHoraireBuilder nature(NatureDOperation nature);
  }

  public interface PosteConnuCoutHoraireBuilder {
    PosteConnu coutHoraire(BigDecimal coutHoraire);
  }
}
