package com.glm.glmback.atelier.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOperateursConnusRepository extends JpaRepository<OperateurConnuEntity, UUID> {
  List<OperateurConnuEntity> findByIdIn(Set<UUID> ids);
}
