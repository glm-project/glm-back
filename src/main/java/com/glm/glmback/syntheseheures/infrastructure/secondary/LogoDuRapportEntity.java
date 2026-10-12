package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.FormatDuLogo;
import com.glm.glmback.syntheseheures.domain.LogoDeLEntreprise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du logo, dans la ligne unique du parametrage. Nommee {@code ...DuRapportEntity} pour ne pas
 * entrer en conflit avec les autres entites qui lisent la meme table.
 */
@Entity
@Immutable
@Table(name = "parametrage")
class LogoDuRapportEntity {

  @Id
  private int id;

  @Column(name = "logo_contenu")
  private byte[] contenu;

  @Column(name = "logo_format", length = 4)
  private String format;

  protected LogoDuRapportEntity() {
    // Constructeur requis par JPA.
  }

  Optional<LogoDeLEntreprise> toDomain() {
    return Optional.ofNullable(format).map(lu -> new LogoDeLEntreprise(contenu, FormatDuLogo.valueOf(lu)));
  }
}
