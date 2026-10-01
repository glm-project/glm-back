package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * L'identifiant que l'entreprise donne elle-meme a ses collaborateurs : de 1 a 6 chiffres, tapes au pave du pupitre.
 *
 * <p>
 * Une chaine de chiffres, pas un nombre : {@code 007} reste distinct de {@code 7}. Facultatif : toutes les entreprises
 * clientes n'attribuent pas d'identifiant. Unique des qu'il est renseigne, la garde vivant dans
 * {@link OperateursService}.
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
