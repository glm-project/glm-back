package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CoutHoraire;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.TauxHoraire;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
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
import java.math.BigDecimal;
import java.time.Duration;
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

  @Column(name = "activite_id")
  private UUID activiteId;

  @Column(name = "activite_visee_id")
  private UUID activiteViseeId;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  private String nature;

  @Column(name = "cout_horaire", precision = 10, scale = 2)
  private BigDecimal coutHoraire;

  @Column(name = "taux_horaire", precision = 10, scale = 2)
  private BigDecimal tauxHoraire;

  @Column(name = "duree_max_secondes")
  private Long dureeMaxSecondes;

  private String auteur;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private OrigineDuPointage origine;

  @Convert(converter = ExactInstantConverter.class)
  private Instant dateDeSurvenue;

  @Column(name = "date_d_enregistrement")
  @Convert(converter = ExactInstantConverter.class)
  private Instant dateDEnregistrement;

  protected EvenementDAtelierEntity() {
    // Constructeur requis par JPA.
  }

  private EvenementDAtelierEntity(SuiviDAtelierEntity suivi, EvenementDAtelier evenement) {
    this.suivi = suivi;
    id = evenement.id().uuid();
    type = evenement.type();
    activiteId = evenement.activite().map(ActiviteId::uuid).orElse(null);
    activiteViseeId = evenement.activiteVisee().map(ActiviteId::uuid).orElse(null);
    operateurId = evenement.operateur().uuid();
    posteId = evenement.poste().map(PosteDeTravailId::uuid).orElse(null);
    nature = evenement.nature().map(NatureDOperation::value).orElse(null);
    coutHoraire = evenement.coutHoraire().map(CoutHoraire::value).orElse(null);
    tauxHoraire = evenement.tauxHoraire().map(TauxHoraire::value).orElse(null);
    dureeMaxSecondes = evenement
      .dureeMax()
      .map(duree -> duree.value().toSeconds())
      .orElse(null);
    auteur = evenement.auteur().value();
    origine = evenement.origine();
    dateDeSurvenue = evenement.dateDeSurvenue();
    dateDEnregistrement = evenement.dateDEnregistrement();
  }

  static EvenementDAtelierEntity from(SuiviDAtelierEntity suivi, EvenementDAtelier evenement) {
    return new EvenementDAtelierEntity(suivi, evenement);
  }

  UUID id() {
    return id;
  }

  EvenementDAtelier toDomain() {
    return EvenementDAtelier.builder()
      .id(new EvenementDAtelierId(id))
      .type(type)
      .activite(Optional.ofNullable(activiteId).map(ActiviteId::new))
      .activiteVisee(Optional.ofNullable(activiteViseeId).map(ActiviteId::new))
      .operateur(new OperateurId(operateurId))
      .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
      .nature(Optional.ofNullable(nature).map(NatureDOperation::new))
      .coutHoraire(Optional.ofNullable(coutHoraire).map(CoutHoraire::new))
      .tauxHoraire(Optional.ofNullable(tauxHoraire).map(TauxHoraire::new))
      .dureeMax(Optional.ofNullable(dureeMaxSecondes).map(secondes -> new MaximumActivityDuration(Duration.ofSeconds(secondes))))
      .auteur(new Auteur(auteur))
      .origine(origine)
      .horodatage(new Horodatage(dateDeSurvenue, dateDEnregistrement));
  }
}
