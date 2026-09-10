package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOperateursSyntheseRepository extends JpaRepository<OperateurSyntheseEntity, UUID> {}
