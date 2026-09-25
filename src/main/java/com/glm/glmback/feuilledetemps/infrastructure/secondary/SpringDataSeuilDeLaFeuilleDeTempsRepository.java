package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSeuilDeLaFeuilleDeTempsRepository extends JpaRepository<SeuilDeLaFeuilleDeTempsEntity, Short> {}
