package com.glm.glmback.pupitre.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataElementsDuPupitreRepository extends JpaRepository<ElementDuPupitreEntity, UUID> {}
