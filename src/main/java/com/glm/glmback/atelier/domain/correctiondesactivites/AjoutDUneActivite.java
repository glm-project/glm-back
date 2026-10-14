package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * L'activite oubliee que le gestionnaire ajoute a un suivi : son identifiant, que le client fournit une fois par saisie
 * et qui est celui de l'ouverture donc celui de l'activite, l'operateur, le poste, la categorie, la plage et l'auteur.
 *
 * <p>
 * Elle s'ecrit en deux faits d'origine regularisation, une ouverture a l'heure du debut et une fin a l'heure de la fin
 * qui vise l'activite. L'identifiant de la fin se derive de celui de la saisie : un renvoi de la meme saisie retrouve
 * ainsi ses deux faits, sans que le client en fournisse un second.
 * </p>
 */
public record AjoutDUneActivite(
  SuiviDAtelierId suivi,
  EvenementDAtelierId evenement,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  CategorieDActivite categorie,
  PlageDAjout plage,
  Auteur auteur
) {
  public AjoutDUneActivite {
    Assert.notNull("suivi", suivi);
    Assert.notNull("id de l'evenement", evenement);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("categorie", categorie);
    Assert.notNull("plage", plage);
    Assert.notNull("auteur", auteur);
  }

  public static AjoutDUneActiviteSuiviBuilder builder() {
    return suivi ->
      evenement ->
        operateur ->
          poste -> categorie -> plage -> auteur -> new AjoutDUneActivite(suivi, evenement, operateur, poste, categorie, plage, auteur);
  }

  public ActiviteId activite() {
    return ActiviteId.ouvertePar(evenement);
  }

  /**
   * L'identifiant du fait qui termine l'activite : derive de celui de la saisie, donc stable d'un renvoi a l'autre.
   */
  public EvenementDAtelierId evenementDeFin() {
    return new EvenementDAtelierId(UUID.nameUUIDFromBytes((evenement.uuid() + "/fin").getBytes(StandardCharsets.UTF_8)));
  }

  public interface AjoutDUneActiviteSuiviBuilder {
    AjoutDUneActiviteEvenementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface AjoutDUneActiviteEvenementBuilder {
    AjoutDUneActiviteOperateurBuilder evenement(EvenementDAtelierId evenement);
  }

  public interface AjoutDUneActiviteOperateurBuilder {
    AjoutDUneActivitePosteBuilder operateur(OperateurId operateur);
  }

  public interface AjoutDUneActivitePosteBuilder {
    AjoutDUneActiviteCategorieBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface AjoutDUneActiviteCategorieBuilder {
    AjoutDUneActivitePlageBuilder categorie(CategorieDActivite categorie);
  }

  public interface AjoutDUneActivitePlageBuilder {
    AjoutDUneActiviteAuteurBuilder plage(PlageDAjout plage);
  }

  public interface AjoutDUneActiviteAuteurBuilder {
    AjoutDUneActivite auteur(Auteur auteur);
  }
}
