package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CoutHoraire;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.ResolutionDesRessources;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDurations;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * La fabrique de domaine des corrections que le gestionnaire apporte a la journee d'un operateur : ajouter une activite
 * oubliee, retirer une activite pointee a tort, changer son poste.
 *
 * <p>
 * Une correction n'efface rien et ne modifie aucun fait : elle ajoute des faits au journal du suivi, datés de l'heure
 * que la correction vise pour un ajout, du moment de la correction pour un retrait et un changement de poste. Elle ne
 * passe pas par la regle de reception, qui juge ensuite les pointages sur le journal tel qu'il est apres elle. Chaque
 * fait porte son auteur et son origine, une regularisation : c'est la trace.
 * </p>
 *
 * <p>
 * Un identifiant deja present dans la table des evenements, de ce suivi ou d'un autre, est un renvoi : il repond comme
 * un succes et n'ecrit rien, avant toute autre regle. Un suivi qui n'existe pas est la seule exception.
 * L'habilitation se verifie sur ce qui place l'operateur sur un poste, le poste d'un ajout et le nouveau poste d'un
 * changement ; elle ne se verifie jamais au retrait, pour qu'on puisse retirer une activite pointee sur un poste dont
 * l'operateur a perdu l'habilitation depuis. Les refus se rendent dans l'ordre ou chaque methode les documente.
 * </p>
 */
public final class CorrectionsDesActivitesService {

  private final SuiviDAtelierRepository repository;
  private final ResolutionDesRessources ressources;
  private final MaximumActivityDurations dureesMax;
  private final Clock clock;

  private CorrectionsDesActivitesService(
    SuiviDAtelierRepository repository,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    MaximumActivityDurations dureesMax,
    Clock clock
  ) {
    this.repository = repository;
    this.ressources = new ResolutionDesRessources(operateurs, postes, habilitations);
    this.dureesMax = dureesMax;
    this.clock = clock;
  }

  public static CorrectionsDesActivitesServiceRepositoryBuilder builder() {
    return repository ->
      operateurs ->
        postes ->
          habilitations ->
            dureesMax -> clock -> new CorrectionsDesActivitesService(repository, operateurs, postes, habilitations, dureesMax, clock);
  }

  /**
   * Ajoute une activite oubliee : une ouverture a l'heure du debut, une fin a l'heure de la fin, d'origine
   * regularisation. La nature, le cout et le taux horaires sont figes comme pour un pointage ; la duree maximale en
   * vigueur est copiee sur l'ouverture.
   *
   * <p>
   * Refus, dans l'ordre : suivi introuvable, operateur introuvable, poste introuvable, operateur non habilite (ici), puis
   * ceux du suivi (voir {@link SuiviDAtelier#ajoute}) : fin qui ne suit pas le debut, fin future, debut anterieur a
   * l'engagement, fin posterieure a la cloture, chevauchement d'une autre activite de la cle sur toute l'activite.
   * </p>
   */
  public CorrectionTraitee ajoute(AjoutDUneActivite commande) {
    SuiviDAtelier suivi = get(commande.suivi());
    if (repository.contientEvenement(commande.evenement())) {
      return new CorrectionTraitee(suivi, true);
    }

    Instant maintenant = clock.now();
    ResolutionDesRessources.Ressources saisies = ressources.ressources(commande.operateur(), commande.poste());
    PlageDAjout plage = commande.plage();
    EvenementDAtelier ouverture = saisies.evenement(
      commande.evenement(),
      commande.categorie().typeDOuverture(),
      Optional.empty(),
      Optional.of(dureesMax.current()),
      commande.auteur(),
      OrigineDuPointage.REGULARISATION,
      new Horodatage(plage.debut(), enregistreeA(plage.debut(), maintenant))
    );
    EvenementDAtelier fin = saisies.evenement(
      commande.evenementDeFin(),
      TypeDEvenementDAtelier.FIN,
      Optional.of(commande.activite()),
      Optional.empty(),
      commande.auteur(),
      OrigineDuPointage.REGULARISATION,
      new Horodatage(plage.fin(), enregistreeA(plage.fin(), maintenant))
    );

    return new CorrectionTraitee(repository.update(suivi.ajoute(ouverture, fin, maintenant)), false);
  }

