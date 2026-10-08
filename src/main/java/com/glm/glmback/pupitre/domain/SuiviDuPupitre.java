package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Une tuile de l'ecran d'atelier : un element engage sur lequel on peut pointer, et ce qui s'y passe.
 *
 * <p>
 * Le nom est celui copie a l'engagement — un element renomme depuis ne reecrit pas l'histoire de l'atelier — tandis
 * que la reference est relue au referentiel a chaque lecture, comme les identites d'operateurs. Les activites sont
 * celles que l'atelier interprete et projette : ce contexte ne rejoue aucun journal, il juge seulement, a l'instant du
 * referentiel, lesquelles sont encore en cours.
 * </p>
 */
public record SuiviDuPupitre(
  SuiviDuPupitreId id,
  NomDElement nom,
  Optional<ReferenceDElement> reference,
  CategorieDElement categorie,
  List<ActiviteSansFin> activites,
  List<SequenceEnConflitDuPupitre> conflits,
  boolean dejaPointe
) {
  public SuiviDuPupitre {
    Assert.notNull("id du suivi", id);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("reference de l'element", reference);
    Assert.notNull("categorie de l'element", categorie);
    Assert.field("activites", activites).notNull().noNullElement();
    activites = List.copyOf(activites);
    Assert.field("conflits", conflits).notNull().noNullElement();
    conflits = List.copyOf(conflits);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static SuiviDuPupitreIdBuilder builder() {
    return id ->
      nom ->
        reference ->
          categorie ->
            activites ->
              conflits ->
                dejaPointe ->
                  new SuiviDuPupitre(
                    id,
                    nom,
                    ReferenceDElement.of(reference),
                    new CategorieDElement(categorie),
                    activites,
                    conflits,
                    dejaPointe
                  );
  }

  /**
   * Les activites en cours a cet instant : une activite dont l'echeance est atteinte en sort, terminee
   * automatiquement.
   */
  public List<ActiviteSansFin> activitesEnCoursA(Instant instant) {
    return activites
      .stream()
      .filter(activite -> activite.estEnCoursA(instant))
      .toList();
  }

  /**
   * L'etat a cet instant, juge sur les seules activites interpretables, comme l'atelier le juge : en cours si l'une
   * l'est, sinon interrompu des qu'un pointage actif existe, sinon en attente. Jamais {@code CLOTURE} : un element
   * cloture n'accepte plus de pointage et ne figure pas au referentiel du pupitre.
   */
  public EtatDuSuivi etatA(Instant instant) {
    if (!activitesEnCoursA(instant).isEmpty()) {
      return EtatDuSuivi.EN_COURS;
    }

    return dejaPointe ? EtatDuSuivi.INTERROMPU : EtatDuSuivi.EN_ATTENTE;
  }

  public interface SuiviDuPupitreIdBuilder {
    SuiviDuPupitreNomBuilder id(SuiviDuPupitreId id);
  }

  public interface SuiviDuPupitreNomBuilder {
    SuiviDuPupitreReferenceBuilder nom(NomDElement nom);
  }

  public interface SuiviDuPupitreReferenceBuilder {
    SuiviDuPupitreCategorieBuilder reference(String reference);
  }

  public interface SuiviDuPupitreCategorieBuilder {
    SuiviDuPupitreActivitesBuilder categorie(String categorie);
  }

  public interface SuiviDuPupitreActivitesBuilder {
    SuiviDuPupitreConflitsBuilder activites(List<ActiviteSansFin> activites);
  }

  public interface SuiviDuPupitreConflitsBuilder {
    SuiviDuPupitreDejaPointeBuilder conflits(List<SequenceEnConflitDuPupitre> conflits);
  }

  public interface SuiviDuPupitreDejaPointeBuilder {
    SuiviDuPupitre dejaPointe(boolean dejaPointe);
  }
}
