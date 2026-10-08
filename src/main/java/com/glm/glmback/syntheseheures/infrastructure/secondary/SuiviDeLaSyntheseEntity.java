package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.CategorieDElement;
import com.glm.glmback.syntheseheures.domain.ElementEngage;
import com.glm.glmback.syntheseheures.domain.ElementId;
import com.glm.glmback.syntheseheures.domain.NomDElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un passage en atelier : l'element avec le nom et la categorie copies a l'engagement, sans interpreter son journal.
 *
 * <p>
 * Le nommage des colonnes reprend celui de l'entite d'ecriture de l'atelier, colonne par colonne : deux entites qui
 * donnent a la meme colonne physique deux noms logiques differents empechent Hibernate de demarrer.
 * </p>
 */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviDeLaSyntheseEntity {

  @Id
  private UUID id;

  private UUID elementId;

  private String elementNom;

  @Column(length = 30)
  private String elementCategorie;

  protected SuiviDeLaSyntheseEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  ElementEngage element() {
    return new ElementEngage(new ElementId(elementId), new NomDElement(elementNom), new CategorieDElement(elementCategorie));
  }
}
