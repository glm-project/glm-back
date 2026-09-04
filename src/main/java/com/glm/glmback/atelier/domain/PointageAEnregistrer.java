package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un pointage d'operateur : sa date de survenue est l'instant present, decide par le service.
 *
 * <p>
 * Le poste de travail est facultatif : un operateur mono-poste, ou une entreprise sans parc machine, garde un seul
 * clic. La nature de l'operation n'est pas fournie par l'appelant, le service la recopie du poste.
 * </p>
 *
 * <p>
 * L'operateur et l'auteur sont deux choses differentes : sur un pupitre partage, celui qui saisit n'est pas
 * necessairement celui dont on compte le temps. L'auteur vient toujours du jeton, jamais du corps de la requete.
 * </p>
 */
public record PointageAEnregistrer(
  SuiviDAtelierId suivi,
  TypeDEvenementDAtelier type,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  Auteur auteur,
  Optional<Instant> dateDeSurvenue,
  EvenementDAtelierId evenement
) {
  public PointageAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("type", type);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
    Assert.notNull("id de l'evenement", evenement);
  }

  public static PointageAEnregistrerSuiviBuilder builder() {
    return suivi ->
      type ->
        operateur ->
          poste -> auteur -> new PointageAEnregistrer(suivi, type, operateur, poste, auteur, Optional.empty(), EvenementDAtelierId.newId());
  }

  public static PointageAEnregistrer of(
    SuiviDAtelierId suivi,
    TypeDEvenementDAtelier type,
    OperateurId operateur,
    Optional<PosteDeTravailId> poste,
    Auteur auteur,
    Optional<Instant> dateDeSurvenue,
    EvenementDAtelierId evenement
  ) {
    return new PointageAEnregistrer(suivi, type, operateur, poste, auteur, dateDeSurvenue, evenement);
  }

  public interface PointageAEnregistrerSuiviBuilder {
    PointageAEnregistrerTypeBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface PointageAEnregistrerTypeBuilder {
    PointageAEnregistrerOperateurBuilder type(TypeDEvenementDAtelier type);
  }

  public interface PointageAEnregistrerOperateurBuilder {
    PointageAEnregistrerPosteBuilder operateur(OperateurId operateur);
  }

  public interface PointageAEnregistrerPosteBuilder {
    PointageAEnregistrerAuteurBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface PointageAEnregistrerAuteurBuilder {
    PointageAEnregistrer auteur(Auteur auteur);
  }
}
