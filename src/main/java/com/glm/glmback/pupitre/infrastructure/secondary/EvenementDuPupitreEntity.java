package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.CleDActivite;
import com.glm.glmback.pupitre.domain.EvenementDuPupitre;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import com.glm.glmback.pupitre.domain.TypeDePointage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un pointage du journal d'atelier, telle que le pupitre la lit.
 *
 * <p>
 * {@code annulationDate} n'est jamais exposee au domaine : seule sa nullite compte, pour ecarter des la requete les
 * evenements annules. Ni auteur, ni cout horaire, ni taux horaire — le pupitre affiche des tuiles, il n'audite ni ne
 * valorise.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class EvenementDuPupitreEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDePointage type;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  private Instant dateDeSurvenue;

  @Column(name = "date_d_enregistrement")
  private Instant dateDEnregistrement;

  private Instant annulationDate;

  protected EvenementDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  UUID suiviId() {
    return suiviId;
  }

  EvenementDuPupitre toDomain() {
    return new EvenementDuPupitre(
      type,
      new CleDActivite(new OperateurId(operateurId), Optional.ofNullable(posteId).map(PosteDeTravailId::new)),
      dateDeSurvenue
    );
  }
}
