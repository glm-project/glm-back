package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSuivisDeLaSyntheseRepository extends JpaRepository<SuiviDeLaSyntheseEntity, UUID> {
  List<SuiviDeLaSyntheseEntity> findByIdIn(Set<UUID> ids);
}
