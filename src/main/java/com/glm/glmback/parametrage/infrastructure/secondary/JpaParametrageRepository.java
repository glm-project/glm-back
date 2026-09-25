package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageIntrouvableException;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import org.springframework.stereotype.Repository;

@Repository
class JpaParametrageRepository implements ParametrageRepository {

  private final SpringDataParametrageRepository parametrages;

  JpaParametrageRepository(SpringDataParametrageRepository parametrages) {
    this.parametrages = parametrages;
  }

  @Override
  public Parametrage get() {
    return parametrages
      .findById(ParametrageEntity.LIGNE_UNIQUE)
      .map(ParametrageEntity::toDomain)
      .orElseThrow(ParametrageIntrouvableException::new);
  }

  @Override
  public void update(Parametrage parametrage) {
    if (!parametrages.existsById(ParametrageEntity.LIGNE_UNIQUE)) {
      throw new ParametrageIntrouvableException();
    }
    parametrages.save(ParametrageEntity.from(parametrage));
  }
}
