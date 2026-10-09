package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.VersionDuLogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la version du logo, dans la ligne unique du parametrage. Ni le contenu ni le format ne sont
 * lus : le referentiel ne porte que la version. Nommee {@code ...DuPupitreEntity} pour ne pas entrer en conflit avec
 * les beans JPA du parametrage qui ecrivent la meme table.
 */
@Entity
@Immutable
@Table(name = "parametrage")
class LogoDuPupitreEntity {

  @Id
  private int id;

  @Column(name = "logo_version")
  private String version;

  protected LogoDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  Optional<VersionDuLogo> toDomain() {
    return Optional.ofNullable(version).map(VersionDuLogo::new);
  }
}
