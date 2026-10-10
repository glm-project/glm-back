package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.FormatDuLogo;
import com.glm.glmback.coutderevient.domain.LogoDeLEntreprise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du logo, dans la ligne unique du parametrage. Nommee {@code ...DuCoutEntity} pour ne pas
 * entrer en conflit avec les entites du parametrage et du pupitre qui lisent la meme table.
 */
@Entity
@Immutable
@Table(name = "parametrage")
class LogoDuCoutEntity {

  @Id
  private int id;

  @Column(name = "logo_contenu")
  private byte[] contenu;

  @Column(name = "logo_format", length = 4)
  private String format;

  protected LogoDuCoutEntity() {
    // Constructeur requis par JPA.
  }

  Optional<LogoDeLEntreprise> toDomain() {
    return Optional.ofNullable(format).map(lu -> new LogoDeLEntreprise(contenu, FormatDuLogo.valueOf(lu)));
  }
}
