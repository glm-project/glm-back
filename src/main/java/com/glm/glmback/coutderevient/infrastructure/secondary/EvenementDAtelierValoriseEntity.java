package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.CoutHoraire;
import com.glm.glmback.coutderevient.domain.EvenementDAtelier;
import com.glm.glmback.coutderevient.domain.NatureDOperation;
import com.glm.glmback.coutderevient.domain.OperateurId;
import com.glm.glmback.coutderevient.domain.PosteDeTravailId;
import com.glm.glmback.coutderevient.domain.TauxHoraire;
import com.glm.glmback.coutderevient.domain.TypeDEvenementDAtelier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un pointage sur un element.
 *
 * <p>
 * {@code annulationDate} n'est jamais exposee au domaine : seule sa nullite compte, pour ecarter des la requete les
 * evenements annules. Ce contexte chiffre du temps, pas l'historique des corrections.
 * </p>
 *
 * <p>
 * Le cout horaire et le taux horaire sont relus tels que l'atelier les a figes a la saisie. Les recalculer depuis le
 * referentiel ferait varier le rapport a chaque revision de tarif, et reecrirait le prix d'heures deja passees.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class EvenementDAtelierValoriseEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDAtelier type;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  private String nature;

  @Column(name = "cout_horaire", precision = 10, scale = 2)
  private BigDecimal coutHoraire;

  @Column(name = "taux_horaire", precision = 10, scale = 2)
  private BigDecimal tauxHoraire;

  private Instant dateDeSurvenue;

  private Instant annulationDate;

  protected EvenementDAtelierValoriseEntity() {
    // Constructeur requis par JPA.
  }

  UUID suiviId() {
    return suiviId;
  }

  EvenementDAtelier toDomain() {
    return EvenementDAtelier.builder()
      .type(type)
      .operateur(new OperateurId(operateurId))
      .poste(Optional.ofNullable(posteId).map(PosteDeTravailId::new))
      .nature(Optional.ofNullable(nature).map(NatureDOperation::new))
      .coutHoraire(CoutHoraire.of(coutHoraire))
      .tauxHoraire(TauxHoraire.of(tauxHoraire))
      .dateDeSurvenue(dateDeSurvenue);
  }
}
