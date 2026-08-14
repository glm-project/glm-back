package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Le referentiel de l'atelier en memoire : operateurs, postes et habilitations.
 *
 * <p>
 * Les trois ports sont servis par un seul etat, parce qu'une habilitation ne se declare pas sans son operateur ni son
 * poste : trois doubles independants obligeraient chaque test a les tenir coherents entre eux.
 * </p>
 */
final class RessourcesDAtelierEnMemoire {

  private final Map<OperateurId, OperateurConnu> operateurs;
  private final Map<PosteDeTravailId, PosteConnu> postes;
  private final Set<Map.Entry<OperateurId, PosteDeTravailId>> habilitations;

  private RessourcesDAtelierEnMemoire(
    List<OperateurConnu> operateurs,
    List<PosteConnu> postes,
    Set<Map.Entry<OperateurId, PosteDeTravailId>> habilitations
  ) {
    this.operateurs = operateurs.stream().collect(Collectors.toMap(OperateurConnu::id, operateur -> operateur));
    this.postes = postes.stream().collect(Collectors.toMap(PosteConnu::id, poste -> poste));
    this.habilitations = habilitations;
  }

  /**
   * Dupont est habilite sur les deux fraiseuses, Martin sur aucune : de quoi eprouver le refus sans rien declarer de
   * plus dans le test.
   */
  static RessourcesDAtelierEnMemoire deLAtelier() {
    return new RessourcesDAtelierEnMemoire(
      List.of(OPERATEUR_CONNU_DUPONT, OPERATEUR_CONNU_MARTIN),
      List.of(POSTE_CONNU_FRAISEUSE_1, POSTE_CONNU_FRAISEUSE_2),
      Set.of(Map.entry(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1), Map.entry(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_2))
    );
  }

  OperateursConnus operateurs() {
    return new OperateursEnMemoire();
  }

  PostesConnus postes() {
    return new PostesEnMemoire();
  }

  Habilitations habilitations() {
    return new HabilitationsEnMemoire();
  }

  private final class OperateursEnMemoire implements OperateursConnus {

    @Override
    public boolean existe(OperateurId id) {
      return operateurs.containsKey(id);
    }

    @Override
    public Optional<OperateurConnu> get(OperateurId id) {
      return Optional.ofNullable(operateurs.get(id));
    }

    @Override
    public List<OperateurConnu> parIds(Set<OperateurId> ids) {
      return ids.stream().map(operateurs::get).filter(Objects::nonNull).toList();
    }
  }

  private final class PostesEnMemoire implements PostesConnus {

    @Override
    public Optional<PosteConnu> get(PosteDeTravailId id) {
      return Optional.ofNullable(postes.get(id));
    }

    @Override
    public List<PosteConnu> parIds(Set<PosteDeTravailId> ids) {
      return ids.stream().map(postes::get).filter(Objects::nonNull).toList();
    }
  }

  private final class HabilitationsEnMemoire implements Habilitations {

    @Override
    public boolean estHabilite(OperateurId operateur, PosteDeTravailId poste) {
      return habilitations.contains(Map.entry(operateur, poste));
    }
  }
}
