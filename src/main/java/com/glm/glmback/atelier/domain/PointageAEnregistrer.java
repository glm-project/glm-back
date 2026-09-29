package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un pointage d'operateur : sa date de survenue vient du pupitre quand il la fournit, sinon le service prend l'instant
 * present.
 *
 * <p>
 * Le poste de travail est facultatif : un operateur mono-poste, ou une entreprise sans parc machine, garde un seul
 * clic. La nature de l'operation n'est pas fournie par l'appelant, le service la recopie du poste.
 * </p>
 *
 * <p>
 * Son intention dit s'il ouvre une activite, remplace celle qu'il vise, ou la termine : le type seul ne le dit pas. La
 * transition et la fin designent l'activite visee par l'identite de son pointage ouvrant.
 * </p>
 *
 * <p>
 * L'operateur et l'auteur sont deux choses differentes : sur un pupitre partage, celui qui saisit n'est pas
 * necessairement celui dont on compte le temps. L'auteur vient toujours du jeton, jamais du corps de la requete.
 * </p>
 */
public record PointageAEnregistrer(
  SuiviDAtelierId suivi,
  TypeDEvenementDAtelier type,
  IntentionDePointage intention,
  Optional<ActiviteId> activiteVisee,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  Auteur auteur,
  Optional<Instant> dateDeSurvenue,
  EvenementDAtelierId evenement
) {
  public PointageAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("type", type);
    Assert.notNull("intention", intention);
    Assert.notNull("activite visee", activiteVisee);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
    Assert.notNull("id de l'evenement", evenement);
  }

  public static PointageAEnregistrerSuiviBuilder builder() {
    return suivi ->
      type ->
        intention ->
          activiteVisee ->
            operateur ->
              poste ->
                auteur ->
                  new PointageAEnregistrer(
                    suivi,
                    type,
                    intention,
                    activiteVisee,
                    operateur,
                    poste,
                    auteur,
                    Optional.empty(),
                    EvenementDAtelierId.newId()
                  );
  }

  public static PointageDuPupitreSuiviBuilder pupitreBuilder() {
    return suivi ->
      type ->
        intention ->
          activiteVisee ->
            operateur ->
              poste ->
                auteur ->
                  dateDeSurvenue ->
                    evenement ->
                      new PointageAEnregistrer(suivi, type, intention, activiteVisee, operateur, poste, auteur, dateDeSurvenue, evenement);
  }

  public interface PointageAEnregistrerSuiviBuilder {
    PointageAEnregistrerTypeBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface PointageAEnregistrerTypeBuilder {
    PointageAEnregistrerIntentionBuilder type(TypeDEvenementDAtelier type);
  }

  public interface PointageAEnregistrerIntentionBuilder {
    PointageAEnregistrerActiviteViseeBuilder intention(IntentionDePointage intention);
  }

  public interface PointageAEnregistrerActiviteViseeBuilder {
    PointageAEnregistrerOperateurBuilder activiteVisee(Optional<ActiviteId> activiteVisee);
  }

  public interface PointageAEnregistrerOperateurBuilder {
    PointageAEnregistrerPosteBuilder operateur(OperateurId operateur);
  }

  public interface PointageAEnregistrerPosteBuilder {
    PointageAEnregistrerAuteurBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface PointageAEnregistrerAuteurBuilder {
    PointageAEnregistrer auteur(Auteur auteur);
  }

  public interface PointageDuPupitreSuiviBuilder {
    PointageDuPupitreTypeBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface PointageDuPupitreTypeBuilder {
    PointageDuPupitreIntentionBuilder type(TypeDEvenementDAtelier type);
  }

  public interface PointageDuPupitreIntentionBuilder {
    PointageDuPupitreActiviteViseeBuilder intention(IntentionDePointage intention);
  }

  public interface PointageDuPupitreActiviteViseeBuilder {
    PointageDuPupitreOperateurBuilder activiteVisee(Optional<ActiviteId> activiteVisee);
  }

  public interface PointageDuPupitreOperateurBuilder {
    PointageDuPupitrePosteBuilder operateur(OperateurId operateur);
  }

  public interface PointageDuPupitrePosteBuilder {
    PointageDuPupitreAuteurBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface PointageDuPupitreAuteurBuilder {
    PointageDuPupitreDateDeSurvenueBuilder auteur(Auteur auteur);
  }

  public interface PointageDuPupitreDateDeSurvenueBuilder {
    PointageDuPupitreEvenementBuilder dateDeSurvenue(Optional<Instant> dateDeSurvenue);
  }

  public interface PointageDuPupitreEvenementBuilder {
    PointageAEnregistrer evenement(EvenementDAtelierId evenement);
  }
}
