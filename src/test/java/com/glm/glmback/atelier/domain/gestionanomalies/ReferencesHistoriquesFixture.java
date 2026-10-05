package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.ConflitsFixture.*;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.util.Optional;
import java.util.UUID;

public final class ReferencesHistoriquesFixture {

  private ReferencesHistoriquesFixture() {}

  public static SuiviDAtelier suiviDu10Mai2026SansFicheDOperateur() {
    var id = EvenementDAtelierId.newId();
    var ouverture = EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(new OperateurId(UUID.randomUUID()))
      .poste(Optional.empty())
      .nature(Optional.empty())
      .coutHoraire(Optional.empty())
      .tauxHoraire(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .remplace(Optional.empty())
      .horodatage(Horodatage.saisiA(LE_10_MAI_2026_A_8H));
    return suiviOF2026000042EngageA(LE_10_MAI_2026_A_8H)
      .enregistre(ouverture)
      .enregistre(finDe(ouverture).a(LE_10_MAI_2026_A_17H))
      .annule(ouverture.id(), annulationParLeroy());
  }
}
