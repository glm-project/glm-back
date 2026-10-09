package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import com.glm.glmback.syntheseheures.domain.ElementId;
import com.glm.glmback.syntheseheures.domain.NatureDOperation;
import com.glm.glmback.syntheseheures.domain.PointageDElement;
import com.glm.glmback.syntheseheures.domain.PointageId;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import com.glm.glmback.syntheseheures.domain.TypeDEvenementDAtelier;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule des evenements d'atelier : le journal brut de l'operateur.
 *
 * <p>
 * Les colonnes reprennent le style de nommage des entites de l'atelier, colonne par colonne : deux noms logiques pour
 * une meme colonne physique empecheraient Hibernate de demarrer.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class PointageDAtelierDeLaSyntheseEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDAtelier type;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  /**
   * Le libelle courant de la nature : le pointage n'en recopie que l'identifiant.
   */
  @Formula("(select referentiel.libelle from nature_de_travail referentiel where referentiel.id = nature_id)")
  private String nature;

  @Convert(converter = ExactInstantConverter.class)
  private Instant dateDeSurvenue;

  protected PointageDAtelierDeLaSyntheseEntity() {
    // Constructeur requis par JPA.
  }

  UUID suiviId() {
    return suiviId;
  }

  PointageDElement toDomain(ElementId element) {
    return PointageDElement.builder()
      .id(new PointageId(id))
      .type(type)
      .element(element)
      .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
      .nature(Optional.ofNullable(nature).map(NatureDOperation::new))
      .dateDeSurvenue(dateDeSurvenue);
  }
}
