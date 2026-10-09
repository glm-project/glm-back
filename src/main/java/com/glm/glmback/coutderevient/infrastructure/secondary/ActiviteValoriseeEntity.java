package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.*;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

/** Lecture privee des activites interpretees par atelier et de leurs tarifs captures. */
@Entity
@Immutable
@Table(name = "activite_d_atelier")
class ActiviteValoriseeEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suivi_id")
  private SuiviValoriseEntity suivi;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ouverture_id")
  private EvenementDAtelierValoriseEntity ouverture;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  /**
   * Le libelle courant de la nature : le pointage n'en recopie que l'identifiant.
   */
  @Formula("(select referentiel.libelle from nature_de_travail referentiel where referentiel.id = nature_id)")
  private String nature;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CategorieDActivite categorie;

  @Convert(converter = ExactInstantConverter.class)
  private Instant debut;

  @Convert(converter = ExactInstantConverter.class)
  private Instant echeance;

  @Convert(converter = ExactInstantConverter.class)
  private Instant fin;

  protected ActiviteValoriseeEntity() {
    /* Requis par JPA. */
  }

  ActiviteInterpretee toDomain() {
    return ActiviteInterpretee.builder()
      .id(new ActiviteId(id))
      .activite(
        new Activite(
          new OperateurId(operateurId),
          suivi.element(),
          Optional.ofNullable(posteId).map(PosteDeTravailId::new),
          Optional.ofNullable(nature).map(NatureDOperation::new),
          ouverture.coutHoraire(),
          ouverture.tauxHoraire(),
          categorie
        )
      )
      .plage(new Plage(debut, Optional.ofNullable(fin)))
      .echeance(echeance);
  }
}
