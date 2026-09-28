package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.DescriptionDElement;
import com.glm.glmback.syntheseheures.domain.ElementId;
import com.glm.glmback.syntheseheures.domain.FicheDElement;
import com.glm.glmback.syntheseheures.domain.ReferenceDElement;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des elements de fabrication, reduite a ce que le releve relit a chaque appel : la
 * reference et la description. Le nom et le type viennent du suivi, qui les a copies a l'engagement.
 */
@Entity
@Immutable
@Table(name = "element_de_fabrication")
class ElementDeLaSyntheseEntity {

  @Id
  private UUID id;

  private String reference;

  private String description;

  protected ElementDeLaSyntheseEntity() {
    // Constructeur requis par JPA.
  }

  FicheDElement toDomain() {
    return new FicheDElement(
      new ElementId(id),
      Optional.ofNullable(reference).map(ReferenceDElement::new),
      Optional.ofNullable(description).map(DescriptionDElement::new)
    );
  }
}
