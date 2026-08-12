package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDePresence;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
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
@Table(name = "evenement_de_presence")
class EvenementDePresenceEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "journee_id", nullable = false)
  private JourneeDeTravailEntity journee;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDePresence type;

  private String auteur;

  private Instant dateDeSurvenue;

  @Column(name = "date_d_enregistrement")
  private Instant dateDEnregistrement;

  private String annulationAuteur;

  private Instant annulationDate;

  private String annulationMotif;

  protected EvenementDePresenceEntity() {
    // Constructeur requis par JPA.
  }

  private EvenementDePresenceEntity(JourneeDeTravailEntity journee, EvenementDePresence evenement) {
    this.journee = journee;
    id = evenement.id().uuid();
    type = evenement.type();
    auteur = evenement.auteur().value();
    dateDeSurvenue = evenement.dateDeSurvenue();
    dateDEnregistrement = evenement.dateDEnregistrement();
    reporteLAnnulation(evenement);
  }

  static EvenementDePresenceEntity from(JourneeDeTravailEntity journee, EvenementDePresence evenement) {
    return new EvenementDePresenceEntity(journee, evenement);
  }

  UUID id() {
    return id;
  }

  void reporteLAnnulation(EvenementDePresence evenement) {
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

  EvenementDePresence toDomain() {
    EvenementDePresence evenement = EvenementDePresence.builder()
      .id(new EvenementDePresenceId(id))
      .type(type)
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
