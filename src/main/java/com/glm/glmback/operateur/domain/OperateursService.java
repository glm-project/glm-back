package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OperateursService {

  private final OperateurRepository repository;
  private final PostesHabilitables postes;

  public OperateursService(OperateurRepository repository, PostesHabilitables postes) {
    this.repository = repository;
    this.postes = postes;
  }

  public ProfilDOperateur create(OperateurACreer aCreer) {
    OperateurId id = OperateurId.newId();
    verifierIdentiteLibre(id, aCreer.nom(), aCreer.prenom());
    verifierMatriculeLibre(id, aCreer.matricule());
    List<PosteHabilitable> habilitations = resoudre(aCreer.postes());

    Operateur cree = repository.create(
      Operateur.builder()
        .id(id)
        .nom(aCreer.nom())
        .prenom(aCreer.prenom())
        .matricule(aCreer.matricule().map(Matricule::value).orElse(null))
        .postes(aCreer.postes())
    );

    return new ProfilDOperateur(cree, habilitations);
  }

  public ProfilDOperateur get(OperateurId id) {
    Operateur operateur = charger(id);

    return new ProfilDOperateur(operateur, resoudre(operateur.postes()));
  }

  public ProfilDOperateur update(OperateurAModifier aModifier) {
    Operateur existant = charger(aModifier.id());
    verifierIdentiteLibre(existant.id(), aModifier.nom(), aModifier.prenom());
    verifierMatriculeLibre(existant.id(), aModifier.matricule());
    List<PosteHabilitable> habilitations = resoudre(aModifier.postes());

    Operateur revise = repository.update(existant.revise(aModifier.nom(), aModifier.prenom(), aModifier.matricule(), aModifier.postes()));

    return new ProfilDOperateur(revise, habilitations);
  }

  public void delete(OperateurId id) {
    repository.delete(id);
  }

  public Page<ProfilDOperateur> list(Optional<PosteHabilitableId> poste, Pageable pageable) {
    Page<Operateur> page = repository.list(new OperateurCriteria(poste), pageable);
    Map<PosteHabilitableId, PosteHabilitable> connus = indexer(page.content());

    return Page.<ProfilDOperateur>builder()
      .content(
        page
          .content()
          .stream()
          .map(operateur -> profil(operateur, connus))
          .toList()
      )
      .currentPage(page.currentPage())
      .pageSize(page.pageSize())
      .totalElementsCount(page.totalElementsCount());
  }

  private Operateur charger(OperateurId id) {
    return repository.get(id).orElseThrow(() -> new OperateurIntrouvableException(id));
  }

  /**
   * Resout les postes d'un operateur en une seule interrogation du contexte voisin, et refuse le premier identifiant
   * qu'il ne connait pas. Le tri rend ce refus deterministe.
   */
  private List<PosteHabilitable> resoudre(Set<PosteHabilitableId> ids) {
    Map<PosteHabilitableId, PosteHabilitable> connus = indexer(ids);

    return ids
      .stream()
      .sorted()
      .map(id -> Optional.ofNullable(connus.get(id)).orElseThrow(() -> new PosteHabilitableIntrouvableException(id)))
      .toList();
  }

  /**
   * Une page entiere se resout en une requete : sans cela, lister vingt operateurs de quatre postes en couterait
   * quatre-vingts.
   */
  private Map<PosteHabilitableId, PosteHabilitable> indexer(List<Operateur> operateurs) {
    return indexer(operateurs.stream().map(Operateur::postes).flatMap(Set::stream).collect(Collectors.toSet()));
  }

  private Map<PosteHabilitableId, PosteHabilitable> indexer(Set<PosteHabilitableId> ids) {
    return postes.parIds(ids).stream().collect(Collectors.toMap(PosteHabilitable::id, Function.identity()));
  }

  private ProfilDOperateur profil(Operateur operateur, Map<PosteHabilitableId, PosteHabilitable> connus) {
    return new ProfilDOperateur(operateur, operateur.postes().stream().map(connus::get).filter(Objects::nonNull).toList());
  }

  /**
   * Un seul chemin sert la creation et la modification : a la creation l'identifiant vient d'etre tire, l'egalite ne
   * peut pas se produire ; a la modification, l'operateur qui conserve sa propre identite ne se heurte pas a lui-meme.
   */
  private void verifierIdentiteLibre(OperateurId id, Nom nom, Prenom prenom) {
    repository
      .idPourIdentite(nom, prenom)
      .filter(detenteur -> !detenteur.equals(id))
      .ifPresent(detenteur -> {
        throw new IdentiteDejaUtiliseeException(nom, prenom);
      });
  }

  private void verifierMatriculeLibre(OperateurId id, Optional<Matricule> matricule) {
    matricule
      .flatMap(repository::idPourMatricule)
      .filter(detenteur -> !detenteur.equals(id))
      .ifPresent(detenteur -> {
        throw new MatriculeDejaUtiliseException(matricule.orElseThrow());
      });
  }
}
