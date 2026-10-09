package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.Parametrage;
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
    return parametrages.findById(ParametrageEntity.UNIQUE).orElseThrow().toDomain();
  }

  @Override
  public Parametrage update(Parametrage parametrage) {
    parametrages.save(ParametrageEntity.from(parametrage));

    return parametrage;
  }
}
