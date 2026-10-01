package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SequenceEnConflit;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Projection des faits en conflit, possedee par atelier et ignoree lors de la reconstitution du suivi. */
@Entity
@Table(name = "sequence_en_conflit")
class SequenceEnConflitDAtelierEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suivi_id", nullable = false)
  private SuiviDAtelierEntity suivi;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  @ElementCollection
  @CollectionTable(name = "pointage_en_conflit", joinColumns = @JoinColumn(name = "sequence_id"))
  @Column(name = "evenement_id")
  @OrderColumn(name = "ordre")
  private List<UUID> pointages = new ArrayList<>();

  protected SequenceEnConflitDAtelierEntity() {
    // Constructeur requis par JPA.
  }

  private SequenceEnConflitDAtelierEntity(SuiviDAtelierEntity suivi, SequenceEnConflit conflit) {
    this.suivi = suivi;
    id = conflit.pointages().getFirst().uuid();
    reporte(conflit);
  }

  static SequenceEnConflitDAtelierEntity from(SuiviDAtelierEntity suivi, SequenceEnConflit conflit) {
    return new SequenceEnConflitDAtelierEntity(suivi, conflit);
  }

  UUID id() {
    return id;
  }

  void reporte(SequenceEnConflit conflit) {
    operateurId = conflit.operateur().uuid();
    posteId = conflit.poste().map(PosteDeTravailId::uuid).orElse(null);
    pointages.clear();
    conflit.pointages().forEach(pointage -> pointages.add(pointage.uuid()));
  }
}
