package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailCriteria;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.Operateur;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaJourneeDeTravailRepository implements JourneeDeTravailRepository {

  /**
   * Le tri place les journees sans debut en dernier, comme le fait le double en memoire en repliant un debut absent
   * sur {@code Instant.MIN}. Sans cette precision, PostgreSQL les remonterait en tete d'un tri descendant : une
   * journee dont tous les evenements ont ete annules ouvrirait la liste.
   */
  private static final Sort PAR_DEBUT_DESCENDANT = Sort.by(Sort.Order.desc("debut").nullsLast(), Sort.Order.asc("id"));

  private final SpringDataJourneeDeTravailRepository journees;

  JpaJourneeDeTravailRepository(SpringDataJourneeDeTravailRepository journees) {
    this.journees = journees;
  }

  @Override
  public JourneeDeTravail create(JourneeDeTravail journee) {
    if (journees.existsById(journee.id().uuid())) {
      throw new JourneeDeTravailDejaOuverteException(journee.operateur());
    }
    journees.save(JourneeDeTravailEntity.from(journee));

    return journee;
  }

  @Override
  public JourneeDeTravail update(JourneeDeTravail journee) {
    JourneeDeTravailEntity entity = journees
      .findForUpdateById(journee.id().uuid())
      .orElseThrow(() -> new JourneeDeTravailIntrouvableException(journee.id()));

    if (entity.contientDesEvenementsAbsentsDe(journee)) {
      throw new SaisieConcurrenteException(journee.id());
    }
    entity.reconcilie(journee);

    return journee;
  }

  @Override
  public Optional<JourneeDeTravail> get(JourneeDeTravailId id) {
    return journees.findById(id.uuid()).map(JourneeDeTravailEntity::toDomain);
  }

  @Override
  public Optional<JourneeDeTravail> getEnCoursPour(Operateur operateur) {
    return journees
      .findFirstByOperateurAndEtatNotOrderByDebutDescIdAsc(operateur.value(), EtatDePresence.ABSENT)
      .map(JourneeDeTravailEntity::toDomain);
  }

  @Override
  public Optional<JourneeDeTravail> journeeContenant(Operateur operateur, Instant instant) {
    Optional<JourneeDeTravailEntity> trouvee = journees.findBy(contient(operateur, instant), requete ->
      requete.sortBy(PAR_DEBUT_DESCENDANT).first()
    );

    return trouvee.map(JourneeDeTravailEntity::toDomain);
  }

  @Override
  public Page<JourneeDeTravail> list(JourneeDeTravailCriteria criteria, Pageable pageable) {
    var page = journees.findAll(correspondA(criteria), PageRequest.of(pageable.page(), pageable.size(), PAR_DEBUT_DESCENDANT));

    return Page.<JourneeDeTravail>builder()
      .content(page.getContent().stream().map(JourneeDeTravailEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }

  /**
   * Traduit {@link JourneeDeTravail#contient(Instant)} : l'instant tombe entre l'arrivee et le depart, une journee
   * encore ouverte n'ayant pas de borne haute. Un debut absent ne contient rien, ce dont la comparaison se charge
   * seule.
   */
  private static Specification<JourneeDeTravailEntity> contient(Operateur operateur, Instant instant) {
    return (racine, requete, constructeur) ->
      constructeur.and(
        constructeur.equal(racine.get("operateur"), operateur.value()),
        constructeur.lessThanOrEqualTo(racine.get("debut"), instant),
        constructeur.or(constructeur.isNull(racine.get("fin")), constructeur.greaterThanOrEqualTo(racine.get("fin"), instant))
      );
  }

  /**
   * Traduit en SQL les regles que {@link JourneeDeTravailCriteria#matches} porte pour le domaine, le debut etant
   * projete plutot que stocke. Un test de parite confronte les deux expressions.
   */
  private static Specification<JourneeDeTravailEntity> correspondA(JourneeDeTravailCriteria criteria) {
    return (racine, requete, constructeur) -> {
      List<Predicate> predicats = new ArrayList<>();
      criteria.operateur().ifPresent(operateur -> predicats.add(constructeur.equal(racine.get("operateur"), operateur.value())));
      criteria.periode().ifPresent(periode -> predicats.add(constructeur.between(racine.get("debut"), periode.debut(), periode.fin())));

      return constructeur.and(predicats.toArray(Predicate[]::new));
    };
  }
}
