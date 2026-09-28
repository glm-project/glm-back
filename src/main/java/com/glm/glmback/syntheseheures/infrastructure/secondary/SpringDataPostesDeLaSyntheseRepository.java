package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPostesDeLaSyntheseRepository extends JpaRepository<PosteDeLaSyntheseEntity, UUID> {
  List<PosteDeLaSyntheseEntity> findByIdIn(Set<UUID> ids);
}
