package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Application;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.Ecart;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.GesteEnAttente;
import com.glm.glmback.atelier.domain.MotifDEcart;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttenteId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TraitementDuPointage;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Les deux natures de geste partagent une ligne : le type d'evenement s'y lit selon la nature, et seul un geste
 * d'atelier porte un suivi.
 */
@Entity
@Table(name = "pointage_en_attente")
class PointageEnAttenteEntity {

  @Id
  private UUID id;

  @Column(name = "evenement_du_pupitre")
  private UUID evenementDuPupitre;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private NatureDuGeste nature;

  @Column(name = "type_evenement", length = 20)
  private String typeEvenement;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Column(name = "poste_id")
  private UUID posteId;

  private Instant dateDeclaree;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private MotifDeMiseEnAttente motif;

  @Column(length = 100)
  private String auteur;

  private Instant dateDeReception;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDeTraitement traitementType;

  @Column(length = 100)
  private String traitementAuteur;

  private Instant traitementDate;

  @Column(length = 255)
  private String traitementMotif;

  protected PointageEnAttenteEntity() {
    // Constructeur requis par JPA.
  }

  private PointageEnAttenteEntity(PointageEnAttente pointage) {
    id = pointage.id().uuid();
    evenementDuPupitre = pointage.evenementDuPupitre();
    operateurId = pointage.geste().operateur().uuid();
    dateDeclaree = pointage.geste().dateDeclaree().orElse(null);
    switch (pointage.geste()) {
      case GesteDePresence presence -> {
        nature = NatureDuGeste.PRESENCE;
        typeEvenement = presence.type().name();
      }
      case GesteDAtelier atelier -> {
        nature = NatureDuGeste.ATELIER;
        typeEvenement = atelier.type().name();
        suiviId = atelier.suivi().uuid();
        posteId = atelier.poste().map(PosteDeTravailId::uuid).orElse(null);
      }
    }
    motif = pointage.motif();
    auteur = pointage.auteur().value();
    dateDeReception = pointage.dateDeReception();
    pointage.traitement().ifPresent(this::traite);
  }

  static PointageEnAttenteEntity from(PointageEnAttente pointage) {
    return new PointageEnAttenteEntity(pointage);
  }

  private void traite(TraitementDuPointage traitement) {
    traitementAuteur = traitement.auteur().value();
    traitementDate = traitement.date();
    switch (traitement) {
      case Application _ -> traitementType = TypeDeTraitement.APPLICATION;
      case Ecart ecart -> {
        traitementType = TypeDeTraitement.ECART;
        traitementMotif = ecart.motif().value();
      }
    }
  }

  PointageEnAttente toDomain() {
    return PointageEnAttente.builder()
      .id(new PointageEnAttenteId(id))
      .evenementDuPupitre(evenementDuPupitre)
      .geste(geste())
      .motif(motif)
      .auteur(new Auteur(auteur))
      .dateDeReception(dateDeReception)
      .traitement(Optional.ofNullable(traitementType).map(this::traitement));
  }

  private GesteEnAttente geste() {
    OperateurId operateur = new OperateurId(operateurId);
    Optional<Instant> declaree = Optional.ofNullable(dateDeclaree);

    return switch (nature) {
      case PRESENCE -> new GesteDePresence(operateur, TypeDEvenementDePresence.valueOf(typeEvenement), declaree);
      case ATELIER -> GesteDAtelier.builder()
        .suivi(new SuiviDAtelierId(suiviId))
        .operateur(operateur)
        .type(TypeDEvenementDAtelier.valueOf(typeEvenement))
        .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
        .dateDeclaree(declaree);
    };
  }

  private TraitementDuPointage traitement(TypeDeTraitement type) {
    Auteur par = new Auteur(traitementAuteur);

    return switch (type) {
      case APPLICATION -> new Application(par, traitementDate);
      case ECART -> new Ecart(par, traitementDate, new MotifDEcart(traitementMotif));
    };
  }

  enum NatureDuGeste {
    PRESENCE,
    ATELIER,
  }

  enum TypeDeTraitement {
    APPLICATION,
    ECART,
  }
}
