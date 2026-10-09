package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ce qui rend une nature unique : son libelle en minuscules, sans accents, aux espaces reduits a un seul.
 *
 * <p>
 * « Soudage », « soudage » et « Soudâge » portent la meme cle : le gestionnaire ne peut pas en declarer deux, ce qui
 * couperait en deux lignes les rapports d'un meme metier.
 * </p>
 */
public record CleDeNature(String value) {
  private static final Pattern MARQUES_DIACRITIQUES = Pattern.compile("\\p{M}+");
  private static final Pattern ESPACES = Pattern.compile("\\s+");

  public CleDeNature {
    Assert.field("cle", value).notBlank();
  }

  static CleDeNature de(LibelleDeNature libelle) {
    String sansAccents = MARQUES_DIACRITIQUES.matcher(Normalizer.normalize(libelle.value(), Normalizer.Form.NFD)).replaceAll("");

    return new CleDeNature(ESPACES.matcher(sansAccents).replaceAll(" ").toLowerCase(Locale.ROOT));
  }
}
