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
 * activites ne sont jamais stockes : ils se deduisent du journal.
 * </p>
 */
public record SuiviDuPupitre(
  SuiviDuPupitreId id,
  NomDElement nom,
  Optional<ReferenceDElement> reference,
  TypeDElementEngage type,
  JournalDuPupitre journal
) {
  public SuiviDuPupitre {
    Assert.notNull("id du suivi", id);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("reference de l'element", reference);
    Assert.notNull("type de l'element", type);
    Assert.notNull("journal", journal);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static SuiviDuPupitreIdBuilder builder() {
    return id -> nom -> reference -> type -> journal -> new SuiviDuPupitre(id, nom, ReferenceDElement.of(reference), type, journal);
  }

  public List<ActiviteEnCours> activitesEnCours() {
    return journal.activitesEnCours();
  }

  /**
   * Jamais {@code CLOTURE} : un element cloture n'accepte plus de pointage et ne figure pas au referentiel du
   * pupitre.
   */
  public EtatDuSuivi etat() {
    if (!activitesEnCours().isEmpty()) {
      return EtatDuSuivi.EN_COURS;
    }

    return journal.estVierge() ? EtatDuSuivi.EN_ATTENTE : EtatDuSuivi.INTERROMPU;
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
    SuiviDuPupitreJournalBuilder type(TypeDElementEngage type);
  }

  public interface SuiviDuPupitreJournalBuilder {
    SuiviDuPupitre journal(JournalDuPupitre journal);
  }
}
