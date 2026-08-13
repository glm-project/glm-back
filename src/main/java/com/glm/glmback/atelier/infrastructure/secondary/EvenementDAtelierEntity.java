package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import jakarta.persistence.Column;
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

@Entity
@Table(name = "evenement_d_atelier")
class EvenementDAtelierEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suivi_id", nullable = false)
  private SuiviDAtelierEntity suivi;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDAtelier type;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  private String nature;

  private String auteur;

  private Instant dateDeSurvenue;

  @Column(name = "date_d_enregistrement")
  private Instant dateDEnregistrement;

  private String annulationAuteur;

  private Instant annulationDate;

  private String annulationMotif;

  protected EvenementDAtelierEntity() {
    // Constructeur requis par JPA.
  }

  private EvenementDAtelierEntity(SuiviDAtelierEntity suivi, EvenementDAtelier evenement) {
    this.suivi = suivi;
    id = evenement.id().uuid();
    type = evenement.type();
    operateurId = evenement.operateur().uuid();
    posteId = evenement.poste().map(PosteDeTravailId::uuid).orElse(null);
    nature = evenement.nature().map(NatureDOperation::value).orElse(null);
    auteur = evenement.auteur().value();
    dateDeSurvenue = evenement.dateDeSurvenue();
    dateDEnregistrement = evenement.dateDEnregistrement();
    reporteLAnnulation(evenement);
  }

  static EvenementDAtelierEntity from(SuiviDAtelierEntity suivi, EvenementDAtelier evenement) {
    return new EvenementDAtelierEntity(suivi, evenement);
  }

  UUID id() {
    return id;
  }

  /**
   * Seule part mutable d'un evenement : le reste est ecrit une fois pour toutes a l'insertion. Reposer des valeurs
   * identiques ne produit aucun UPDATE, c'est le controle de saletes de Hibernate qui tranche.
   */
  void reporteLAnnulation(EvenementDAtelier evenement) {
    annulationAuteur = evenement
      .annulation()
      .map(annulation -> annulation.auteur().value())
      .orElse(null);
    annulationDate = evenement.annulation().map(Annulation::date).orElse(null);
    annulationMotif = evenement
      .annulation()
      .map(annulation -> annulation.motif().value())
      .orElse(null);
  }

  EvenementDAtelier toDomain() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .id(new EvenementDAtelierId(id))
      .type(type)
      .operateur(new OperateurId(operateurId))
      .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
      .nature(Optional.ofNullable(nature).map(NatureDOperation::new))
      .auteur(new Auteur(auteur))
      .horodatage(new Horodatage(dateDeSurvenue, dateDEnregistrement));

    return annulation().map(evenement::annule).orElse(evenement);
  }

  private Optional<Annulation> annulation() {
    return Optional.ofNullable(annulationDate).map(date -> new Annulation(new Auteur(annulationAuteur), date, motif()));
  }

  private MotifDAnnulation motif() {
    return new MotifDAnnulation(annulationMotif);
  }
}
