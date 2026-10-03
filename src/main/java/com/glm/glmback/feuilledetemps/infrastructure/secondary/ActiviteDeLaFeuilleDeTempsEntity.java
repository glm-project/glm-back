package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.Activite;
import com.glm.glmback.feuilledetemps.domain.ActiviteId;
import com.glm.glmback.feuilledetemps.domain.ActiviteInterpretee;
import com.glm.glmback.feuilledetemps.domain.CategorieDActivite;
import com.glm.glmback.feuilledetemps.domain.ElementId;
import com.glm.glmback.feuilledetemps.domain.NatureDOperation;
import com.glm.glmback.feuilledetemps.domain.Plage;
import com.glm.glmback.feuilledetemps.domain.PosteDeTravailId;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Vue privee en lecture seule de l'interpretation possedee par atelier. */
@Entity
@Immutable
@Table(name = "activite_d_atelier")
class ActiviteDeLaFeuilleDeTempsEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suivi_id")
  private SuiviDeLaFeuilleDeTempsEntity suivi;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

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

  @Column(name = "fin_au_plus_tard")
  @Convert(converter = ExactInstantConverter.class)
  private Instant finAuPlusTard;

  protected ActiviteDeLaFeuilleDeTempsEntity() {
    // Constructeur requis par JPA.
  }

  ActiviteInterpretee toDomain() {
    return ActiviteInterpretee.builder()
      .id(new ActiviteId(id))
      .activite(
        new Activite(
          new ElementId(suivi.elementId()),
          Optional.ofNullable(posteId).map(PosteDeTravailId::new),
          Optional.ofNullable(nature).map(NatureDOperation::new),
          categorie
        )
      )
      .plage(new Plage(debut, Optional.ofNullable(fin)))
      .echeance(echeance)
      .finAuPlusTard(Optional.ofNullable(finAuPlusTard));
  }
}
