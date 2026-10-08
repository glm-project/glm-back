package com.glm.glmback.pupitre.infrastructure.secondary;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataCategoriesDuPupitreRepository extends JpaRepository<CategorieDuPupitreEntity, String> {
  @Query("select categorie from CategorieDuPupitreEntity categorie order by categorie.rang, categorie.code")
  List<CategorieDuPupitreEntity> toutes();
}
