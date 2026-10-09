package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.Logo;
import com.glm.glmback.parametrage.domain.LogoRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaLogoRepository implements LogoRepository {

  private final SpringDataLogoRepository logos;

  JpaLogoRepository(SpringDataLogoRepository logos) {
    this.logos = logos;
  }

  @Override
  public Optional<Logo> get() {
    return logos.findById(ParametrageEntity.UNIQUE).orElseThrow().toDomain();
  }

  @Override
  public Logo update(Logo logo) {
    logos.save(LogoEntity.from(logo));

    return logo;
  }
}
