package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.OperateurId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'une journee de travail, reduite a l'etat de presence qu'elle porte.
 *
 * <p>
 * {@code etat} et {@code debut} sont les colonnes de projection ecrites par l'atelier. Le pupitre les relit plutot que
 * de rejouer le journal de presence, contrairement a ce qu'il fait du journal d'atelier : l'etat courant est ici la
 * seule chose demandee, pour tous les operateurs a la fois, et le replier supposerait de rapporter tous les journaux
 * ouverts a chaque synchronisation. Le journal reste la source de verite de qui le lit — {@code feuilledetemps} et
 * {@code syntheseheures}, qui comptent des durees.
 * </p>
 */
@Entity
@Immutable
@Table(name = "journee_de_travail")
class JourneeDuPupitreEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private EtatDePresence etat;

  private Instant debut;

  protected JourneeDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  OperateurId operateur() {
    return new OperateurId(operateurId);
  }

  EtatDePresence etat() {
    return etat;
  }
}
