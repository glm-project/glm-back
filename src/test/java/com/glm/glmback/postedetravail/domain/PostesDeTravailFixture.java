package com.glm.glmback.postedetravail.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public final class PostesDeTravailFixture {

  public static final Libelle LIBELLE_TOUR_1 = new Libelle("Tour 1");
  public static final Libelle LIBELLE_POSTE_DE_SOUDURE = new Libelle("Poste de soudure");
  public static final Libelle LIBELLE_FRAISEUSE_1 = new Libelle("Fraiseuse 1");

  public static final NatureDeTravail NATURE_TOURNAGE = new NatureDeTravail("tournage");
  public static final NatureDeTravail NATURE_SOUDAGE = new NatureDeTravail("soudage");
  public static final NatureDeTravail NATURE_FRAISAGE = new NatureDeTravail("fraisage");

  public static final NatureDeTravailId NATURE_DE_TRAVAIL_ID_TOURNAGE = new NatureDeTravailId(
    UUID.fromString("7c0e6a52-1f3d-4b8e-9d21-5a4b3c2d1e01")
  );
  public static final NatureDeTravailId NATURE_DE_TRAVAIL_ID_SOUDAGE = new NatureDeTravailId(
    UUID.fromString("7c0e6a52-1f3d-4b8e-9d21-5a4b3c2d1e02")
  );
  public static final NatureDeTravailId NATURE_DE_TRAVAIL_ID_FRAISAGE = new NatureDeTravailId(
    UUID.fromString("7c0e6a52-1f3d-4b8e-9d21-5a4b3c2d1e03")
  );

  public static final NatureDuPoste NATURE_DU_POSTE_TOURNAGE = new NatureDuPoste(NATURE_DE_TRAVAIL_ID_TOURNAGE, NATURE_TOURNAGE);
  public static final NatureDuPoste NATURE_DU_POSTE_SOUDAGE = new NatureDuPoste(NATURE_DE_TRAVAIL_ID_SOUDAGE, NATURE_SOUDAGE);
  public static final NatureDuPoste NATURE_DU_POSTE_FRAISAGE = new NatureDuPoste(NATURE_DE_TRAVAIL_ID_FRAISAGE, NATURE_FRAISAGE);

  public static final CoutHoraire COUT_HORAIRE_45_50 = new CoutHoraire(new BigDecimal("45.50"));
  public static final CoutHoraire COUT_HORAIRE_60 = new CoutHoraire(new BigDecimal("60.00"));

  private PostesDeTravailFixture() {}

  public static PosteDeTravail posteDeTravailTour1() {
    return posteDeTravailTour1(PosteDeTravailId.newId());
  }

  public static PosteDeTravail posteDeTravailTour1(PosteDeTravailId id) {
    return new PosteDeTravail(id, LIBELLE_TOUR_1, NATURE_DU_POSTE_TOURNAGE, Optional.empty());
  }

  public static PosteDeTravail posteDeTravailTour1AvecCoutHoraire() {
    return new PosteDeTravail(PosteDeTravailId.newId(), LIBELLE_TOUR_1, NATURE_DU_POSTE_TOURNAGE, Optional.of(COUT_HORAIRE_45_50));
  }

  public static PosteDeTravail posteDeTravailPosteDeSoudure() {
    return posteDeTravailPosteDeSoudure(PosteDeTravailId.newId());
  }

  public static PosteDeTravail posteDeTravailPosteDeSoudure(PosteDeTravailId id) {
    return new PosteDeTravail(id, LIBELLE_POSTE_DE_SOUDURE, NATURE_DU_POSTE_SOUDAGE, Optional.empty());
  }

  public static PosteDeTravail posteDeTravailFraiseuse1() {
    return new PosteDeTravail(PosteDeTravailId.newId(), LIBELLE_FRAISEUSE_1, NATURE_DU_POSTE_FRAISAGE, Optional.empty());
  }

  public static PosteDeTravailACreer posteDeTravailACreerTour1() {
    return new PosteDeTravailACreer(LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.empty());
  }

  public static PosteDeTravailACreer posteDeTravailACreerTour1AvecCoutHoraire() {
    return new PosteDeTravailACreer(LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.of(COUT_HORAIRE_45_50));
  }

  public static PosteDeTravailACreer posteDeTravailACreerPosteDeSoudure() {
    return new PosteDeTravailACreer(LIBELLE_POSTE_DE_SOUDURE, NATURE_SOUDAGE, Optional.empty());
  }

  public static PosteDeTravailAModifier posteDeTravailAModifierFraiseuse1(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, LIBELLE_FRAISEUSE_1, NATURE_FRAISAGE, Optional.empty());
  }

  public static PosteDeTravailAModifier posteDeTravailAModifierTour1(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.empty());
  }

  public static PosteDeTravailAModifier posteDeTravailAModifierTour1AvecCoutHoraire(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.of(COUT_HORAIRE_60));
  }

  public static PosteDeTravailCriteria criteresDeTournage() {
    return new PosteDeTravailCriteria(Optional.of(NATURE_TOURNAGE));
  }

  public static PosteDeTravailCriteria criteresSansFiltre() {
    return new PosteDeTravailCriteria(Optional.empty());
  }
}
