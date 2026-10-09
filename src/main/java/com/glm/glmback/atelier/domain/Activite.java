package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une activite telle que la lecture du journal la donne : le pointage ouvrant qui la porte, et sa fin reelle quand un
 * fait l'a terminee.
 *
 * <p>
 * Son identite est celle de son pointage ouvrant d'origine ; l'ouvrant, lui, est le fait qui la porte. Son operateur, son poste, sa nature, sa categorie et son debut sont
 * ceux de cet ouvrant.
 * </p>
 *
 * <p>
 * La fin est reelle ou absente : elle vient d'une fin pointee, d'une fin regularisee par le gestionnaire, ou de la
 * cloture du suivi. Elle ne depend jamais de l'instant ou on lit. Seule la lecture a un instant d'evaluation,
 * {@link #a(Instant)}, decide si une activite sans fin reelle est encore en cours ou deja terminee automatiquement a
 * son echeance.
 * </p>
 */
public record Activite(EvenementDAtelier ouvrant, Optional<Instant> fin) {
  public Activite {
    Assert.notNull("ouvrant", ouvrant);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(ouvrant.dateDeSurvenue()));
  }

  static Activite ouvertePar(EvenementDAtelier ouvrant) {
    return new Activite(ouvrant, Optional.empty());
  }

  Activite termineeA(Instant date) {
    return new Activite(ouvrant, Optional.of(date));
  }

  public ActiviteId id() {
    return ouvrant.activite().orElseThrow();
  }

  public CleDActivite cle() {
    return ouvrant.cle();
  }

  public CategorieDActivite categorie() {
    return ouvrant.type().categorie().orElseThrow();
  }

  public Instant debut() {
    return ouvrant.dateDeSurvenue();
  }

  /**
   * Le debut plus la duree maximale que l'ouvrant a portee, c'est-a-dire celle qui etait en vigueur quand l'activite a
   * commence, quoi que le gestionnaire ait fixe depuis.
   */
  public Echeance echeance() {
    return Echeance.apres(debut(), ouvrant.dureeMax().orElseThrow());
  }

  /**
   * L'activite telle qu'elle se lit a l'instant d'evaluation : terminee a sa fin reelle si un fait l'a terminee ;
   * sinon terminee automatiquement a son echeance, avec une anomalie, des que l'echeance est atteinte ; sinon en
   * cours. C'est ici que le domaine applique l'instant de
   * lecture. La regle d'echeance a trois lecteurs : le domaine ({@code Activite.a}), la supervision
   * ({@code ActiviteDeSupervision.a}) et le SQL de la liste des fins automatiques, qui ne peut pas appeler
   * {@link Echeance} et en recopie la comparaison. Leur parite est tenue par l'execution :
   * {@code ListeDesFinsAutomatiquesDAtelierIT} confronte ce SQL a {@code Activite.a}, a tout instant d'evaluation.
   */
  public IntervalleDActivite a(Instant evaluation) {
    if (fin.isEmpty() && echeance().estAtteinteA(evaluation)) {
      return intervalle(Optional.of(echeance().value()), true);
    }

    return intervalle(fin, false);
  }

  /**
   * Vrai si aucun fait n'a termine l'activite et que son echeance n'est pas encore atteinte a l'instant d'evaluation.
   */
  public boolean estEnCoursA(Instant evaluation) {
    return fin.isEmpty() && !echeance().estAtteinteA(evaluation);
  }

  private IntervalleDActivite intervalle(Optional<Instant> bornee, boolean finAutomatique) {
    return IntervalleDActivite.builder()
      .evenement(ouvrant.id())
      .activite(id())
      .operateur(ouvrant.operateur())
      .poste(ouvrant.poste())
      .nature(ouvrant.nature())
      .categorie(categorie())
      .debut(debut())
      .fin(bornee)
      .finAutomatique(finAutomatique);
  }
}
