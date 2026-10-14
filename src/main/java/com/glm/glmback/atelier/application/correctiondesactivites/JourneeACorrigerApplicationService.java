package com.glm.glmback.atelier.application.correctiondesactivites;

import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.correctiondesactivites.ActivitesDeLOperateur;
import com.glm.glmback.atelier.domain.correctiondesactivites.FichesDElement;
import com.glm.glmback.atelier.domain.correctiondesactivites.FuseauHoraireDeLEntreprise;
import com.glm.glmback.atelier.domain.correctiondesactivites.JourneeACorriger;
import com.glm.glmback.atelier.domain.correctiondesactivites.JourneesACorrigerService;
import com.glm.glmback.atelier.domain.correctiondesactivites.SuivisPresents;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La lecture de la journee a corriger, reservee au gestionnaire. C'est ici que se lit l'horloge quand l'appelant ne fixe
 * pas d'instant d'evaluation : le domaine le recoit explicitement.
 */
@Service
public class JourneeACorrigerApplicationService {

  private final JourneesACorrigerService journees;
  private final Clock clock;

  public JourneeACorrigerApplicationService(
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    ActivitesDeLOperateur activites,
    SuivisPresents suivisPresents,
    FichesDElement fiches,
    FuseauHoraireDeLEntreprise fuseau,
    Clock clock
  ) {
    this.journees = JourneesACorrigerService.builder()
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .activites(activites)
      .suivisPresents(suivisPresents)
      .fiches(fiches)
      .fuseau(fuseau);
    this.clock = clock;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public JourneeACorriger lit(OperateurId operateur, LocalDate jour, Optional<Instant> evaluation) {
    return journees.lit(operateur, jour, evaluation.orElseGet(clock::now));
  }
}
