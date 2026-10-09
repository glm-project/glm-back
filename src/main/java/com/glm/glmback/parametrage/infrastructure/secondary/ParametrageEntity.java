package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.DureeMaxDActivite;
import com.glm.glmback.parametrage.domain.Parametrage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.util.Optional;

@Entity
@Table(name = "parametrage")
class ParametrageEntity {

  static final int UNIQUE = 1;

  @Id
  private int id;

  @Column(name = "duree_max_d_activite_secondes")
  private Long dureeMaxDActiviteSecondes;

  protected ParametrageEntity() {
    // Constructeur requis par JPA.
  }

  Parametrage toDomain() {
    return new Parametrage(
      Optional.ofNullable(dureeMaxDActiviteSecondes)
        .map(secondes -> new DureeMaxDActivite(Duration.ofSeconds(secondes)))
        .orElseGet(DureeMaxDActivite::parDefaut)
    );
  }
}
