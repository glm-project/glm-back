package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule des evenements d'atelier : de quoi retrouver le dernier pointage d'un operateur, rien d'autre.
 *
 * <p>
 * Les colonnes reprennent le style de nommage des entites de l'atelier, colonne par colonne : deux noms logiques pour
 * une meme colonne physique empecheraient Hibernate de demarrer.
 * </p>
 */
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
