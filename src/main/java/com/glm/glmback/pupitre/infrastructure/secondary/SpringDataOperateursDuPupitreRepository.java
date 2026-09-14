package com.glm.glmback.pupitre.infrastructure.secondary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataOperateursDuPupitreRepository extends JpaRepository<OperateurDuPupitreEntity, UUID> {
  @Query(
    """
    select distinct operateur
    from OperateurDuPupitreEntity operateur
    left join fetch operateur.postes
    order by operateur.nom, operateur.prenom, operateur.id
    """
  )
  List<OperateurDuPupitreEntity> tous();
}
