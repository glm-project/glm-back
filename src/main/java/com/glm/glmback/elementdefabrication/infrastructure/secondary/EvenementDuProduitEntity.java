package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Vue en lecture seule du journal d'atelier : de quoi dire si un suivi a deja ete pointe. */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class EvenementDuProduitEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  protected EvenementDuProduitEntity() {
    // Constructeur requis par JPA.
  }
}
