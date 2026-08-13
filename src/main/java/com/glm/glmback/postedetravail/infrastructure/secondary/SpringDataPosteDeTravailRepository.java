package com.glm.glmback.postedetravail.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataPosteDeTravailRepository
  extends JpaRepository<PosteDeTravailEntity, UUID>, JpaSpecificationExecutor<PosteDeTravailEntity>
{
  @Query("SELECT poste.id FROM PosteDeTravailEntity poste WHERE poste.libelle = :libelle")
  Optional<UUID> findIdByLibelle(@Param("libelle") String libelle);
}
