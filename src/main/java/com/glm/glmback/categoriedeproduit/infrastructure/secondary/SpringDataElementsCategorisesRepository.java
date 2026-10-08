package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataElementsCategorisesRepository extends JpaRepository<ElementCategoriseEntity, UUID> {
  Optional<ElementCategoriseEntity> findFirstByCategorie(String categorie);

  @Query("select distinct element.categorie from ElementCategoriseEntity element where element.categorie in :categories")
  Set<String> findCategoriesUtiliseesParmi(Collection<String> categories);
}
