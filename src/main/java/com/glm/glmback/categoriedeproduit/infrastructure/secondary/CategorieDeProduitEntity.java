package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.categoriedeproduit.domain.Rang;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorie_de_produit")
class CategorieDeProduitEntity {

  @Id
  @Column(length = 10)
  private String code;

  @Column
  private int rang;

  protected CategorieDeProduitEntity() {
    // Constructeur requis par JPA.
  }

  private CategorieDeProduitEntity(CategorieDeProduit categorie) {
    code = categorie.code().value();
    rang = categorie.rang().value();
  }

  static CategorieDeProduitEntity from(CategorieDeProduit categorie) {
    return new CategorieDeProduitEntity(categorie);
  }

  CategorieDeProduit toDomain() {
    return new CategorieDeProduit(new CodeDeCategorie(code), new Rang(rang));
  }
}
