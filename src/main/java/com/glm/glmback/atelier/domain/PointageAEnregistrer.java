package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Un pointage d'operateur : sa date de survenue est l'instant present, decide par le service.
 *
 * <p>
 * Le poste de travail est facultatif : un operateur mono-poste, ou une entreprise sans parc machine, garde un seul
 * clic. La nature de l'operation n'est pas fournie par l'appelant, le service la recopie du profil de l'operateur.
 * </p>
 */
public record PointageAEnregistrer(
  SuiviDAtelierId suivi,
  TypeDEvenementDAtelier type,
  Operateur operateur,
  Optional<PosteDeTravail> poste
) {
  public PointageAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("type", type);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
  }

  public PointageAEnregistrer(SuiviDAtelierId suivi, TypeDEvenementDAtelier type, Operateur operateur) {
    this(suivi, type, operateur, Optional.empty());
  }

  public static PointageAEnregistrerSuiviBuilder builder() {
    return suivi -> type -> operateur -> poste -> new PointageAEnregistrer(suivi, type, operateur, poste);
  }

  public interface PointageAEnregistrerSuiviBuilder {
    PointageAEnregistrerTypeBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface PointageAEnregistrerTypeBuilder {
    PointageAEnregistrerOperateurBuilder type(TypeDEvenementDAtelier type);
  }

  public interface PointageAEnregistrerOperateurBuilder {
    PointageAEnregistrerPosteBuilder operateur(Operateur operateur);
  }

  public interface PointageAEnregistrerPosteBuilder {
    PointageAEnregistrer poste(Optional<PosteDeTravail> poste);
  }
}