  /**
   * Change le poste d'une activite terminee : un fait qui vise l'activite, date du moment de la correction, et qui
   * recopie la nature et le cout horaire du nouveau poste. Le taux horaire reste celui de l'activite.
   *
   * <p>
   * Refus, dans l'ordre : suivi introuvable, activite introuvable (ou retiree), activite en cours, poste introuvable,
   * operateur non habilite sur le nouveau poste, meme poste, chevauchement sur la nouvelle cle, changement posterieur
   * a la cloture.
   * </p>
   */
  public CorrectionTraitee changeLePoste(ChangementDePosteAEnregistrer commande) {
    SuiviDAtelier suivi = get(commande.suivi());
    if (repository.contientEvenement(commande.evenement())) {
      return new CorrectionTraitee(suivi, true);
    }

    Instant maintenant = clock.now();
    Activite activite = suivi.exigeUneActiviteCorrigeable(commande.activite(), maintenant);
    PosteConnu poste = ressources.posteHabilite(activite.cle().operateur(), commande.poste());
    if (activite.cle().poste().filter(poste.id()::equals).isPresent()) {
      throw new MemePosteException(commande.activite(), poste.id());
    }

    EvenementDAtelier changement = correction(
      commande.evenement(),
      TypeDEvenementDAtelier.CHANGEMENT_DE_POSTE,
      activite,
      Optional.of(poste.id()),
      Optional.of(poste.nature()),
      poste.coutHoraire(),
      commande.auteur(),
      maintenant
    );

    return new CorrectionTraitee(repository.update(suivi.changeLePoste(changement, maintenant)), false);
  }

  /**
   * Retire une activite terminee : un fait qui vise l'activite, date du moment de la correction. Aucune habilitation
   * n'est verifiee.
   *
   * <p>
   * Refus, dans l'ordre : suivi introuvable, activite introuvable (ou deja retiree), activite en cours, retrait
   * posterieur a la cloture.
   * </p>
   */
  public CorrectionTraitee retire(RetraitAEnregistrer commande) {
    SuiviDAtelier suivi = get(commande.suivi());
    if (repository.contientEvenement(commande.evenement())) {
      return new CorrectionTraitee(suivi, true);
    }

    Instant maintenant = clock.now();
    Activite activite = suivi.exigeUneActiviteCorrigeable(commande.activite(), maintenant);
    EvenementDAtelier porteur = activite.porteur();
    EvenementDAtelier retrait = correction(
      commande.evenement(),
      TypeDEvenementDAtelier.RETRAIT,
      activite,
      porteur.poste(),
      porteur.nature(),
      porteur.coutHoraire(),
      commande.auteur(),
      maintenant
    );

    return new CorrectionTraitee(repository.update(suivi.retire(retrait)), false);
  }

  /**
   * L'enregistrement d'un fait ajoute : maintenant. Un fait du futur n'a pas d'enregistrement possible avant lui, et le
   * suivi le refuse avant de l'ecrire : l'evenement n'est alors construit que pour lui etre presente.
   */
  private static Instant enregistreeA(Instant survenue, Instant maintenant) {
    return survenue.isAfter(maintenant) ? survenue : maintenant;
  }

  private SuiviDAtelier get(SuiviDAtelierId id) {
    return repository.get(id).orElseThrow(() -> new SuiviDAtelierIntrouvableException(id));
  }

  /**
   * Un fait du gestionnaire qui vise une activite sans en ouvrir : l'operateur et le taux horaire sont ceux de l'activite,
   * le poste, la nature et le cout horaire ceux qu'on lui donne. Il est date du moment de la correction.
   */
  private static EvenementDAtelier correction(
    EvenementDAtelierId id,
    TypeDEvenementDAtelier type,
    Activite activite,
    Optional<PosteDeTravailId> poste,
    Optional<NatureDOperation> nature,
    Optional<CoutHoraire> coutHoraire,
    Auteur auteur,
    Instant maintenant
  ) {
    return EvenementDAtelier.builder()
      .id(id)
      .type(type)
      .activite(Optional.empty())
      .activiteVisee(Optional.of(activite.id()))
      .operateur(activite.cle().operateur())
      .poste(poste)
      .nature(nature)
      .coutHoraire(coutHoraire)
      .tauxHoraire(activite.porteur().tauxHoraire())
      .dureeMax(Optional.empty())
      .auteur(auteur)
      .origine(OrigineDuPointage.REGULARISATION)
      .horodatage(Horodatage.saisiA(maintenant));
  }

  public interface CorrectionsDesActivitesServiceRepositoryBuilder {
    CorrectionsDesActivitesServiceOperateursBuilder repository(SuiviDAtelierRepository repository);
  }

  public interface CorrectionsDesActivitesServiceOperateursBuilder {
    CorrectionsDesActivitesServicePostesBuilder operateurs(OperateursConnus operateurs);
  }

  public interface CorrectionsDesActivitesServicePostesBuilder {
    CorrectionsDesActivitesServiceHabilitationsBuilder postes(PostesConnus postes);
  }

  public interface CorrectionsDesActivitesServiceHabilitationsBuilder {
    CorrectionsDesActivitesServiceDureeMaxBuilder habilitations(Habilitations habilitations);
  }

  public interface CorrectionsDesActivitesServiceDureeMaxBuilder {
    CorrectionsDesActivitesServiceClockBuilder dureeMax(MaximumActivityDurations dureesMax);
  }

  public interface CorrectionsDesActivitesServiceClockBuilder {
    CorrectionsDesActivitesService clock(Clock clock);
  }
}
