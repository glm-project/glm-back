package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import java.util.Optional;

/**
 * La resolution des ressources d'un fait que le gestionnaire ou le pupitre ecrit au journal : l'operateur et le poste
 * doivent exister, l'operateur doit etre habilite sur le poste, et la nature de l'operation, le cout horaire et le taux
 * horaire sont recopies, <b>du poste</b> et de l'operateur, au moment de la saisie.
 *
 * <p>
 * L'habilitation est la seule regle dure de ce contexte : la nature, elle, ne bloque rien. La regle ne joue que
 * lorsqu'un poste est fourni, une entreprise sans parc machine n'ayant aucune habilitation a declarer. Elle vaut pour
 * toutes les ecritures qui placent l'operateur sur un poste : pointage, regularisation, ajout d'une activite, changement
 * de poste. Sans quoi le back-office deviendrait un contournement de la regle que le pupitre applique.
 * </p>
 */
public final class ResolutionDesRessources {

  private final OperateursConnus operateurs;
  private final PostesConnus postes;
  private final Habilitations habilitations;

  public ResolutionDesRessources(OperateursConnus operateurs, PostesConnus postes, Habilitations habilitations) {
    this.operateurs = operateurs;
    this.postes = postes;
    this.habilitations = habilitations;
  }

  /**
   * L'operateur et le poste d'un geste, une fois etabli qu'ils existent et que l'operateur est habilite sur le poste.
   */
  public Ressources ressources(OperateurId operateur, Optional<PosteDeTravailId> poste) {
    return new Ressources(operateurConnu(operateur), poste.map(id -> posteHabilite(operateur, id)));
  }

  public OperateurConnu operateurConnu(OperateurId operateur) {
    return operateurs.get(operateur).orElseThrow(() -> new OperateurDAtelierIntrouvableException(operateur));
  }

  /**
   * Le poste, une fois etabli qu'il existe et que l'operateur y est habilite.
   */
  public PosteConnu posteHabilite(OperateurId operateur, PosteDeTravailId poste) {
    PosteConnu connu = postes.get(poste).orElseThrow(() -> new PosteDAtelierIntrouvableException(poste));

    if (!habilitations.estHabilite(operateur, poste)) {
      throw new OperateurNonHabiliteException(operateur, poste);
    }

    return connu;
  }

  /**
   * Ce qu'un fait recopie du referentiel : la fiche de l'operateur et, s'il y en a un, le poste.
   */
  public record Ressources(OperateurConnu operateur, Optional<PosteConnu> poste) {
    /**
     * Le fait que ces ressources portent, avec leur nature, leur cout horaire et leur taux horaire figes.
     */
    public EvenementDAtelier evenement(
      EvenementDAtelierId id,
      TypeDEvenementDAtelier type,
      Optional<ActiviteId> cible,
      Optional<MaximumActivityDuration> dureeMax,
      Auteur auteur,
      OrigineDuPointage origine,
      Horodatage horodatage
    ) {
      return EvenementDAtelier.builder()
        .id(id)
        .type(type)
        .activite(type.ouvreUneActivite() ? Optional.of(ActiviteId.ouvertePar(id)) : Optional.empty())
        .activiteVisee(cible)
        .operateur(operateur.id())
        .poste(poste.map(PosteConnu::id))
        .nature(poste.map(PosteConnu::nature))
        .coutHoraire(poste.flatMap(PosteConnu::coutHoraire))
        .tauxHoraire(operateur.tauxHoraire())
        .dureeMax(dureeMax)
        .auteur(auteur)
        .origine(origine)
        .horodatage(horodatage);
    }
  }
}
