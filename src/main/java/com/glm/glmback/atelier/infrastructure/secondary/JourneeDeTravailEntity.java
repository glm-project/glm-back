package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.EvenementDePresence;
import com.glm.glmback.atelier.domain.JournalDePresence;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Periode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La ligne d'une journee de travail, et son journal de presence.
 *
 * <p>
 * {@code etat}, {@code debut} et {@code fin} sont des projections, ecrites depuis le domaine et jamais relues par
 * {@link #toDomain()}. Elles rendent exprimables en SQL le tri de la liste, la recherche de la journee en cours d'un
 * operateur et celle de la journee contenant un instant — cette derniere etant sur le chemin de chaque lecture de
 * temps effectif.
 * </p>
 */
@Entity
@Table(name = "journee_de_travail")
class JourneeDeTravailEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private EtatDePresence etat;

  private Instant debut;

  private Instant fin;

  @OneToMany(mappedBy = "journee", cascade = CascadeType.ALL)
  @OrderBy("dateDeSurvenue, id")
  private List<EvenementDePresenceEntity> journal = new ArrayList<>();

  protected JourneeDeTravailEntity() {
    // Constructeur requis par JPA.
  }

  private JourneeDeTravailEntity(JourneeDeTravail journee) {
    id = journee.id().uuid();
    operateurId = journee.operateur().uuid();
    reconcilie(journee);
  }

  static JourneeDeTravailEntity from(JourneeDeTravail journee) {
    return new JourneeDeTravailEntity(journee);
  }

  /**
   * Vrai si la ligne porte un evenement que l'agregat entrant ignore : meme controle d'obsolescence que pour un suivi
   * d'atelier, et pour la meme raison — un journal ne perd jamais un evenement.
   */
  boolean contientDesEvenementsAbsentsDe(JourneeDeTravail journee) {
    Set<UUID> entrants = journee
      .journal()
      .evenements()
      .stream()
      .map(evenement -> evenement.id().uuid())
      .collect(Collectors.toSet());

    return journal
      .stream()
      .map(EvenementDePresenceEntity::id)
      .anyMatch(identifiant -> !entrants.contains(identifiant));
  }

  /**
   * Meme rapprochement que pour un suivi d'atelier : l'operateur ne change jamais, et le journal se rejoint par
   * identifiant, un evenement connu n'ayant pu gagner que son annulation.
   */
  void reconcilie(JourneeDeTravail journee) {
    etat = journee.etat();
    debut = journee.debut().orElse(null);
    fin = journee.amplitude().map(Periode::fin).orElse(null);

    Map<UUID, EvenementDePresenceEntity> connus = journal
      .stream()
      .collect(Collectors.toMap(EvenementDePresenceEntity::id, Function.identity()));
    journee
      .journal()
      .evenements()
      .forEach(evenement -> rapproche(connus, evenement));
  }

  JourneeDeTravail toDomain() {
    return new JourneeDeTravail(
      new JourneeDeTravailId(id),
      new OperateurId(operateurId),
      new JournalDePresence(journal.stream().map(EvenementDePresenceEntity::toDomain).toList())
    );
  }

  private void rapproche(Map<UUID, EvenementDePresenceEntity> connus, EvenementDePresence evenement) {
    EvenementDePresenceEntity connu = connus.get(evenement.id().uuid());

    if (connu == null) {
      journal.add(EvenementDePresenceEntity.from(this, evenement));
    } else {
      connu.reporteLAnnulation(evenement);
    }
  }
}
