package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOperateursLectureRepository extends JpaRepository<OperateurLectureEntity, UUID> {}
