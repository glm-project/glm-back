package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Vue en lecture seule des activites projetees par l'atelier : de quoi dire si l'une court encore. */
@Entity
@Immutable
@Table(name = "activite_d_atelier")
class ActiviteDuProduitEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Convert(converter = ExactInstantConverter.class)
  private Instant echeance;

  @Convert(converter = ExactInstantConverter.class)
  private Instant fin;

  protected ActiviteDuProduitEntity() {
    // Constructeur requis par JPA.
  }

  UUID suivi() {
    return suiviId;
  }
}
