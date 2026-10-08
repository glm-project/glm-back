package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.regex.Pattern;

/**
 * Ce qui designe une categorie, et rien d'autre : le code est a la fois le libelle affiche et le prefixe du nom des
 * elements qui s'y creent.
 *
 * <p>
 * Son motif est celui du prefixe de nom d'element. C'est pour cela qu'il ne se renomme jamais : il entre dans la cle
 * qui fabrique les noms.
 * </p>
 */
public record CodeDeCategorie(String value) {
  private static final Pattern PATTERN = Pattern.compile("^[A-Z]{1,10}$");

  public CodeDeCategorie {
    Assert.field("code", value).notBlank().matches(PATTERN);
  }
}
