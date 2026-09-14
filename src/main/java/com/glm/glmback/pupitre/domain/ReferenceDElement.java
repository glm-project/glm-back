package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * L'identifiant que l'entreprise donne elle-meme a l'element, dans son propre systeme.
 *
 * <p>
 * Relue a chaque lecture, jamais copiee a l'engagement comme l'est le nom : une reference corrigee doit s'afficher
 * corrigee sur la tuile du pupitre. Facultative, toutes les entreprises n'en attribuent pas.
 * </p>
 */
public record ReferenceDElement(String value) {
  private static final int MAX_LENGTH = 100;

  public ReferenceDElement {
    Assert.field("reference de l'element", value).notBlank().maxLength(MAX_LENGTH);
  }

  public static Optional<ReferenceDElement> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(ReferenceDElement::new);
  }
}
