package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.ElementValorise;
import com.glm.glmback.coutderevient.domain.NomDElement;
import com.glm.glmback.coutderevient.domain.TypeDElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des elements de fabrication.
 *
 * <p>
 * Ce contexte lit la table du voisin sans jamais importer son code, annote {@code BusinessContext} — exactement ce
 * que l'atelier fait deja sur la meme table. Il n'en retient que l'identite, le nom et le type : c'est tout ce que le
 * rapport a besoin de nommer.
 * </p>
 */
@Entity
@Immutable
@Table(name = "element_de_fabrication")
class ElementValorisableEntity {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private TypeDElement type;

  private String nom;

  protected ElementValorisableEntity() {
    // Constructeur requis par JPA.
  }

  ElementValorise toDomain() {
    return new ElementValorise(new ElementId(id), new NomDElement(nom), type);
  }
}
