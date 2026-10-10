package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import com.glm.glmback.postedetravail.domain.NaturesDeclarees;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Les natures de l'entreprise courante, lues dans la table du referentiel.
 */
@Repository
class NaturesDuReferentiel implements NaturesDeclarees {

  private final SpringDataNaturesDuReferentielRepository natures;

  NaturesDuReferentiel(SpringDataNaturesDuReferentielRepository natures) {
    this.natures = natures;
  }

  @Override
  public Optional<NatureDuPoste> get(NatureDeTravailId id) {
    return natures.findById(id.uuid()).map(NatureDuReferentielEntity::toDomain);
  }
}
