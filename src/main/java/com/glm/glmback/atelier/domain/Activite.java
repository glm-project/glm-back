package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une activite telle que l'interpretation des faits actifs d'une cle la donne : le pointage ouvrant qui la porte
 * aujourd'hui, et sa fin reelle quand un fait l'a terminee.
 *
 * <p>
 * Son identite est celle de son pointage ouvrant d'origine ; l'ouvrant, lui, est le fait actif qui la porte, le
 * remplacant d'une correction le cas echeant. Son operateur, son poste, sa nature, sa categorie et son debut sont
 * ceux de cet ouvrant.
 * </p>
 *
 * <p>
 * La fin est reelle ou absente : elle vient d'un geste qui termine l'activite, ou de la cloture du suivi. Elle ne
 * depend jamais de l'instant ou on lit.
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

  IntervalleDActivite intervalle() {
    return IntervalleDActivite.builder()
      .evenement(ouvrant.id())
      .operateur(ouvrant.operateur())
      .poste(ouvrant.poste())
      .nature(ouvrant.nature())
      .categorie(categorie())
      .debut(debut())
      .fin(fin);
  }
}
