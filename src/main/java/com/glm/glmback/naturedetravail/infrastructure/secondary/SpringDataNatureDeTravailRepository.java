package com.glm.glmback.naturedetravail.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataNatureDeTravailRepository extends JpaRepository<NatureDeTravailEntity, UUID> {
  @Query("SELECT nature.id FROM NatureDeTravailEntity nature WHERE nature.cle = :cle")
  Optional<UUID> findIdByCle(@Param("cle") String cle);
}
