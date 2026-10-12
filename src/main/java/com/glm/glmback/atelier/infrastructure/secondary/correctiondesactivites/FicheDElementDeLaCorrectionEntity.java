package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.correctiondesactivites.DescriptionDElement;
import com.glm.glmback.atelier.domain.correctiondesactivites.FicheDElement;
import com.glm.glmback.atelier.domain.correctiondesactivites.ReferenceDElement;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des elements de fabrication, reduite a ce que la journee a corriger relit a chaque
 * appel : la reference et la description. Le nom et la categorie viennent du suivi, qui les a copies a l'engagement.
 */
@Entity
@Immutable
@Table(name = "element_de_fabrication")
class FicheDElementDeLaCorrectionEntity {

  @Id
  private UUID id;

  private String reference;

  private String description;

  protected FicheDElementDeLaCorrectionEntity() {
    // Constructeur requis par JPA.
  }

  FicheDElement toDomain() {
    return new FicheDElement(
      new ElementEngageId(id),
      Optional.ofNullable(reference).map(ReferenceDElement::new),
      Optional.ofNullable(description).map(DescriptionDElement::new)
    );
  }
}
