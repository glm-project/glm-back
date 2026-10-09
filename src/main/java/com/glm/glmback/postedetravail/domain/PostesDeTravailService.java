package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public final class PostesDeTravailService {

  private final PosteDeTravailRepository repository;
  private final PostesEnUsage usages;
  private final PostesPointes pointages;
  private final NaturesDeclarees natures;

  public PostesDeTravailService(
    PosteDeTravailRepository repository,
    PostesEnUsage usages,
    PostesPointes pointages,
    NaturesDeclarees natures
  ) {
    this.repository = repository;
    this.usages = usages;
    this.pointages = pointages;
    this.natures = natures;
  }

  public PosteDeTravail create(PosteDeTravailACreer aCreer) {
    PosteDeTravailId id = PosteDeTravailId.newId();
    verifierLibelleLibre(id, aCreer.libelle());

    return repository.create(
      PosteDeTravail.builder()
        .id(id)
        .libelle(aCreer.libelle())
        .nature(nature(aCreer.nature()))
        .coutHoraire(aCreer.coutHoraire().map(CoutHoraire::value).orElse(null))
    );
  }

  public PosteDeTravail get(PosteDeTravailId id) {
    return repository.get(id).orElseThrow(() -> new PosteDeTravailIntrouvableException(id));
  }

  public Page<PosteDeTravail> list(Optional<NatureDeTravail> nature, Pageable pageable) {
    return repository.list(new PosteDeTravailCriteria(nature), pageable);
  }

  public PosteDeTravail update(PosteDeTravailAModifier aModifier) {
    PosteDeTravail existant = get(aModifier.id());
    verifierLibelleLibre(existant.id(), aModifier.libelle());

    return repository.update(existant.revise(aModifier.libelle(), nature(aModifier.nature()), aModifier.coutHoraire()));
  }

  /**
   * Supprimer un poste encore habilite laisserait des operateurs pointer sur du vide : le geste correct est de le
   * retirer de leurs habilitations d'abord.
   */
  /**
   * Deux raisons distinctes de refuser : une habilitation encore declaree, et du temps deja pointe. La seconde est
   * definitive — retirer l'habilitation ne rendra jamais le poste supprimable, sous peine de laisser des heures de
   * travail sans machine.
   */
  public void delete(PosteDeTravailId id) {
    if (usages.estHabilite(id)) {
      throw new PosteDeTravailUtiliseException(id);
    }
    if (pointages.aServiAPointer(id)) {
      throw new PosteDeTravailPointeException(id);
    }
    repository.delete(id);
  }

  /**
   * Chemin de transition : le libelle saisi en texte designe la nature qui porte la meme cle, declaree a la volee si
   * elle manque, pour que le front d'avant le referentiel continue de declarer ses postes. Retire avec glm-back#130.
   */
  @SuppressWarnings("removal")
  private NatureDuPoste nature(NatureDeTravail libelle) {
    return natures
      .parLibelle(libelle)
      .orElseGet(() -> {
        NatureDuPoste declaree = new NatureDuPoste(NatureDeTravailId.newId(), libelle);
        natures.declare(declaree);
        return declaree;
      });
  }

  /**
   * Un seul chemin sert la creation et la modification : a la creation l'identifiant vient d'etre tire, l'egalite ne
   * peut pas se produire ; a la modification, le poste qui conserve son propre libelle ne se heurte pas a lui-meme.
   */
  private void verifierLibelleLibre(PosteDeTravailId id, Libelle libelle) {
    repository
      .idPourLibelle(libelle)
      .filter(detenteur -> !detenteur.equals(id))
      .ifPresent(detenteur -> {
        throw new LibelleDejaUtiliseException(libelle);
      });
  }
}
