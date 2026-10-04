package com.glm.glmback.atelier.gestionconflits.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;

public final class EmpreinteFixture {

  private EmpreinteFixture() {}

  public static RelecturesDUnActe relecturesDUnDebutRegularise() {
    var evenement = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_8H);
    var avant = suiviDAtelierEngage().enregistre(evenement);
    var reel = EvenementDAtelier.builder()
      .id(evenement.id())
      .type(evenement.type())
      .intention(evenement.intention())
      .activite(evenement.activite())
      .activiteVisee(evenement.activiteVisee())
      .operateur(evenement.operateur())
      .poste(evenement.poste())
      .nature(evenement.nature())
      .coutHoraire(evenement.coutHoraire())
      .tauxHoraire(evenement.tauxHoraire())
      .auteur(AUTEUR_MARTIN)
      .origine(evenement.origine())
      .remplace(evenement.remplace())
      .horodatage(new Horodatage(evenement.dateDeSurvenue(), evenement.dateDEnregistrement().plusSeconds(3600)));
    var apres = SuiviDAtelier.relectureBuilder(new RevisionDuSuivi(27))
      .id(avant.id())
      .element(avant.element())
      .engagement(avant.engagement())
      .journal(suiviDAtelierEngage().journal().enregistre(reel));
    return new RelecturesDUnActe(avant, apres);
  }

  public record RelecturesDUnActe(SuiviDAtelier previsionnel, SuiviDAtelier reel) {}
}
