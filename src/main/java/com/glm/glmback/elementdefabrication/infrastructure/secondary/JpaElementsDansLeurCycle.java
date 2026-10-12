package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.CycleDeVie;
import com.glm.glmback.elementdefabrication.domain.ElementDansSonCycle;
import com.glm.glmback.elementdefabrication.domain.ElementsDansLeurCycle;
import com.glm.glmback.elementdefabrication.domain.ElementsDansLeurCycleCriteria;
import com.glm.glmback.elementdefabrication.domain.EtatEnAtelier;
import com.glm.glmback.elementdefabrication.domain.FiltreDesElements;
import com.glm.glmback.elementdefabrication.domain.Recherche;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Lit les elements avec leur suivi d'atelier, par une jointure sur la table de l'atelier.
 *
 * <p>
 * Un element n'a qu'un suivi dans sa vie (contrainte unique de l'atelier) : la jointure externe rend au plus une ligne
 * par element. L'etat d'un suivi ouvert se juge ensuite sur la seule page lue, en deux requetes, avec la regle de
 * l'atelier : en cours si une activite sans fin reelle n'a pas atteint son echeance a l'instant d'evaluation, sinon
 * interrompu s'il porte un evenement, sinon en attente.
 * </p>
 */
@Repository
class JpaElementsDansLeurCycle implements ElementsDansLeurCycle {

  private final EntityManager entities;

  JpaElementsDansLeurCycle(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  public Page<ElementDansSonCycle> list(ElementsDansLeurCycleCriteria criteria, Pageable pageable) {
    CriteriaBuilder constructeur = entities.getCriteriaBuilder();
    CriteriaQuery<Tuple> requete = constructeur.createTupleQuery();
    Lecture lecture = Lecture.sur(requete, constructeur);
    requete.multiselect(lecture.element(), lecture.suivi()).where(lecture.correspondA(criteria)).orderBy(lecture.tri(criteria));

    List<Tuple> lignes = entities
      .createQuery(requete)
      .setFirstResult((int) pageable.offset())
      .setMaxResults(pageable.size())
      .getResultList();

    return Page.<ElementDansSonCycle>builder()
      .content(lus(lignes, criteria.evaluation()))
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(compte(criteria));
  }

  private long compte(ElementsDansLeurCycleCriteria criteria) {
    CriteriaBuilder constructeur = entities.getCriteriaBuilder();
    CriteriaQuery<Long> requete = constructeur.createQuery(Long.class);
    Lecture lecture = Lecture.sur(requete, constructeur);
    requete.select(constructeur.count(lecture.element())).where(lecture.correspondA(criteria));

    return entities.createQuery(requete).getSingleResult();
  }

  private List<ElementDansSonCycle> lus(List<Tuple> lignes, Instant evaluation) {
    List<UUID> ouverts = lignes
      .stream()
      .map(ligne -> ligne.get(1, SuiviDuProduitEntity.class))
      .filter(suivi -> suivi != null && !suivi.estCloture())
      .map(SuiviDuProduitEntity::id)
      .toList();
    Set<UUID> enCours = enCours(ouverts, evaluation);
    Set<UUID> pointes = pointes(ouverts);

    return lignes
      .stream()
      .map(ligne -> {
        SuiviDuProduitEntity suivi = ligne.get(1, SuiviDuProduitEntity.class);
        return new ElementDansSonCycle(
          ligne.get(0, ElementDeFabricationEntity.class).toDomain(),
          Optional.ofNullable(suivi).map(lu -> lu.passage(etat(lu.id(), enCours, pointes)))
        );
      })
      .toList();
  }

  private static EtatEnAtelier etat(UUID suivi, Set<UUID> enCours, Set<UUID> pointes) {
    if (enCours.contains(suivi)) {
      return EtatEnAtelier.EN_COURS;
    }
    if (pointes.contains(suivi)) {
      return EtatEnAtelier.INTERROMPU;
    }
    return EtatEnAtelier.EN_ATTENTE;
  }

  private Set<UUID> enCours(Collection<UUID> suivis, Instant evaluation) {
    if (suivis.isEmpty()) {
      return Set.of();
    }
    return entities
      .createQuery(
        "SELECT activite.suiviId FROM ActiviteDuProduitEntity activite WHERE activite.suiviId IN :suivis AND activite.fin IS NULL AND activite.echeance > :evaluation",
        UUID.class
      )
      .setParameter("suivis", suivis)
      .setParameter("evaluation", evaluation)
      .getResultStream()
      .collect(Collectors.toSet());
  }

  private Set<UUID> pointes(Collection<UUID> suivis) {
    if (suivis.isEmpty()) {
      return Set.of();
    }
    return entities
      .createQuery("SELECT evenement.suiviId FROM EvenementDuProduitEntity evenement WHERE evenement.suiviId IN :suivis", UUID.class)
      .setParameter("suivis", suivis)
      .getResultStream()
      .collect(Collectors.toSet());
  }

  /**
   * La meme jointure et les memes regles pour la page et pour son total.
   */
  private record Lecture(
    Root<ElementDeFabricationEntity> element,
    Join<ElementDeFabricationEntity, SuiviDuProduitEntity> suivi,
    CriteriaBuilder constructeur
  ) {
    static Lecture sur(AbstractQuery<?> requete, CriteriaBuilder constructeur) {
      Root<ElementDeFabricationEntity> element = requete.from(ElementDeFabricationEntity.class);
      Join<ElementDeFabricationEntity, SuiviDuProduitEntity> suivi = element.join(SuiviDuProduitEntity.class, JoinType.LEFT);
      suivi.on(constructeur.equal(suivi.get("elementId"), element.get("id")));
      return new Lecture(element, suivi, constructeur);
    }

    Predicate[] correspondA(ElementsDansLeurCycleCriteria criteria) {
      List<Predicate> predicats = new ArrayList<>();
      FiltreDesElements filtre = criteria.filtre();
      filtre
        .creation()
        .ifPresent(periode -> predicats.add(constructeur.between(element.get("dateDeCreation"), periode.debut(), periode.fin())));
      filtre.categorie().ifPresent(categorie -> predicats.add(constructeur.equal(element.get("categorie"), categorie.value())));
      filtre.recherche().ifPresent(recherche -> predicats.add(trouve(recherche)));
      criteria.cycle().ifPresent(cycle -> predicats.add(dans(cycle)));
      return predicats.toArray(Predicate[]::new);
    }

    /**
     * La regle de {@link Recherche#trouveDans} : le texte, pris a la lettre, dans le nom, la reference ou la
     * description, sans tenir compte de la casse.
     */
    private Predicate trouve(Recherche recherche) {
      String motif = "%" + recherche.value().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
      return constructeur.or(
        constructeur.like(constructeur.lower(element.get("nom")), motif, '\\'),
        constructeur.like(constructeur.lower(element.get("reference")), motif, '\\'),
        constructeur.like(constructeur.lower(element.get("description")), motif, '\\')
      );
    }

    private Predicate dans(CycleDeVie cycle) {
      return switch (cycle) {
        case EN_PREPARATION -> constructeur.isNull(suivi.get("id"));
        case EN_ATELIER -> constructeur.and(
          constructeur.isNotNull(suivi.get("id")),
          constructeur.isNull(suivi.get("clotureDateDeSurvenue"))
        );
        case CLOTURE -> constructeur.isNotNull(suivi.get("clotureDateDeSurvenue"));
      };
    }

    /**
     * Chaque cycle se lit de son evenement le plus recent : creation, mise a l'atelier ou cloture ; sans cycle, la
     * creation. L'identite de l'element departage.
     */
    List<Order> tri(ElementsDansLeurCycleCriteria criteria) {
      Expression<?> date = criteria
        .cycle()
        .<Expression<?>>map(cycle ->
          switch (cycle) {
            case EN_PREPARATION -> element.get("dateDeCreation");
            case EN_ATELIER -> suivi.get("engagementDate");
            case CLOTURE -> suivi.get("clotureDateDeSurvenue");
          }
        )
        .orElseGet(() -> element.get("dateDeCreation"));
      return List.of(constructeur.desc(date), constructeur.asc(element.get("id")));
    }
  }
}
