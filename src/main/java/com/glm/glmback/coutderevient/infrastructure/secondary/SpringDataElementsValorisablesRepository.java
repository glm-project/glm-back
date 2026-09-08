package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataElementsValorisablesRepository extends JpaRepository<ElementValorisableEntity, UUID> {}
