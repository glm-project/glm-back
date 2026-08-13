package com.glm.glmback.postedetravail.domain;

import java.util.Optional;

public final class PostesDeTravailFixture {

  public static final Libelle LIBELLE_TOUR_1 = new Libelle("Tour 1");
  public static final Libelle LIBELLE_POSTE_DE_SOUDURE = new Libelle("Poste de soudure");
  public static final Libelle LIBELLE_FRAISEUSE_1 = new Libelle("Fraiseuse 1");

  public static final NatureDeTravail NATURE_TOURNAGE = new NatureDeTravail("tournage");
  public static final NatureDeTravail NATURE_SOUDAGE = new NatureDeTravail("soudage");
  public static final NatureDeTravail NATURE_FRAISAGE = new NatureDeTravail("fraisage");

  private PostesDeTravailFixture() {}

  public static PosteDeTravail posteDeTravailTour1() {
    return posteDeTravailTour1(PosteDeTravailId.newId());
  }

  public static PosteDeTravail posteDeTravailTour1(PosteDeTravailId id) {
    return new PosteDeTravail(id, LIBELLE_TOUR_1, NATURE_TOURNAGE);
  }

  public static PosteDeTravail posteDeTravailPosteDeSoudure() {
    return posteDeTravailPosteDeSoudure(PosteDeTravailId.newId());
  }

  public static PosteDeTravail posteDeTravailPosteDeSoudure(PosteDeTravailId id) {
    return new PosteDeTravail(id, LIBELLE_POSTE_DE_SOUDURE, NATURE_SOUDAGE);
  }

  public static PosteDeTravail posteDeTravailFraiseuse1() {
    return new PosteDeTravail(PosteDeTravailId.newId(), LIBELLE_FRAISEUSE_1, NATURE_FRAISAGE);
  }

  public static PosteDeTravailACreer posteDeTravailACreerTour1() {
    return new PosteDeTravailACreer(LIBELLE_TOUR_1, NATURE_TOURNAGE);
  }

  public static PosteDeTravailACreer posteDeTravailACreerPosteDeSoudure() {
    return new PosteDeTravailACreer(LIBELLE_POSTE_DE_SOUDURE, NATURE_SOUDAGE);
  }

  public static PosteDeTravailAModifier posteDeTravailAModifierFraiseuse1(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, LIBELLE_FRAISEUSE_1, NATURE_FRAISAGE);
  }

  public static PosteDeTravailAModifier posteDeTravailAModifierTour1(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, NATURE_TOURNAGE);
  }

  public static PosteDeTravailCriteria criteresDeTournage() {
    return new PosteDeTravailCriteria(Optional.of(NATURE_TOURNAGE));
  }

  public static PosteDeTravailCriteria criteresSansFiltre() {
    return new PosteDeTravailCriteria(Optional.empty());
  }
}
