package com.glm.glmback.atelier.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SpringDataPointagesSignalesRepository
  extends JpaRepository<PointageSignaleEntity, UUID>, JpaSpecificationExecutor<PointageSignaleEntity> {}
