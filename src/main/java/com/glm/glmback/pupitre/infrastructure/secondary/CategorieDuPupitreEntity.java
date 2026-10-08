package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.CategorieDElement;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du referentiel des categories de produit.
 *
 * <p>
 * Le rang n'est lu que pour trier, dans la requete : le pupitre recoit les codes deja ranges. Nommee
 * {@code ...DuPupitreEntity} pour ne pas entrer en conflit avec les beans JPA homonymes des autres contextes qui
 * lisent la meme table.
 * </p>
 */
@Entity
@Immutable
@Table(name = "categorie_de_produit")
class CategorieDuPupitreEntity {

  @Id
  private String code;

  private int rang;

  protected CategorieDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  CategorieDElement toDomain() {
    return new CategorieDElement(code);
  }
}
