package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.ActiviteId;
import com.glm.glmback.syntheseheures.domain.PointageId;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import com.glm.glmback.syntheseheures.domain.SequenceEnConflit;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Lecture privee des conflits possedes par atelier. */
@Entity
@Immutable
@Table(name = "sequence_en_conflit")
class SequenceEnConflitDeLaSyntheseEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suivi_id")
  private SuiviDeLaSyntheseEntity suivi;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  @ElementCollection
  @CollectionTable(name = "pointage_en_conflit", joinColumns = @JoinColumn(name = "sequence_id"))
  @Column(name = "evenement_id")
  @OrderColumn(name = "ordre")
  private List<UUID> pointages;

  protected SequenceEnConflitDeLaSyntheseEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  SequenceEnConflit toDomain(List<ActiviteId> activites) {
    return SequenceEnConflit.builder()
      .element(suivi.element().id())
      .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
      .activites(activites)
      .pointages(pointages.stream().map(PointageId::new).toList());
  }
}
