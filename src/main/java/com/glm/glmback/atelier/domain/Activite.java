package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une activite telle que l'interpretation des faits actifs d'une cle la donne : le pointage ouvrant qui la porte
 * aujourd'hui, sa fin reelle quand un fait l'a terminee, et si une sequence en conflit la laisse a resoudre.
 *
 * <p>
 * Son identite est celle de son pointage ouvrant d'origine ; l'ouvrant, lui, est le fait actif qui la porte, le
 * remplacant d'une correction le cas echeant. Son operateur, son poste, sa nature, sa categorie et son debut sont
 * ceux de cet ouvrant.
 * </p>
 *
 * <p>
 * La fin est reelle ou absente : elle vient d'un geste qui termine l'activite, ou de la cloture du suivi. Elle ne
 * depend jamais de l'instant ou on lit. Seule la lecture a un instant d'evaluation, {@link #a(Instant)}, decide si une
 * activite sans fin reelle est encore en cours ou deja terminee automatiquement a son echeance.
 * </p>
 *
 * <p>
 * Une activite a resoudre n'a pas de fin : des pointages contradictoires la concernent, et le systeme ne choisit
 * aucune de leurs lectures. Elle n'est ni en cours ni terminee, et son echeance ne la termine pas : seule une
 * correction ou une annulation du gestionnaire la rend de nouveau interpretable.
 * </p>
 */
public record Activite(EvenementDAtelier ouvrant, Optional<Instant> fin, boolean aResoudre) {
  public Activite {
    Assert.notNull("ouvrant", ouvrant);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(ouvrant.dateDeSurvenue()));
    if (aResoudre) {
      Assert.field("fin d'une activite a resoudre", fin.stream().toList()).maxSize(0);
    }
  }

  static Activite ouvertePar(EvenementDAtelier ouvrant) {
    return new Activite(ouvrant, Optional.empty(), false);
  }

  Activite termineeA(Instant date) {
    return new Activite(ouvrant, Optional.of(date), false);
  }

  /**
   * La meme activite, prise dans une sequence en conflit : elle perd sa fin, que les pointages contradictoires ne
   * permettent plus d'affirmer.
   */
  Activite enConflit() {
    return new Activite(ouvrant, Optional.empty(), true);
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

  public Echeance echeance() {
    return Echeance.apres(debut());
  }

  /**
   * L'activite telle qu'elle se lit a l'instant d'evaluation : a resoudre si une sequence en conflit la concerne ;
   * sinon terminee a sa fin reelle si un fait l'a terminee ; sinon terminee automatiquement a son echeance, avec une
   * anomalie, des que l'echeance est atteinte ; sinon en cours. C'est le seul endroit ou l'instant de lecture
   * intervient.
   */
  public IntervalleDActivite a(Instant evaluation) {
    if (aResoudre) {
      return intervalle(Optional.empty(), false);
    }

    if (fin.isEmpty() && echeance().estAtteinteA(evaluation)) {
      return intervalle(Optional.of(echeance().value()), true);
    }

    return intervalle(fin, false);
  }

  /**
   * Vrai si l'activite est interpretable, qu'aucun fait ne l'a terminee et que son echeance n'est pas encore atteinte a
   * l'instant d'evaluation.
   */
  public boolean estEnCoursA(Instant evaluation) {
    return !aResoudre && fin.isEmpty() && !echeance().estAtteinteA(evaluation);
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
      .finAutomatique(finAutomatique)
      .aResoudre(aResoudre);
  }
}
