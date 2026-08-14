package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.EvenementDePresence;
import com.glm.glmback.feuilledetemps.domain.TypeDEvenementDePresence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un pointage de presence.
 *
 * <p>
 * {@code annulationDate} n'est jamais exposee au domaine : seule sa nullite compte, pour ecarter des la requete les
 * evenements annules. Ce contexte affiche du temps, pas l'historique des corrections.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_de_presence")
class EvenementDePresenceLectureEntity {

  @Id
  private UUID id;

  @Column(name = "journee_id")
  private UUID journeeId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDePresence type;

  private Instant dateDeSurvenue;

  private Instant annulationDate;

  protected EvenementDePresenceLectureEntity() {
    // Constructeur requis par JPA.
  }

  UUID journeeId() {
    return journeeId;
  }

  EvenementDePresence toDomain() {
    return new EvenementDePresence(type, dateDeSurvenue);
  }
}
