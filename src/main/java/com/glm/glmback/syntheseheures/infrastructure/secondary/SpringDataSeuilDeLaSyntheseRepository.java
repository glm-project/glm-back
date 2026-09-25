package com.glm.glmback.syntheseheures.infrastructure.secondary;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSeuilDeLaSyntheseRepository extends JpaRepository<SeuilDeLaSyntheseEntity, Short> {}
