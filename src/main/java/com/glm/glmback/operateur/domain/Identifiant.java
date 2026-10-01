package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * L'identifiant que l'entreprise donne elle-meme a ses collaborateurs.
 *
 * <p>
 * Facultatif : toutes les entreprises clientes n'attribuent pas d'identifiant. Unique des qu'il est renseigne, la garde vivant dans
 * {@link OperateursService}.
 * </p>
 */
public record Identifiant(String value) {
  private static final int MAX_LENGTH = 50;

  public Identifiant {
    Assert.field("identifiant", value).notBlank().maxLength(MAX_LENGTH);
  }

  public static Optional<Identifiant> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(Identifiant::new);
  }
}
