package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataConflitsDuCoutRepository extends JpaRepository<SequenceEnConflitDuCoutEntity, UUID> {
  @Query(
    """
    select distinct sequence from SequenceEnConflitDuCoutEntity sequence join fetch sequence.suivi left join fetch sequence.pointages
    where sequence.suivi.elementId = :element order by sequence.id
    """
  )
  List<SequenceEnConflitDuCoutEntity> deLElement(UUID element);

  @Query(
    """
    select distinct sequence from SequenceEnConflitDuCoutEntity sequence join fetch sequence.suivi left join fetch sequence.pointages
    where sequence.operateurId in :operateurs order by sequence.id
    """
  )
  List<SequenceEnConflitDuCoutEntity> desOperateurs(Set<UUID> operateurs);
}
