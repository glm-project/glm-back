package com.glm.glmback.atelier.domain.gestionconflits;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.time.Instant;
import java.util.Optional;

public final class ActeDeResolutionFixture {

  public static final String LE_10_MAI_2026_A_19H_AVEC_NEUF_DECIMALES = "2026-05-10T19:00:00.123456789+02:00";

  private ActeDeResolutionFixture() {}

  public static ActeDeResolution.Correction correctionDeLaFinAvecNeufDecimales() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    RegularisationAEnregistrer remplacement = RegularisationAEnregistrer.builder()
      .suivi(suiviDAtelierEngage().id())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(debut.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(Instant.parse(LE_10_MAI_2026_A_19H_AVEC_NEUF_DECIMALES));
    return new ActeDeResolution.Correction(
      new CorrectionAEnregistrer(finDe(debut).a(LE_10_MAI_2026_A_17H).id(), MOTIF_ERREUR_DE_SAISIE, remplacement),
      LE_10_MAI_2026_A_19H_AVEC_NEUF_DECIMALES
    );
  }
}
