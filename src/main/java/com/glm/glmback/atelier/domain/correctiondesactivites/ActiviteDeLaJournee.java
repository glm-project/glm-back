package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Une activite qui touche le jour lu, telle que la journee a corriger la montre : son identite (celle de son pointage
 * ouvrant), son suivi et son element, son poste courant, sa categorie, ses bornes entieres et son etat.
 *
 * <p>
 * Sa duree n'est comptee que si elle est terminee, reelle ou automatique : {@code dureeSurLeJour} est la portion comptee
 * le jour lu, {@code autresJours} les portions comptees ailleurs, coupees aux minuits de l'entreprise. Une activite en
 * cours n'a ni fin, ni duree, ni autre jour. La fin d'une activite terminee automatiquement est son echeance.
 * </p>
 */
public record ActiviteDeLaJournee(
  ActiviteId id,
  SuiviDAtelierId suivi,
  ElementEngage element,
  Optional<PosteConnu> poste,
  CategorieDActivite categorie,
  Instant debut,
  Optional<Instant> fin,
  EtatDActiviteDeLaJournee etat,
  Optional<Duration> dureeSurLeJour,
  List<PortionDeJour> autresJours
) {
  public ActiviteDeLaJournee {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("suivi", suivi);
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("categorie", categorie);
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    Assert.notNull("etat", etat);
    Assert.notNull("duree sur le jour", dureeSurLeJour);
    Assert.notNull("autres jours", autresJours);

    autresJours = List.copyOf(autresJours);
  }

  public static ActiviteDeLaJourneeIdBuilder builder() {
    return id ->
      suivi ->
        element ->
          poste ->
            categorie ->
              debut ->
                fin ->
                  etat ->
                    dureeSurLeJour ->
                      autresJours ->
                        new ActiviteDeLaJournee(id, suivi, element, poste, categorie, debut, fin, etat, dureeSurLeJour, autresJours);
  }

  public interface ActiviteDeLaJourneeIdBuilder {
    ActiviteDeLaJourneeSuiviBuilder id(ActiviteId id);
  }

  public interface ActiviteDeLaJourneeSuiviBuilder {
    ActiviteDeLaJourneeElementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface ActiviteDeLaJourneeElementBuilder {
    ActiviteDeLaJourneePosteBuilder element(ElementEngage element);
  }

  public interface ActiviteDeLaJourneePosteBuilder {
    ActiviteDeLaJourneeCategorieBuilder poste(Optional<PosteConnu> poste);
  }

  public interface ActiviteDeLaJourneeCategorieBuilder {
    ActiviteDeLaJourneeDebutBuilder categorie(CategorieDActivite categorie);
  }

  public interface ActiviteDeLaJourneeDebutBuilder {
    ActiviteDeLaJourneeFinBuilder debut(Instant debut);
  }

  public interface ActiviteDeLaJourneeFinBuilder {
    ActiviteDeLaJourneeEtatBuilder fin(Optional<Instant> fin);
  }

  public interface ActiviteDeLaJourneeEtatBuilder {
    ActiviteDeLaJourneeDureeSurLeJourBuilder etat(EtatDActiviteDeLaJournee etat);
  }

  public interface ActiviteDeLaJourneeDureeSurLeJourBuilder {
    ActiviteDeLaJourneeAutresJoursBuilder dureeSurLeJour(Optional<Duration> dureeSurLeJour);
  }

  public interface ActiviteDeLaJourneeAutresJoursBuilder {
    ActiviteDeLaJournee autresJours(List<PortionDeJour> autresJours);
  }
}
