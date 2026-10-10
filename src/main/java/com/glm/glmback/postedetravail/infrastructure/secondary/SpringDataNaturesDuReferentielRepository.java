package com.glm.glmback.postedetravail.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataNaturesDuReferentielRepository extends JpaRepository<NatureDuReferentielEntity, UUID> {}
