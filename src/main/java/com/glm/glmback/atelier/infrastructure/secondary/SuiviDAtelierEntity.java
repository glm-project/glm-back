package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CategorieDElement;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La ligne d'un suivi d'atelier, son journal, et la projection de ses activites.
 *
 * <p>
 * Les activites sont une projection : elles s'ecrivent depuis le domaine a chaque enregistrement et ne sont jamais
 * relues par {@link #toDomain()}, qui rejoue toujours le journal. Le journal reste donc la seule source de verite, et
 * cette table n'est qu'un index qui rend le filtre de l'ecran d'atelier exprimable en SQL. Elle ne depend que du
 * journal, jamais de l'instant courant : elle est donc stable entre deux ecritures, et c'est la lecture qui juge
 * l'echeance.
 * </p>
 */
@Entity
@Table(name = "suivi_d_atelier")
class SuiviDAtelierEntity {

  @Id
  private UUID id;

  private long revision;

  private UUID elementId;

  private String elementNom;

  @Column(length = 30)
  private String elementCategorie;

  private String engagementAuteur;

  @Convert(converter = ExactInstantConverter.class)
  private Instant engagementDate;

  private String clotureAuteur;

  @Convert(converter = ExactInstantConverter.class)
  private Instant clotureDateDeSurvenue;

  @Column(name = "cloture_date_d_enregistrement")
  @Convert(converter = ExactInstantConverter.class)
  private Instant clotureDateDEnregistrement;

  @OneToMany(mappedBy = "suivi", cascade = CascadeType.ALL)
  @OrderBy("dateDeSurvenue, id")
  private List<EvenementDAtelierEntity> journal = new ArrayList<>();

  @OneToMany(mappedBy = "suivi", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ActiviteDAtelierEntity> activites = new ArrayList<>();

  protected SuiviDAtelierEntity() {
    // Constructeur requis par JPA.
  }

  private SuiviDAtelierEntity(SuiviDAtelier suivi) {
    id = suivi.id().uuid();
    revision = suivi.revision().value();
    elementId = suivi.element().id().uuid();
    elementNom = suivi.element().nom().value();
    elementCategorie = suivi.element().categorie().value();
    engagementAuteur = suivi.engagement().auteur().value();
    engagementDate = suivi.engagement().date();
    reconcilie(suivi);
  }

  static SuiviDAtelierEntity from(SuiviDAtelier suivi) {
    return new SuiviDAtelierEntity(suivi);
  }

  boolean porteUneAutreRevisionQue(SuiviDAtelier suivi) {
    return revision != suivi.revision().value();
  }

  void enregistre(SuiviDAtelier suivi) {
    if (!toDomain().equals(suivi)) {
      reconcilie(suivi);
      revision++;
    }
  }

  /**
   * Rapproche la ligne de l'agregat que le domaine vient de reconstruire.
   *
   * <p>
   * L'element et l'engagement ne changent jamais apres la creation, ils ne sont donc pas retouches. Le journal, lui,
   * se rapproche par identifiant : un evenement inconnu est insere, un evenement connu ne change jamais, et aucun
   * n'est jamais supprime. Le cout d'un pointage est donc d'une ligne, quelle que soit la longueur du journal.
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

    Map<UUID, EvenementDAtelierEntity> connus = journal
      .stream()
      .collect(Collectors.toMap(EvenementDAtelierEntity::id, Function.identity()));
    suivi
      .journal()
      .evenements()
      .forEach(evenement -> rapproche(connus, evenement));
    projette(suivi.activites());
  }

  SuiviDAtelier toDomain() {
    SuiviDAtelier suivi = SuiviDAtelier.relectureBuilder(new RevisionDuSuivi(revision))
      .id(new SuiviDAtelierId(id))
      .element(new ElementEngage(new ElementEngageId(elementId), new NomDElement(elementNom), new CategorieDElement(elementCategorie)))
      .engagement(new Engagement(new Auteur(engagementAuteur), engagementDate))
      .journal(new JournalDAtelier(journal.stream().map(EvenementDAtelierEntity::toDomain).toList()));

    return cloture().map(suivi::cloture).orElse(suivi);
  }

  /**
   * Reecrit la projection des activites, rapprochee elle aussi par identifiant : le journal ne perd jamais un evenement, donc
   * aucune activite ne disparait ; une activite connue recoit ses valeurs courantes, une nouvelle est inseree.
   */
  private void projette(List<Activite> lues) {
    Map<UUID, ActiviteDAtelierEntity> projetees = activites
      .stream()
      .collect(Collectors.toMap(ActiviteDAtelierEntity::id, Function.identity()));

    lues.forEach(activite -> {
      ActiviteDAtelierEntity projetee = projetees.get(activite.id().uuid());
      if (projetee == null) {
        activites.add(ActiviteDAtelierEntity.from(this, activite));
      } else {
        projetee.reporte(activite);
      }
    });
  }

  private void rapproche(Map<UUID, EvenementDAtelierEntity> connus, EvenementDAtelier evenement) {
    EvenementDAtelierEntity connu = connus.get(evenement.id().uuid());

    if (connu == null) {
      journal.add(EvenementDAtelierEntity.from(this, evenement));
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
