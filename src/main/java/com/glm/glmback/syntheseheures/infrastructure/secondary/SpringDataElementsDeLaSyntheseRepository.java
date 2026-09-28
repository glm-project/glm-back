package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataElementsDeLaSyntheseRepository extends JpaRepository<ElementDeLaSyntheseEntity, UUID> {
  List<ElementDeLaSyntheseEntity> findByIdIn(Set<UUID> ids);
}
