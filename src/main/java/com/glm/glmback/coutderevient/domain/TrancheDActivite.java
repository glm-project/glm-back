package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.Optional;

/**
 * Une activite et la periode fermee pendant laquelle elle a couru : la matiere du calcul.
 *
 * <p>
 * C'est ce que devient un {@link IntervalleDActivite} une fois le temps arrete. Tout ce qui suit — decoupage sur les
 * sous-periodes de parallelisme, valorisation, agregation par nature — ne travaille plus que sur des tranches, donc
 * sur des durees connues.
 * </p>
 */
public record TrancheDActivite(Activite activite, Periode periode) {
  public TrancheDActivite {
    Assert.notNull("activite", activite);
    Assert.notNull("periode", periode);
  }

  public Duration duree() {
    return periode.duree();
  }

  public OperateurId operateur() {
    return activite.operateur();
  }

  /**
   * La meme tranche reduite a la periode donnee, s'il en reste quelque chose. C'est par la que passe le decoupage sur
   * les sous-periodes ou le diviseur est constant.
   */
  public Optional<TrancheDActivite> reduiteA(Periode autre) {
    return periode.intersection(autre).map(part -> new TrancheDActivite(activite, part));
  }
}
