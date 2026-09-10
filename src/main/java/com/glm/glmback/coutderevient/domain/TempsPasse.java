package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/**
 * Le temps passe sur un element, separe en bon travail et en reprise de non conformite.
 *
 * <p>
 * Une piece ratee se refait sur le meme element et au meme tarif, mais comptee a part : c'est ce que le client veut
 * savoir a la cloture, combien de temps a fait du bon travail et combien a refait.
 * </p>
 */
public record TempsPasse(Duration travail, Duration nonConformite) {
  public static final TempsPasse AUCUN = new TempsPasse(Duration.ZERO, Duration.ZERO);

  public TempsPasse {
    Assert.notNull("temps de travail", travail);
    Assert.notNull("temps en non conformite", nonConformite);
  }

  public Duration total() {
    return travail.plus(nonConformite);
  }

  public TempsPasse plus(TempsPasse autre) {
    return new TempsPasse(travail.plus(autre.travail), nonConformite.plus(autre.nonConformite));
  }
}
