package com.glm.glmback.parametrage.infrastructure.secondary;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataLogoRepository extends JpaRepository<LogoEntity, Integer> {
  @Query("SELECT logo.version FROM LogoEntity logo WHERE logo.id = " + ParametrageEntity.UNIQUE)
  Optional<String> findVersion();
}
