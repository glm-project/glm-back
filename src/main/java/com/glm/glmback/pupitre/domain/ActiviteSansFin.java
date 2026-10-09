package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une activite qu'aucun fait n'a terminee, telle que l'atelier la projette : qui travaille, sur quel
 * poste, dans quelle categorie, depuis quand, et jusqu'a quand au plus tard.
 *
 * <p>
 * Elle est en cours jusqu'a son echeance, et terminee automatiquement des que l'echeance est atteinte, a l'echeance
 * pile comprise. L'echeance est celle que l'atelier a calculee, son debut plus la duree maximale en vigueur a ce debut :
 * ce contexte ne la recalcule pas, il la lit et la transmet au pupitre, qui juge lui-meme l'expiration hors ligne.
 * </p>
 */
public record ActiviteSansFin(ActiviteId ouverture, CleDActivite activite, CategorieDActivite categorie, Instant depuis, Instant echeance) {
  public ActiviteSansFin {
    Assert.notNull("ouverture", ouverture);
    Assert.notNull("activite", activite);
    Assert.notNull("categorie", categorie);
    Assert.notNull("depuis", depuis);
    Assert.notNull("echeance", echeance);
  }

  private ActiviteSansFin(ActiviteSansFinBuilder builder) {
    this(builder.ouverture, builder.activite, builder.categorie, builder.depuis, builder.echeance);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static ActiviteSansFinOuvertureBuilder builder() {
    return new ActiviteSansFinBuilder();
  }

  /**
   * Vrai tant que l'echeance n'est pas atteinte a cet instant.
   */
  public boolean estEnCoursA(Instant instant) {
    return instant.isBefore(echeance);
  }

  public OperateurId operateur() {
    return activite.operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return activite.poste();
  }

  private static final class ActiviteSansFinBuilder
    implements
      ActiviteSansFinOuvertureBuilder,
      ActiviteSansFinActiviteBuilder,
      ActiviteSansFinCategorieBuilder,
      ActiviteSansFinDepuisBuilder,
      ActiviteSansFinEcheanceBuilder
  {

    private ActiviteId ouverture;
    private CleDActivite activite;
    private CategorieDActivite categorie;
    private Instant depuis;
    private Instant echeance;

    @Override
    public ActiviteSansFinActiviteBuilder ouverture(ActiviteId ouverture) {
      this.ouverture = ouverture;

      return this;
    }

    @Override
    public ActiviteSansFinCategorieBuilder activite(CleDActivite activite) {
      this.activite = activite;

      return this;
    }

    @Override
    public ActiviteSansFinDepuisBuilder categorie(CategorieDActivite categorie) {
      this.categorie = categorie;

      return this;
    }

    @Override
    public ActiviteSansFinEcheanceBuilder depuis(Instant depuis) {
      this.depuis = depuis;

      return this;
    }

    @Override
    public ActiviteSansFin echeance(Instant echeance) {
      this.echeance = echeance;

      return new ActiviteSansFin(this);
    }
  }

  public interface ActiviteSansFinOuvertureBuilder {
    ActiviteSansFinActiviteBuilder ouverture(ActiviteId ouverture);
  }

  public interface ActiviteSansFinActiviteBuilder {
    ActiviteSansFinCategorieBuilder activite(CleDActivite activite);
  }

  public interface ActiviteSansFinCategorieBuilder {
    ActiviteSansFinDepuisBuilder categorie(CategorieDActivite categorie);
  }

  public interface ActiviteSansFinDepuisBuilder {
    ActiviteSansFinEcheanceBuilder depuis(Instant depuis);
  }

  public interface ActiviteSansFinEcheanceBuilder {
    ActiviteSansFin echeance(Instant echeance);
  }
}
