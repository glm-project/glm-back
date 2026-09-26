package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.GesteRecu;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.PointagesEnAttenteService;
import com.glm.glmback.atelier.domain.Recueil;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Le chemin commun des gestes du pupitre : reserver l'identifiant du geste, rendre le rejeu tel quel, sinon ecrire —
 * ou mettre en attente ce qui ne se rattache a rien. Le pupitre recoit toujours un succes, sauf sur un element
 * cloture.
 */
final class GestesDuPupitre {

  private final PointagesEnAttenteService enAttente;
  private final IdentitesDEvenements identites;

  GestesDuPupitre(PointagesEnAttenteService enAttente, IdentitesDEvenements identites) {
    this.enAttente = enAttente;
    this.identites = identites;
  }

  <T> ResultatDEcriture<T> ecrit(
    GesteRecu recu,
    EmpreinteDEvenement empreinte,
    Function<UUID, T> relecture,
    Supplier<ResultatDEcriture<T>> ecriture
  ) {
    Recueil<ResultatDEcriture<T>> recueil = enAttente.recueille(recu, () -> {
      ReservationDEvenement reservation = identites.reserve(recu.evenement(), empreinte);
      if (reservation.estUnRejeu()) {
        return rejoue(reservation.agregat().orElseThrow(), relecture);
      }
      return ecriture.get();
    });

    return switch (recueil) {
      case Recueil.Enregistre<ResultatDEcriture<T>>(ResultatDEcriture<T> resultat) -> resultat;
      case Recueil.MisEnAttente<ResultatDEcriture<T>>(var pointage) -> {
        if (pointage.motif() != MotifDeMiseEnAttente.IDENTIFIANT_REUTILISE) {
          identites.associe(recu.evenement(), new AgregatDEvenement(TypeDAgregatDEvenement.POINTAGE_EN_ATTENTE, pointage.id().uuid()));
        }
        yield ResultatDEcriture.enAttente(false);
      }
    };
  }

  private static <T> ResultatDEcriture<T> rejoue(AgregatDEvenement agregat, Function<UUID, T> relecture) {
    if (agregat.type() == TypeDAgregatDEvenement.POINTAGE_EN_ATTENTE) {
      return ResultatDEcriture.enAttente(true);
    }

    return new ResultatDEcriture<>(relecture.apply(agregat.id()), true);
  }
}
