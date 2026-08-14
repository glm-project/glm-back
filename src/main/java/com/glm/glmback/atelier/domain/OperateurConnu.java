package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ce que l'atelier retient d'un operateur du referentiel : son identite, relue a chaque lecture, et son taux
 * horaire.
 *
 * <p>
 * Rien n'est copie dans le journal a partir de l'identite, qui ne stocke que l'identifiant. C'est la difference
 * exacte avec {@link ElementEngage} : un element renomme ne doit pas reecrire l'histoire de l'atelier, alors qu'une
 * identite corrigee doit s'afficher corrigee partout. Le taux horaire, lui, est recopie sur l'evenement au moment de
 * la saisie, sur le meme patron que la {@link NatureDOperation} du poste.
 * </p>
 */
public record OperateurConnu(OperateurId id, Nom nom, Prenom prenom, Optional<TauxHoraire> tauxHoraire) {
  public OperateurConnu {
    Assert.notNull("id de l'operateur", id);
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
    Assert.notNull("taux horaire", tauxHoraire);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}. La resolution depuis le referentiel, elle, reste l'affaire de ce domaine.
   */
  public static OperateurConnuIdBuilder builder() {
    return id -> nom -> prenom -> tauxHoraire -> new OperateurConnu(id, nom, prenom, TauxHoraire.of(tauxHoraire));
  }

  public interface OperateurConnuIdBuilder {
    OperateurConnuNomBuilder id(OperateurId id);
  }

  public interface OperateurConnuNomBuilder {
    OperateurConnuPrenomBuilder nom(Nom nom);
  }

  public interface OperateurConnuPrenomBuilder {
    OperateurConnuTauxHoraireBuilder prenom(Prenom prenom);
  }

  public interface OperateurConnuTauxHoraireBuilder {
    OperateurConnu tauxHoraire(BigDecimal tauxHoraire);
  }
}
