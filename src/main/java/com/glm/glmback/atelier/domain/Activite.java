package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Une activite telle que la lecture du journal la donne : le pointage ouvrant qui la porte, et sa fin reelle quand un
 * fait l'a terminee.
 *
 * <p>
 * Son identite est celle de son pointage ouvrant d'origine ; l'ouvrant, lui, est le fait qui la porte. Son operateur, sa
 * categorie et son debut sont ceux de cet ouvrant, qui lui donne aussi sa duree maximale et donc son echeance.
 * </p>
 *
 * <p>
 * Son poste, sa nature, son cout horaire et son taux horaire sont ceux de son <i>porteur</i> : l'ouvrant, tant que
 * personne n'a corrige son poste, puis le dernier changement de poste que le gestionnaire a porte au journal. La
 * correction ne modifie aucun fait : elle change seulement celui qui parle pour l'activite.
 * </p>
 *
 * <p>
 * La fin est reelle ou absente : elle vient d'une fin pointee, d'une fin regularisee par le gestionnaire, ou de la
 * cloture du suivi. Elle ne depend jamais de l'instant ou on lit. Seule la lecture a un instant d'evaluation,
 * {@link #a(Instant)}, decide si une activite sans fin reelle est encore en cours ou deja terminee automatiquement a
 * son echeance.
 * </p>
 */
public record Activite(
  EvenementDAtelier ouvrant,
  Optional<EvenementDAtelier> changementDePoste,
  Optional<Instant> fin,
  Optional<EvenementDAtelier> evenementDeFin
) {
  public Activite {
    Assert.notNull("ouvrant", ouvrant);
    Assert.notNull("changement de poste", changementDePoste);
    Assert.notNull("fin", fin);
    Assert.notNull("evenement de fin", evenementDeFin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(ouvrant.dateDeSurvenue()));
  }

  public Activite(EvenementDAtelier ouvrant, Optional<Instant> fin) {
    this(ouvrant, Optional.empty(), fin, Optional.empty());
  }

  static Activite ouvertePar(EvenementDAtelier ouvrant) {
    return new Activite(ouvrant, Optional.empty());
  }

  /**
   * Terminee a cette heure sans qu'aucun fait la ferme : la cloture du suivi.
   */
  Activite termineeA(Instant date) {
    return new Activite(ouvrant, changementDePoste, Optional.of(date), Optional.empty());
  }

  /**
   * Terminee par ce fait, fin pointee ou fin regularisee, a son heure.
   */
  Activite termineePar(EvenementDAtelier fin) {
    return new Activite(ouvrant, changementDePoste, Optional.of(fin.dateDeSurvenue()), Optional.of(fin));
  }

  /**
   * Comptee desormais sur le poste de ce changement, qui devient son porteur.
   */
  Activite surLePosteDe(EvenementDAtelier changement) {
    return new Activite(ouvrant, Optional.of(changement), fin, evenementDeFin);
  }

  /**
   * Le fait qui porte le poste, la nature et les tarifs de l'activite : son dernier changement de poste, sinon son
   * ouvrant.
   */
  public EvenementDAtelier porteur() {
    return changementDePoste.orElse(ouvrant);
  }

  /**
   * Les gestes qui la bornent : son ouverture et, quand un fait la termine, sa fin. Ce sont les faits que la regle de
   * reception compte sur la cle de l'activite, ni la cloture ni un changement de poste.
   */
  public Stream<EvenementDAtelier> gestes() {
    return Stream.concat(Stream.of(ouvrant), evenementDeFin.stream());
  }

  public ActiviteId id() {
    return ouvrant.activite().orElseThrow();
  }

  public CleDActivite cle() {
    return new CleDActivite(ouvrant.operateur(), porteur().poste());
  }

  public CategorieDActivite categorie() {
    return ouvrant.type().categorie().orElseThrow();
  }

  public Instant debut() {
    return ouvrant.dateDeSurvenue();
  }

  /**
   * Le debut plus la duree maximale que l'ouvrant a portee, c'est-a-dire celle qui etait en vigueur quand l'activite a
   * commence, quoi que le gestionnaire ait fixe depuis.
   */
  public Echeance echeance() {
    return Echeance.apres(debut(), ouvrant.dureeMax().orElseThrow());
  }

  /**
   * L'activite telle qu'elle se lit a l'instant d'evaluation : terminee a sa fin reelle si un fait l'a terminee ;
   * sinon terminee automatiquement a son echeance, avec une anomalie, des que l'echeance est atteinte ; sinon en
   * cours. C'est ici que le domaine applique l'instant de
   * lecture. La regle d'echeance a trois lecteurs : le domaine ({@code Activite.a}), la supervision
   * ({@code ActiviteDeSupervision.a}) et le SQL de la liste des fins automatiques, qui ne peut pas appeler
   * {@link Echeance} et en recopie la comparaison. Leur parite est tenue par l'execution :
   * {@code ListeDesFinsAutomatiquesDAtelierIT} confronte ce SQL a {@code Activite.a}, a tout instant d'evaluation.
   */
  public IntervalleDActivite a(Instant evaluation) {
    if (fin.isEmpty() && echeance().estAtteinteA(evaluation)) {
      return intervalle(Optional.of(echeance().value()), true);
    }

    return intervalle(fin, false);
  }

  /**
   * Vrai si aucun fait n'a termine l'activite et que son echeance n'est pas encore atteinte a l'instant d'evaluation.
   */
  public boolean estEnCoursA(Instant evaluation) {
    return fin.isEmpty() && !echeance().estAtteinteA(evaluation);
  }

  private IntervalleDActivite intervalle(Optional<Instant> bornee, boolean finAutomatique) {
    return IntervalleDActivite.builder()
      .evenement(ouvrant.id())
      .activite(id())
      .operateur(ouvrant.operateur())
      .poste(porteur().poste())
      .nature(porteur().nature())
      .categorie(categorie())
      .debut(debut())
      .fin(bornee)
      .finAutomatique(finAutomatique);
  }
}
