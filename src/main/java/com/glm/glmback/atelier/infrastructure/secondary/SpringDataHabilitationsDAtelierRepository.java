package com.glm.glmback.atelier.infrastructure.secondary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataHabilitationsDAtelierRepository extends JpaRepository<HabilitationDAtelierEntity, UUID> {
  Optional<HabilitationDAtelierEntity> findFirstByOperateurIdAndPosteId(UUID operateurId, UUID posteId);

  /**
   * Les identifiants des postes, en valeurs : la cle de la table est composite, et l'identite JPA declaree n'en retient
   * que la moitie.
   */
  @Query(
    "select habilitation.posteId from HabilitationDAtelierEntity habilitation where habilitation.operateurId = :operateur order by habilitation.posteId"
  )
  List<UUID> postesHabilites(UUID operateur);
}
