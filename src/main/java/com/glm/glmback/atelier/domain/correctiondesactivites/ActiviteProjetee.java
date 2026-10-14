package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.Echeance;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une activite telle que la projection de l'atelier la porte : son identite, son suivi et l'element engage, son poste
 * courant et la nature figee a la saisie, sa categorie, son debut, son echeance et sa fin reelle. Elle ne depend pas de l'instant de lecture ; c'est la
 * lecture de la journee qui dit si elle est en cours ou terminee automatiquement.
 */
public record ActiviteProjetee(
  ActiviteId id,
  SuiviDAtelierId suivi,
  ElementEngage element,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  CategorieDActivite categorie,
  Instant debut,
  Echeance echeance,
  Optional<Instant> fin
) {
  public ActiviteProjetee {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("suivi", suivi);
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("categorie", categorie);
    Assert.notNull("debut", debut);
    Assert.notNull("echeance", echeance);
    Assert.notNull("fin", fin);
  }

  public static ActiviteProjeteeIdBuilder builder() {
    return id ->
      suivi ->
        element ->
          poste ->
            nature ->
              categorie ->
                debut -> echeance -> fin -> new ActiviteProjetee(id, suivi, element, poste, nature, categorie, debut, echeance, fin);
  }

  /**
   * L'etat a l'instant d'evaluation : terminee a sa fin reelle si elle en a une, terminee automatiquement des que son
   * echeance est atteinte, sinon en cours.
   */
  EtatDActiviteDeLaJournee etatA(Instant evaluation) {
    if (fin.isPresent()) {
      return EtatDActiviteDeLaJournee.TERMINEE;
    }

    return echeance.estAtteinteA(evaluation) ? EtatDActiviteDeLaJournee.TERMINEE_AUTOMATIQUEMENT : EtatDActiviteDeLaJournee.EN_COURS;
  }

  /**
   * La fin sur laquelle l'activite compte : sa fin reelle, a defaut son echeance. Une activite en cours n'en a pas
   * qui compte, mais la projection porte toujours son echeance : c'est l'etat qui dit de ne pas la compter.
   */
  Instant finRetenue() {
    return fin.orElse(echeance.value());
  }

  public interface ActiviteProjeteeIdBuilder {
    ActiviteProjeteeSuiviBuilder id(ActiviteId id);
  }

  public interface ActiviteProjeteeSuiviBuilder {
    ActiviteProjeteeElementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface ActiviteProjeteeElementBuilder {
    ActiviteProjeteePosteBuilder element(ElementEngage element);
  }

  public interface ActiviteProjeteePosteBuilder {
    ActiviteProjeteeNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface ActiviteProjeteeNatureBuilder {
    ActiviteProjeteeCategorieBuilder nature(Optional<NatureDOperation> nature);
  }

  public interface ActiviteProjeteeCategorieBuilder {
    ActiviteProjeteeDebutBuilder categorie(CategorieDActivite categorie);
  }

  public interface ActiviteProjeteeDebutBuilder {
    ActiviteProjeteeEcheanceBuilder debut(Instant debut);
  }

  public interface ActiviteProjeteeEcheanceBuilder {
    ActiviteProjeteeFinBuilder echeance(Echeance echeance);
  }

  public interface ActiviteProjeteeFinBuilder {
    ActiviteProjetee fin(Optional<Instant> fin);
  }
}
