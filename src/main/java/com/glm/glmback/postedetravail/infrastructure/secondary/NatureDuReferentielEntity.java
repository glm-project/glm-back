package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * La table du referentiel des natures, que ce contexte lit et alimente sans jamais importer son code.
 *
 * <p>
 * L'ecriture ne sert qu'au chemin de transition du libelle saisi en texte, et disparait avec lui (glm-back#130).
 * </p>
 */
@Entity
@Table(name = "nature_de_travail")
class NatureDuReferentielEntity {

  @Id
  private UUID id;

  @Column(length = 50)
  private String libelle;

  @Column(length = 50)
  private String cle;

  protected NatureDuReferentielEntity() {
    // Constructeur requis par JPA.
  }

  @SuppressWarnings("removal")
  private NatureDuReferentielEntity(NatureDuPoste nature) {
    id = nature.id().uuid();
    libelle = nature.libelle().value();
    cle = nature.libelle().cle();
  }

  static NatureDuReferentielEntity from(NatureDuPoste nature) {
    return new NatureDuReferentielEntity(nature);
  }

  NatureDuPoste toDomain() {
    return new NatureDuPoste(new NatureDeTravailId(id), new NatureDeTravail(libelle));
  }
}
