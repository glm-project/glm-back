package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataCategorieDeProduitRepository extends JpaRepository<CategorieDeProduitEntity, String> {
  @Query("SELECT MAX(categorie.rang) FROM CategorieDeProduitEntity categorie")
  Optional<Integer> findDernierRang();
}
