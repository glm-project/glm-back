package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.AmplitudeMaximale;
import com.glm.glmback.parametrage.domain.Auteur;
import com.glm.glmback.parametrage.domain.Modification;
import com.glm.glmback.parametrage.domain.Parametrage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "parametrage")
class ParametrageEntity {

  /**
   * La seule ligne de la table, contrainte par le schema.
   */
  static final short LIGNE_UNIQUE = 1;

  @Id
  private short id;

  @Column(name = "amplitude_maximale_minutes")
  private int amplitudeMaximaleMinutes;

  @Column(name = "modifie_par")
  private String modifiePar;

  @Column(name = "modifie_le")
  private Instant modifieLe;

  protected ParametrageEntity() {
    // Constructeur requis par JPA.
  }

  private ParametrageEntity(Parametrage parametrage) {
    id = LIGNE_UNIQUE;
    amplitudeMaximaleMinutes = Math.toIntExact(parametrage.amplitudeMaximale().value().toMinutes());
    modifiePar = parametrage
      .derniereModification()
      .map(modification -> modification.auteur().value())
      .orElse(null);
    modifieLe = parametrage.derniereModification().map(Modification::date).orElse(null);
  }

  static ParametrageEntity from(Parametrage parametrage) {
    return new ParametrageEntity(parametrage);
  }

  Parametrage toDomain() {
    return new Parametrage(new AmplitudeMaximale(Duration.ofMinutes(amplitudeMaximaleMinutes)), derniereModification());
  }

  private Optional<Modification> derniereModification() {
    return Optional.ofNullable(modifiePar).map(auteur -> new Modification(new Auteur(auteur), modifieLe));
  }
}
