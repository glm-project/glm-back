package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du referentiel des categories de produit.
 *
 * <p>
 * Ce contexte lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext} : il n'en
 * retient que le code, la seule chose dont il a besoin pour refuser une categorie inconnue.
 * </p>
 */
@Entity
@Immutable
@Table(name = "categorie_de_produit")
class CategorieDeclareeEntity {

  @Id
  private String code;

  protected CategorieDeclareeEntity() {
    // Constructeur requis par JPA.
  }
}
