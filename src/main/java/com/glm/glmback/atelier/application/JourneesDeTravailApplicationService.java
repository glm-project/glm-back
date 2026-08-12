package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnulationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.JourneesDeTravailService;
import com.glm.glmback.atelier.domain.Operateur;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.RegularisationDePresenceAEnregistrer;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la presence des operateurs.
 *
 * <p>
 * Arrivee, pause, reprise et depart sont des gestes de l'operateur lui-meme ; leur correction appartient au
 * gestionnaire.
 * </p>
 */
@Service
public class JourneesDeTravailApplicationService {

  private final JourneesDeTravailService journeesDeTravail;

  public JourneesDeTravailApplicationService(JourneeDeTravailRepository repository, Clock clock) {
    this.journeesDeTravail = new JourneesDeTravailService(repository, clock);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public JourneeDeTravail arrive(ArriveeAEnregistrer commande) {
    return journeesDeTravail.arrive(commande);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public JourneeDeTravail pointe(PointageDePresenceAEnregistrer commande) {
    return journeesDeTravail.pointe(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande) {
    return journeesDeTravail.regularise(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail annule(AnnulationDePresenceAEnregistrer commande) {
    return journeesDeTravail.annule(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande) {
    return journeesDeTravail.corrige(commande);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public JourneeDeTravail get(JourneeDeTravailId id) {
    return journeesDeTravail.get(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<JourneeDeTravail> list(Optional<Periode> periode, Optional<Operateur> operateur, Pageable pageable) {
    return journeesDeTravail.list(periode, operateur, pageable);
  }
}
