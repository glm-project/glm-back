package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.CascadeType;
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
import java.util.UUID;

/**
 * Une activite interpretee du journal d'un suivi, projetee a chaque ecriture et jamais relue par le domaine.
 *
 * <p>
 * Elle ne porte que ce qui ne depend pas de l'instant de lecture : son debut, son echeance, sa fin reelle, et si une
 * sequence en conflit la laisse a resoudre, auquel cas elle n'a pas de fin. Qu'une activite interpretable soit en cours
 * ou terminee automatiquement se juge a la lecture, en comparant l'echeance a l'instant d'evaluation.
 * </p>
 */
@Entity
@Table(name = "activite_d_atelier")
class ActiviteDAtelierEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suivi_id", nullable = false)
  private SuiviDAtelierEntity suivi;

  @Column(name = "ouverture_id")
  private UUID ouvertureId;

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

  @Column(name = "a_resoudre")
  private boolean aResoudre;

  @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "sequence_id")
  private SequenceEnConflitDAtelierEntity sequence;

  private Integer ordreDansSequence;

  protected ActiviteDAtelierEntity() {
    // Constructeur requis par JPA.
  }

  private ActiviteDAtelierEntity(SuiviDAtelierEntity suivi, Activite activite) {
    this.suivi = suivi;
    id = activite.id().uuid();
    reporte(activite);
  }

  static ActiviteDAtelierEntity from(SuiviDAtelierEntity suivi, Activite activite) {
    return new ActiviteDAtelierEntity(suivi, activite);
  }

  void rattacheA(SequenceEnConflitDAtelierEntity conflit, Integer ordre) {
    sequence = conflit;
    ordreDansSequence = ordre;
  }

  UUID id() {
    return id;
  }

  /**
   * Reporte l'interpretation courante de l'activite : un debut corrige deplace son echeance, un geste lui donne une fin
   * reelle, le remplacant d'une correction en devient l'ouverture, et une contradiction la laisse a resoudre.
   */
  void reporte(Activite activite) {
    ouvertureId = activite.ouvrant().id().uuid();
    operateurId = activite.cle().operateur().uuid();
    posteId = activite.cle().poste().map(PosteDeTravailId::uuid).orElse(null);
    nature = activite.ouvrant().nature().map(NatureDOperation::value).orElse(null);
    categorie = activite.categorie();
    debut = activite.debut();
    echeance = activite.echeance().value();
    fin = activite.fin().orElse(null);
    finAuPlusTard = activite.finAuPlusTard().orElse(null);
    aResoudre = activite.aResoudre();
  }
}
