package com.glm.glmback.categoriedeproduit.domain;

public final class CategoriesDeProduitFixture {

  public static final CodeDeCategorie CODE_MOULE = new CodeDeCategorie("MOULE");
  public static final CodeDeCategorie CODE_OF = new CodeDeCategorie("OF");

  public static final Rang RANG_1 = new Rang(1);
  public static final Rang RANG_2 = new Rang(2);

  private CategoriesDeProduitFixture() {}

  public static CategorieDeProduit categorieDeProduitMoule() {
    return new CategorieDeProduit(CODE_MOULE, RANG_1);
  }

  public static CategorieDeProduit categorieDeProduitOf() {
    return new CategorieDeProduit(CODE_OF, RANG_2);
  }
}
