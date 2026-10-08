package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.regex.Pattern;

/**
 * La categorie de produit dans laquelle l'element est range, declaree par l'entreprise.
 *
 * <p>
 * Son code sert aussi de prefixe au nom de l'element : son motif est donc celui que {@link Nom} attend.
 * </p>
 */
public record Categorie(String value) {
  private static final Pattern PATTERN = Pattern.compile("^[A-Z]{1,10}$");

  public Categorie {
    Assert.field("categorie", value).notBlank().matches(PATTERN);
  }
}
