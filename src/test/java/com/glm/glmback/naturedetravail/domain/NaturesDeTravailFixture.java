package com.glm.glmback.naturedetravail.domain;

import java.util.UUID;

public final class NaturesDeTravailFixture {

  public static final NatureDeTravailId NATURE_DE_TRAVAIL_ID_SOUDAGE = new NatureDeTravailId(
    UUID.fromString("5b1d3c1e-7a51-4f0e-9a4e-0f6c1d2b3a01")
  );
  public static final NatureDeTravailId NATURE_DE_TRAVAIL_ID_TOURNAGE = new NatureDeTravailId(
    UUID.fromString("5b1d3c1e-7a51-4f0e-9a4e-0f6c1d2b3a02")
  );

  public static final LibelleDeNature LIBELLE_SOUDAGE = new LibelleDeNature("Soudage");
  public static final LibelleDeNature LIBELLE_TOURNAGE = new LibelleDeNature("Tournage");

  private NaturesDeTravailFixture() {}

  public static NatureDeTravail natureDeTravailSoudage() {
    return new NatureDeTravail(NATURE_DE_TRAVAIL_ID_SOUDAGE, LIBELLE_SOUDAGE);
  }

  public static NatureDeTravail natureDeTravailTournage() {
    return new NatureDeTravail(NATURE_DE_TRAVAIL_ID_TOURNAGE, LIBELLE_TOURNAGE);
  }
}
