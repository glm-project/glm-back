package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.ElementId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Rattachement de l'activite a l'element. */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviValoriseEntity {

  @Id
  private UUID id;

  private UUID elementId;

  protected SuiviValoriseEntity() {
    /* Requis par JPA. */
  }

  ElementId element() {
    return new ElementId(elementId);
  }
}
