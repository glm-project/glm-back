package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * La fabrique des pointages en attente : c'est ici que se decide qu'un geste du pupitre ne peut etre rattache a rien.
 *
 * <p>
 * Un geste n'est mis en attente que pour les refus qui disent qu'il ne se rattache a rien — operateur, poste ou
 * element inconnu, geste hors sequence, identifiant reutilise. Tout autre refus remonte : un element cloture reste le
 * seul refus montre a l'operateur, une saisie concurrente est rejouee. Ces refus sont tous leves avant la moindre
 * ecriture : mettre en attente ne laisse jamais d'agregat a moitie ecrit.
 * </p>
 */
public final class PointagesEnAttenteService {

  private final PointagesEnAttente pointages;
  private final Clock clock;

  public PointagesEnAttenteService(PointagesEnAttente pointages, Clock clock) {
    this.pointages = pointages;
    this.clock = clock;
  }

  public <T> Recueil<T> recueille(GesteRecu recu, Supplier<T> enregistrement) {
    try {
      return new Recueil.Enregistre<>(enregistrement.get());
    } catch (OperateurDAtelierIntrouvableException e) {
      return metEnAttente(recu, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    } catch (PosteDAtelierIntrouvableException e) {
      return metEnAttente(recu, MotifDeMiseEnAttente.POSTE_INCONNU);
    } catch (SuiviDAtelierIntrouvableException e) {
      return metEnAttente(recu, MotifDeMiseEnAttente.ELEMENT_INCONNU);
    } catch (TransitionDePresenceInterditeException | TransitionDAtelierInterditeException | AucuneJourneeDeTravailEnCoursException e) {
      return metEnAttente(recu, MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE);
    } catch (IdentifiantDEvenementReutiliseException e) {
      return this.<T>dejaEnAttente(recu).orElseGet(() -> metEnAttente(recu, MotifDeMiseEnAttente.IDENTIFIANT_REUTILISE));
    }
  }

  public Page<PointageEnAttente> list(Optional<OperateurId> operateur, Optional<MotifDeMiseEnAttente> motif, Pageable pageable) {
    return pointages.list(new CriteresDePointageEnAttente(operateur, motif), pageable);
  }

  /**
   * L'application est faite avant que le pointage ne sorte de la liste : si elle est refusee, il y reste.
   */
  public PointageEnAttente applique(PointageEnAttenteId id, Auteur auteur, Consumer<PointageEnAttente> application) {
    PointageEnAttente pointage = get(id);
    PointageEnAttente traite = pointage.traite(new Application(auteur, clock.now()));
    application.accept(pointage);

    return pointages.update(traite);
  }

  public PointageEnAttente ecarte(PointageEnAttenteId id, Auteur auteur, MotifDEcart motif) {
    return pointages.update(get(id).traite(new Ecart(auteur, clock.now(), motif)));
  }

  private PointageEnAttente get(PointageEnAttenteId id) {
    return pointages.get(id).orElseThrow(() -> new PointageEnAttenteIntrouvableException(id));
  }

  /**
   * Un identifiant reutilise n'est associe a aucun pointage en attente, puisqu'il appartient deja a un autre contenu :
   * c'est ici que se retrouve le rejeu du meme geste, tant qu'il n'a pas ete traite.
   */
  private <T> Optional<Recueil<T>> dejaEnAttente(GesteRecu recu) {
    return pointages
      .parEvenementDuPupitre(recu.evenement())
      .stream()
      .filter(pointage -> !pointage.estTraite() && pointage.geste().equals(recu.geste()))
      .findFirst()
      .map(pointage -> new Recueil.MisEnAttente<>(pointage));
  }

  private <T> Recueil<T> metEnAttente(GesteRecu recu, MotifDeMiseEnAttente motif) {
    return new Recueil.MisEnAttente<>(
      pointages.create(
        PointageEnAttente.builder()
          .id(PointageEnAttenteId.newId())
          .evenementDuPupitre(recu.evenement())
          .geste(recu.geste())
          .motif(motif)
          .auteur(recu.auteur())
          .dateDeReception(clock.now())
          .traitement(Optional.empty())
      )
    );
  }
}
