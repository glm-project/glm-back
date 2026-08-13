package com.glm.glmback.operateur.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataOperateurRepository extends JpaRepository<OperateurEntity, UUID>, JpaSpecificationExecutor<OperateurEntity> {
  @Query("SELECT operateur.id FROM OperateurEntity operateur WHERE operateur.nom = :nom AND operateur.prenom = :prenom")
  Optional<UUID> findIdByIdentite(@Param("nom") String nom, @Param("prenom") String prenom);

  @Query("SELECT operateur.id FROM OperateurEntity operateur WHERE operateur.matricule = :matricule")
  Optional<UUID> findIdByMatricule(@Param("matricule") String matricule);
}
