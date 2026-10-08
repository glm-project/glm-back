package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataElementsCategorisesRepository extends JpaRepository<ElementCategoriseEntity, UUID> {
  Optional<ElementCategoriseEntity> findFirstByCategorie(String categorie);
}
