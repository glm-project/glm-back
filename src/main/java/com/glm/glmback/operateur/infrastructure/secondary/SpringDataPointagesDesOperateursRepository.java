package com.glm.glmback.operateur.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPointagesDesOperateursRepository extends JpaRepository<PointageDOperateurEntity, UUID> {
  Optional<PointageDOperateurEntity> findFirstByOperateurId(UUID operateurId);
}
