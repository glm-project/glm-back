package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import com.glm.glmback.postedetravail.domain.NaturesDeclarees;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Les natures de l'entreprise courante, lues dans la table du referentiel.
 */
@Repository
@SuppressWarnings("removal")
class NaturesDuReferentiel implements NaturesDeclarees {

  private final SpringDataNaturesDuReferentielRepository natures;

  NaturesDuReferentiel(SpringDataNaturesDuReferentielRepository natures) {
    this.natures = natures;
  }

  @Override
  public Optional<NatureDuPoste> parLibelle(NatureDeTravail libelle) {
    return natures.findByCle(libelle.cle()).map(NatureDuReferentielEntity::toDomain);
  }

  @Override
  public void declare(NatureDuPoste nature) {
    natures.save(NatureDuReferentielEntity.from(nature));
  }
}
