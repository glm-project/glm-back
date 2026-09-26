package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CibleDuSignalement;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.MotifDeSignalement;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageSignale;
import com.glm.glmback.atelier.domain.PointageSignaleId;
import com.glm.glmback.atelier.domain.Resolution;
import com.glm.glmback.atelier.domain.TypeDeCible;
import com.glm.glmback.atelier.domain.TypeDeResolution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Les motifs tiennent dans une colonne, separes par une virgule : ils sont peu nombreux, et leurs noms ne se
 * recouvrent pas, ce qui rend le filtre par motif exprimable en SQL.
 */
@Entity
@Table(name = "pointage_signale")
class PointageSignaleEntity {

  static final String SEPARATEUR = ",";

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "cible_type", length = 32)
  private TypeDeCible cibleType;

  @Column(name = "cible_id")
  private UUID cibleId;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(length = 200)
  private String motifs;

  private Instant dateDeSurvenue;

  @Column(name = "date_d_enregistrement")
  private Instant dateDEnregistrement;

  private Instant dateDeclaree;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDeResolution resolutionType;

  @Column(length = 100)
  private String resolutionAuteur;

  private Instant resolutionDate;

  protected PointageSignaleEntity() {
    // Constructeur requis par JPA.
  }

  private PointageSignaleEntity(PointageSignale pointage) {
    id = pointage.id().uuid();
    cibleType = pointage.cible().type();
    cibleId = pointage.cible().id();
    operateurId = pointage.operateur().uuid();
    motifs = pointage.motifs().stream().map(Enum::name).sorted().collect(Collectors.joining(SEPARATEUR));
    dateDeSurvenue = pointage.horodatage().dateDeSurvenue();
    dateDEnregistrement = pointage.horodatage().dateDEnregistrement();
    dateDeclaree = pointage.dateDeclaree().orElse(null);
    resolutionType = pointage.resolution().map(Resolution::type).orElse(null);
    resolutionAuteur = pointage
      .resolution()
      .map(resolution -> resolution.auteur().value())
      .orElse(null);
    resolutionDate = pointage.resolution().map(Resolution::date).orElse(null);
  }

  static PointageSignaleEntity from(PointageSignale pointage) {
    return new PointageSignaleEntity(pointage);
  }

  PointageSignale toDomain() {
    return PointageSignale.builder()
      .id(new PointageSignaleId(id))
      .cible(new CibleDuSignalement(cibleType, cibleId))
      .operateur(new OperateurId(operateurId))
      .motifs(Arrays.stream(motifs.split(SEPARATEUR)).map(MotifDeSignalement::valueOf).collect(Collectors.toSet()))
      .horodatage(new Horodatage(dateDeSurvenue, dateDEnregistrement))
      .dateDeclaree(Optional.ofNullable(dateDeclaree))
      .resolution(Optional.ofNullable(resolutionType).map(type -> new Resolution(type, new Auteur(resolutionAuteur), resolutionDate)));
  }
}
