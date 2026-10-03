package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPostesDuCoutRepository extends JpaRepository<PosteDuCoutEntity, UUID> {}
