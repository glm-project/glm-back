package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Dernier fait utilise uniquement par la presence, jusqu'a son retrait de la feuille. */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class PointageDAtelierDeLaFeuilleDeTempsEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  private Instant dateDeSurvenue;
  private Instant annulationDate;

  protected PointageDAtelierDeLaFeuilleDeTempsEntity() {
    // Constructeur requis par JPA.
  }

  Instant dateDeSurvenue() {
    return dateDeSurvenue;
  }
}
