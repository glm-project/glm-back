package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Le code que l'operateur tape au pupitre pour se designer : de 1 a 6 chiffres, zeros en tete compris.
 *
 * <p>
 * Il designe, il ne prouve rien : ce qui securise la saisie est l'identite d'appareil du poste et le controle
 * physique de l'atelier, pas ce code. Facultatif, comme au referentiel — un operateur sans identifiant n'est
 * simplement pas designable au pupitre.
 * </p>
 */
public record Identifiant(String value) {
  private static final Pattern FORMAT = Pattern.compile("^\\d{1,6}$");

  public Identifiant {
    Assert.field("identifiant", value).notBlank().matches(FORMAT);
  }

  public static Optional<Identifiant> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(Identifiant::new);
  }
}
