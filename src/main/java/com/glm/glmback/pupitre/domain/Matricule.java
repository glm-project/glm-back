package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Le code que l'operateur tape au pupitre pour se designer.
 *
 * <p>
 * Il designe, il ne prouve rien : ce qui securise la saisie est l'identite d'appareil du poste et le controle
 * physique de l'atelier, pas ce code. Facultatif, comme au referentiel — un operateur sans matricule n'est
 * simplement pas designable au pupitre.
 * </p>
 */
public record Matricule(String value) {
  private static final int MAX_LENGTH = 50;

  public Matricule {
    Assert.field("matricule", value).notBlank().maxLength(MAX_LENGTH);
  }

  public static Optional<Matricule> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(Matricule::new);
  }
}
