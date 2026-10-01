package com.glm.glmback.pupitre.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** L'existence d'un pointage actif distingue un suivi interrompu d'un suivi en attente. */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class EvenementDuPupitreEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  private Instant annulationDate;

  protected EvenementDuPupitreEntity() {
    // Constructeur requis par JPA.
  }
}
