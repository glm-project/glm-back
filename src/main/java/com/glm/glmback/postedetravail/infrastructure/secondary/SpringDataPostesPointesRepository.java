package com.glm.glmback.postedetravail.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPostesPointesRepository extends JpaRepository<PostePointeEntity, UUID> {
  Optional<PostePointeEntity> findFirstByPosteId(UUID posteId);
}
