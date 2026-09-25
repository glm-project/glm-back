package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.AmplitudeMaximale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du parametrage de l'entreprise.
 *
 * <p>
 * L'pupitre lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext} : il n'en
 * retient que l'amplitude maximale d'une journee.
 * </p>
 */
@Entity
@Immutable
@Table(name = "parametrage")
class SeuilDuPupitreEntity {

  /**
   * La seule ligne de la table, contrainte par le schema.
   */
  static final short LIGNE_UNIQUE = 1;

  @Id
  private short id;

  @Column(name = "amplitude_maximale_minutes")
  private int amplitudeMaximaleMinutes;

  protected SeuilDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  AmplitudeMaximale toDomain() {
    return new AmplitudeMaximale(Duration.ofMinutes(amplitudeMaximaleMinutes));
  }
}
