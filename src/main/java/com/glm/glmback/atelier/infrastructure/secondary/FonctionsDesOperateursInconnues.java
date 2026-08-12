package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.FonctionsDesOperateurs;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.Operateur;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Aucune nature d'operation, faute de referentiel des ressources.
 *
 * <p>
 * L'application ne connait pas encore le profil des operateurs. La nature etant facultative par invariant et ne
 * bloquant rien, une entreprise sans metiers distincts retrouve ici son comportement nominal : c'est un port en
 * attente de sa source, pas un cas degrade.
 * </p>
 */
@Repository
class FonctionsDesOperateursInconnues implements FonctionsDesOperateurs {

  @Override
  public Optional<NatureDOperation> fonction(Operateur operateur) {
    return Optional.empty();
  }
}
