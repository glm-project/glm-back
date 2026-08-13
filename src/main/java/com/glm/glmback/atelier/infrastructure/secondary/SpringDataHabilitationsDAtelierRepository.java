package com.glm.glmback.atelier.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataHabilitationsDAtelierRepository extends JpaRepository<HabilitationDAtelierEntity, UUID> {
  Optional<HabilitationDAtelierEntity> findFirstByOperateurIdAndPosteId(UUID operateurId, UUID posteId);
}
