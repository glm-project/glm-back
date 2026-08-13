package com.glm.glmback.operateur.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPostesHabilitablesRepository extends JpaRepository<PosteHabilitableEntity, UUID> {
  List<PosteHabilitableEntity> findByIdIn(Set<UUID> ids);
}
