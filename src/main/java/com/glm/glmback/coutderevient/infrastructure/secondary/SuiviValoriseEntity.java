package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.PassageEnAtelier;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Rattachement de l'activite a l'element, et cloture du passage en atelier. */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviValoriseEntity {

  @Id
  private UUID id;

  private UUID elementId;

  @Convert(converter = ExactInstantConverter.class)
  private Instant clotureDateDeSurvenue;

  protected SuiviValoriseEntity() {
    /* Requis par JPA. */
  }

  ElementId element() {
    return new ElementId(elementId);
  }

  PassageEnAtelier passage() {
    return new PassageEnAtelier(Optional.ofNullable(clotureDateDeSurvenue));
  }
}
