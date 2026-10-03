package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.*;
import jakarta.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Lecture privee des sequences, y compris celles sans activite. */
@Entity
@Immutable
@Table(name = "sequence_en_conflit")
class SequenceEnConflitDuCoutEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suivi_id")
  private SuiviValoriseEntity suivi;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  @ElementCollection
  @CollectionTable(name = "pointage_en_conflit", joinColumns = @JoinColumn(name = "sequence_id"))
  @Column(name = "evenement_id")
  @OrderColumn(name = "ordre")
  private List<UUID> pointages;

  protected SequenceEnConflitDuCoutEntity() {
    /* Requis par JPA. */
  }

  UUID id() {
    return id;
  }

  List<UUID> pointages() {
    return pointages;
  }

  SequenceEnConflit toDomain(List<ActiviteId> activites, Map<UUID, PointageEnConflit> faits) {
    return SequenceEnConflit.builder()
      .element(suivi.element())
      .operateur(new OperateurId(operateurId))
      .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
      .activites(activites)
      .pointages(
        pointages
          .stream()
          .flatMap(pointage -> Optional.ofNullable(faits.get(pointage)).stream())
          .toList()
      );
  }
}
