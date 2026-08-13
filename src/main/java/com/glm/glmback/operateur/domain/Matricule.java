package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * L'identifiant que l'entreprise donne elle-meme a ses collaborateurs.
 *
 * <p>
 * Facultatif : toutes les entreprises clientes ne matriculent pas. Unique des qu'il est renseigne, la garde vivant dans
 * {@link OperateursService}.
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
