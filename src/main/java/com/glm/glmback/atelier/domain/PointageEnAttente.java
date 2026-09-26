package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Un geste du pupitre qu'on ne sait rattacher a rien (lot 8c de la strategie « bornes de fin de journee ») : conserve
 * tel quel, hors de tous les calculs, jusqu'a ce que le gestionnaire l'applique ou l'ecarte. L'operateur n'en est pas
 * informe : le pupitre a recu un succes.
 */
public record PointageEnAttente(
  PointageEnAttenteId id,
  UUID evenementDuPupitre,
  GesteEnAttente geste,
  MotifDeMiseEnAttente motif,
  Auteur auteur,
  Instant dateDeReception,
  Optional<TraitementDuPointage> traitement
) {
  public PointageEnAttente {
    Assert.notNull("id", id);
    Assert.notNull("evenement du pupitre", evenementDuPupitre);
    Assert.notNull("geste", geste);
    Assert.notNull("motif", motif);
    Assert.notNull("auteur", auteur);
    Assert.notNull("date de reception", dateDeReception);
    Assert.notNull("traitement", traitement);
  }

  public static PointageEnAttenteIdBuilder builder() {
    return id ->
      evenementDuPupitre ->
        geste ->
          motif ->
            auteur ->
              dateDeReception ->
                traitement -> new PointageEnAttente(id, evenementDuPupitre, geste, motif, auteur, dateDeReception, traitement);
  }

  /**
   * La date a laquelle l'appliquer : celle du pupitre, sauf si elle est future, ou absente — le geste datait alors
   * de sa reception.
   */
  public Instant dateDeSurvenue() {
    return geste
      .dateDeclaree()
      .filter(declaree -> !declaree.isAfter(dateDeReception))
      .orElse(dateDeReception);
  }

  public boolean estTraite() {
    return traitement.isPresent();
  }

  /**
   * Le meme pointage, sorti de la liste. Il ne se traite qu'une fois.
   */
  public PointageEnAttente traite(TraitementDuPointage nouveau) {
    if (estTraite()) {
      throw new PointageEnAttenteDejaTraiteException(id);
    }

    return new PointageEnAttente(id, evenementDuPupitre, geste, motif, auteur, dateDeReception, Optional.of(nouveau));
  }

  public interface PointageEnAttenteIdBuilder {
    PointageEnAttenteEvenementDuPupitreBuilder id(PointageEnAttenteId id);
  }

  public interface PointageEnAttenteEvenementDuPupitreBuilder {
    PointageEnAttenteGesteBuilder evenementDuPupitre(UUID evenementDuPupitre);
  }

  public interface PointageEnAttenteGesteBuilder {
    PointageEnAttenteMotifBuilder geste(GesteEnAttente geste);
  }

  public interface PointageEnAttenteMotifBuilder {
    PointageEnAttenteAuteurBuilder motif(MotifDeMiseEnAttente motif);
  }

  public interface PointageEnAttenteAuteurBuilder {
    PointageEnAttenteDateDeReceptionBuilder auteur(Auteur auteur);
  }

  public interface PointageEnAttenteDateDeReceptionBuilder {
    PointageEnAttenteTraitementBuilder dateDeReception(Instant dateDeReception);
  }

  public interface PointageEnAttenteTraitementBuilder {
    PointageEnAttente traitement(Optional<TraitementDuPointage> traitement);
  }
}
