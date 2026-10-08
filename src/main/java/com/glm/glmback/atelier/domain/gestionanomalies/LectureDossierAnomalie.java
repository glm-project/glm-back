package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Le dossier d'une fin automatique : l'activite echue que le pointage de l'adresse ouvre, et que rien n'a terminee.
 *
 * <p>
 * Une fin automatique regularisee, comme tout pointage qui n'ouvre pas d'activite echue, n'a pas de dossier.
 * </p>
 */
public record LectureDossierAnomalie(AdresseDossierAnomalie adresse, LectureDuSuivi lecture, IntervalleDActivite activite) {
  public LectureDossierAnomalie {
    Assert.notNull("adresse", adresse);
    Assert.notNull("lecture", lecture);
    Assert.notNull("activite", activite);
  }

  public static LectureDossierAnomalie de(AdresseDossierAnomalie adresse, LectureDuSuivi lecture) {
    return lecture
      .suivi()
      .activites()
      .stream()
      .filter(activite -> activite.ouvrant().id().equals(adresse.pointage()))
      .map(activite -> activite.a(lecture.evaluation()))
      .filter(IntervalleDActivite::finAutomatique)
      .findFirst()
      .map(activite -> new LectureDossierAnomalie(adresse, lecture, activite))
      .orElseThrow(() -> new FinAutomatiqueIntrouvableException(adresse));
  }

  /**
   * L'instant que la fin regularisee ne peut pas depasser : le plus tot du debut suivant sur la cle et de la cloture,
   * ou rien.
   */
  public Optional<Instant> borneDeFin() {
    return lecture.suivi().borneDeFin(activite.activite());
  }

  /** Les pointages de la cle de l'activite echue : meme operateur, meme poste, dans l'ordre du journal. */
  public List<EvenementDAtelier> pointages() {
    return lecture
      .suivi()
      .journal()
      .evenements()
      .stream()
      .filter(pointage -> pointage.cle().equals(activite.cle()))
      .toList();
  }
}
