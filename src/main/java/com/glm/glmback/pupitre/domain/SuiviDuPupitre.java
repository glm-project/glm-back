package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/**
 * Une tuile de l'ecran d'atelier : un element engage sur lequel on peut pointer, et ce qui s'y passe.
 *
 * <p>
 * Le nom est celui copie a l'engagement — un element renomme depuis ne reecrit pas l'histoire de l'atelier — tandis
 * que la reference est relue au referentiel a chaque lecture, comme les identites d'operateurs. L'etat et les
 * activites viennent de la situation interpretee par l'atelier a l'instant du referentiel.
 * </p>
 */
public record SuiviDuPupitre(
  SuiviDuPupitreId id,
  NomDElement nom,
  Optional<ReferenceDElement> reference,
  TypeDElementEngage type,
  SituationDuSuivi situation
) {
  public SuiviDuPupitre {
    Assert.notNull("id du suivi", id);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("reference de l'element", reference);
    Assert.notNull("type de l'element", type);
    Assert.notNull("situation", situation);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static SuiviDuPupitreIdBuilder builder() {
    return id -> nom -> reference -> type -> situation -> new SuiviDuPupitre(id, nom, ReferenceDElement.of(reference), type, situation);
  }

  public List<ActivitePointable> activitesEnCours() {
    return situation.activites();
  }

  /**
   * Jamais {@code CLOTURE} : un element cloture n'accepte plus de pointage et ne figure pas au referentiel du
   * pupitre.
   */
  public EtatDuSuivi etat() {
    return situation.etat();
  }

  public interface SuiviDuPupitreIdBuilder {
    SuiviDuPupitreNomBuilder id(SuiviDuPupitreId id);
  }

  public interface SuiviDuPupitreNomBuilder {
    SuiviDuPupitreReferenceBuilder nom(NomDElement nom);
  }

  public interface SuiviDuPupitreReferenceBuilder {
    SuiviDuPupitreTypeBuilder reference(String reference);
  }

  public interface SuiviDuPupitreTypeBuilder {
    SuiviDuPupitreSituationBuilder type(TypeDElementEngage type);
  }

  public interface SuiviDuPupitreSituationBuilder {
    SuiviDuPupitre situation(SituationDuSuivi situation);
  }
}
