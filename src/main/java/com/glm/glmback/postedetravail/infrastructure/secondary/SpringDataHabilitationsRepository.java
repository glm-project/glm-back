package com.glm.glmback.postedetravail.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataHabilitationsRepository extends JpaRepository<OperateurHabiliteEntity, UUID> {
  Optional<OperateurHabiliteEntity> findFirstByPosteId(UUID posteId);
}
