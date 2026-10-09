package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.FormatDImage;
import com.glm.glmback.parametrage.domain.Logo;
import com.glm.glmback.parametrage.domain.VersionDuLogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;

/**
 * Les colonnes du logo dans la ligne unique du parametrage : le logo se lit et s'ecrit sans toucher aux autres
 * reglages.
 */
@Entity
@Table(name = "parametrage")
class LogoEntity {

  @Id
  private int id;

  @Column(name = "logo_contenu")
  private byte[] contenu;

  @Column(name = "logo_format", length = 4)
  private String format;

  @Column(name = "logo_version", length = 16)
  private String version;

  protected LogoEntity() {
    // Constructeur requis par JPA.
  }

  private LogoEntity(Logo logo) {
    id = ParametrageEntity.UNIQUE;
    contenu = logo.contenu();
    format = logo.format().name();
    version = logo.version().value();
  }

  private LogoEntity(int id) {
    this.id = id;
  }

  static LogoEntity aucun() {
    return new LogoEntity(ParametrageEntity.UNIQUE);
  }

  static LogoEntity from(Logo logo) {
    return new LogoEntity(logo);
  }

  Optional<Logo> toDomain() {
    return Optional.ofNullable(version).map(lue -> new Logo(contenu, FormatDImage.valueOf(format), new VersionDuLogo(lue)));
  }
}
