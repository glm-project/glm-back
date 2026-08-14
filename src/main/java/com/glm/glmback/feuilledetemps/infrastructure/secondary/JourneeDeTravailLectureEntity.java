package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'une journee de travail, reduite a ce qui permet de la retrouver.
 *
 * <p>
 * {@code debut} et {@code fin} sont les colonnes de projection ecrites par l'atelier : elles servent ici a borner la
 * requete, jamais a reconstruire la presence, qui se rejoue toujours depuis le journal.
 * </p>
 */
@Entity
@Immutable
@Table(name = "journee_de_travail")
class JourneeDeTravailLectureEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  private Instant debut;

  private Instant fin;

  protected JourneeDeTravailLectureEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }
}
