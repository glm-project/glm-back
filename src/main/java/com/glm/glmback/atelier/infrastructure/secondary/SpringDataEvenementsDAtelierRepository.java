package com.glm.glmback.atelier.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEvenementsDAtelierRepository extends JpaRepository<EvenementDAtelierEntity, UUID> {}
