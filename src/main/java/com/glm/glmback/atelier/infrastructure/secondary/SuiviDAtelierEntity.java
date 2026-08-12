package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDElementEngage;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La ligne d'un suivi d'atelier, et son journal.
 *
 * <p>
 * {@code etat} est une projection : elle s'ecrit depuis le domaine a chaque enregistrement et n'est jamais relue par
 * {@link #toDomain()}, qui rejoue toujours le journal. Le journal reste donc la seule source de verite, et cette
 * colonne n'est qu'un index qui rend le filtre de l'ecran d'atelier exprimable en SQL. Elle ne depend que du journal,
 * jamais de l'instant courant : elle est donc stable entre deux ecritures.
 * </p>
 */
@Entity
@Table(name = "suivi_d_atelier")
class SuiviDAtelierEntity {

  @Id
  private UUID id;

  private UUID elementId;

  private String elementNom;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private TypeDElementEngage elementType;

  private String engagementAuteur;

  private Instant engagementDate;

  private String clotureAuteur;

  private Instant clotureDateDeSurvenue;

  @Column(name = "cloture_date_d_enregistrement")
  private Instant clotureDateDEnregistrement;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private EtatDAtelier etat;

  @OneToMany(mappedBy = "suivi", cascade = CascadeType.ALL)
  @OrderBy("dateDeSurvenue, id")
  private List<EvenementDAtelierEntity> journal = new ArrayList<>();

  protected SuiviDAtelierEntity() {
    // Constructeur requis par JPA.
  }

  private SuiviDAtelierEntity(SuiviDAtelier suivi) {
    id = suivi.id().uuid();
    elementId = suivi.element().id().uuid();
    elementNom = suivi.element().nom().value();
    elementType = suivi.element().type();
    engagementAuteur = suivi.engagement().auteur().value();
    engagementDate = suivi.engagement().date();
    reconcilie(suivi);
  }

  static SuiviDAtelierEntity from(SuiviDAtelier suivi) {
    return new SuiviDAtelierEntity(suivi);
  }

  /**
   * Vrai si la ligne porte un evenement que l'agregat entrant ignore.
   *
   * <p>
   * Le journal ne perd jamais un evenement : une annulation le marque, elle ne le retire pas. Un evenement stocke
   * absent de l'agregat entrant ne peut donc signifier qu'une chose — cet agregat a ete calcule sur un journal deja
   * perime. C'est le seul controle d'obsolescence necessaire, et il ne coute rien : la collection est deja chargee
   * pour le rapprochement.
   * </p>
   */
  boolean contientDesEvenementsAbsentsDe(SuiviDAtelier suivi) {
    Set<UUID> entrants = suivi
      .journal()
      .evenements()
      .stream()
      .map(evenement -> evenement.id().uuid())
      .collect(Collectors.toSet());

    return journal
      .stream()
      .map(EvenementDAtelierEntity::id)
      .anyMatch(identifiant -> !entrants.contains(identifiant));
  }

  /**
   * Rapproche la ligne de l'agregat que le domaine vient de reconstruire.
   *
   * <p>
   * L'element et l'engagement ne changent jamais apres la creation, ils ne sont donc pas retouches. Le journal, lui,
   * se rapproche par identifiant : un evenement inconnu est insere, un evenement connu ne peut avoir gagne que son
   * annulation, et aucun n'est jamais supprime — une annulation marque l'evenement, elle ne le retire pas. Le cout
   * d'un pointage est donc d'une ligne, quelle que soit la longueur du journal.
   * </p>
   *
   * <p>
   * Si un journal devenait assez long pour que la lecture de la collection pese, la sortie serait un upsert natif
   * garde ({@code on conflict (id) do update ... where ... is distinct from ...}), qui epargne a PostgreSQL toute
   * version de tuple sur les lignes inchangees. Il n'est pas retenu aujourd'hui : il ne gagne qu'un SELECT indexe de
   * quelques dizaines de lignes courtes, contre une scission permanente entre lecture JPA et ecriture JDBC.
   * </p>
   */
  void reconcilie(SuiviDAtelier suivi) {
    reporteLaCloture(suivi.cloture());
    etat = suivi.etat();

    Map<UUID, EvenementDAtelierEntity> connus = journal
      .stream()
      .collect(Collectors.toMap(EvenementDAtelierEntity::id, Function.identity()));
    suivi
      .journal()
      .evenements()
      .forEach(evenement -> rapproche(connus, evenement));
  }

  SuiviDAtelier toDomain() {
    SuiviDAtelier suivi = SuiviDAtelier.builder()
      .id(new SuiviDAtelierId(id))
      .element(new ElementEngage(new ElementEngageId(elementId), new NomDElement(elementNom), elementType))
      .engagement(new Engagement(new Auteur(engagementAuteur), engagementDate))
      .journal(new JournalDAtelier(journal.stream().map(EvenementDAtelierEntity::toDomain).toList()));

    return cloture().map(suivi::cloture).orElse(suivi);
  }

  private void rapproche(Map<UUID, EvenementDAtelierEntity> connus, EvenementDAtelier evenement) {
    EvenementDAtelierEntity connu = connus.get(evenement.id().uuid());

    if (connu == null) {
      journal.add(EvenementDAtelierEntity.from(this, evenement));
    } else {
      connu.reporteLAnnulation(evenement);
    }
  }

  private void reporteLaCloture(Optional<Cloture> cloture) {
    clotureAuteur = cloture.map(fin -> fin.auteur().value()).orElse(null);
    clotureDateDeSurvenue = cloture.map(Cloture::dateDeSurvenue).orElse(null);
    clotureDateDEnregistrement = cloture.map(fin -> fin.horodatage().dateDEnregistrement()).orElse(null);
  }

  private Optional<Cloture> cloture() {
    return Optional.ofNullable(clotureDateDeSurvenue).map(survenue ->
      new Cloture(new Auteur(clotureAuteur), new Horodatage(survenue, clotureDateDEnregistrement))
    );
  }
}
