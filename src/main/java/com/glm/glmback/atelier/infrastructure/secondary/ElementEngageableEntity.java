package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.CategorieDElement;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.NomDElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des elements de fabrication.
 *
 * <p>
 * L'atelier lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext} : il n'en
 * retient que l'identite, le nom et la categorie, les trois seules choses qu'il copie a l'engagement.
 * </p>
 */
@Entity
@Immutable
@Table(name = "element_de_fabrication")
class ElementEngageableEntity {

  @Id
  private UUID id;

  @Column(length = 30)
  private String categorie;

  private String nom;

  private String reference;

  protected ElementEngageableEntity() {
    // Constructeur requis par JPA.
  }

  ElementEngage toDomain() {
    return new ElementEngage(new ElementEngageId(id), new NomDElement(nom), new CategorieDElement(categorie));
  }
}
