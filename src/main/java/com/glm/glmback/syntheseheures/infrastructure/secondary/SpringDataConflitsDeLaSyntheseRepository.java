package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataConflitsDeLaSyntheseRepository extends JpaRepository<SequenceEnConflitDeLaSyntheseEntity, UUID> {
  @Query(
    """
    select distinct sequence from SequenceEnConflitDeLaSyntheseEntity sequence
    join fetch sequence.suivi left join fetch sequence.pointages
    where sequence.operateurId = :operateur
    order by sequence.id
    """
  )
  List<SequenceEnConflitDeLaSyntheseEntity> de(UUID operateur);
}
