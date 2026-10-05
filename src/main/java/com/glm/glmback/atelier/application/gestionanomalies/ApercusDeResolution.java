package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.gestionanomalies.ActeDeResolution;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.gestionanomalies.EtatDAdresseDossier;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDossierAnomalie;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApercusDeResolution {

  private final SuiviDAtelierRepository suivis;
  private final PreparationDesActes preparation;
  private final Clock clock;

  ApercusDeResolution(SuiviDAtelierRepository suivis, PreparationDesActes preparation, Clock clock) {
    this.suivis = suivis;
    this.preparation = preparation;
    this.clock = clock;
  }

  public static SuivisBuilder builder() {
    return suivis -> preparation -> clock -> new ApercusDeResolution(suivis, preparation, clock);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public ApercuDeResolution apercu(
    UUID commande,
    AdresseDossierAnomalie adresse,
    RevisionDuSuivi revision,
    ActeDeResolution acte,
    ContexteDeResolution contexte
  ) {
    var suivi = suivis.get(adresse.suivi()).orElseThrow(() -> new SuiviDAtelierIntrouvableException(adresse.suivi()));
    if (!suivi.revision().equals(revision)) {
      throw new ApercuObsoleteException();
    }
    var maintenant = clock.now();
    var avant = new LectureDossierAnomalie(adresse, new LectureDuSuivi(suivi, maintenant));
    if (avant.kind() != EtatDAdresseDossier.EN_CONFLIT) {
      throw new ApercuObsoleteException();
    }
    Optional<EvenementDAtelierId> evenement =
      acte instanceof ActeDeResolution.Annulation ? Optional.empty() : Optional.of(EvenementDAtelierId.newId());
    var prepare = preparation.prepare(suivi, acte, evenement, contexte.gestionnaire().auteur(), maintenant);
    var proposition = PropositionAConfirmer.builder()
      .commande(commande)
      .adresse(adresse)
      .revision(suivi.revision())
      .acte(acte)
      .evenement(evenement)
      .empreinteConsequences(prepare.empreinteConsequences());
    return new ApercuDeResolution(proposition, maintenant, avant, avant.apresActe(new LectureDuSuivi(prepare.apres(), maintenant)));
  }

  public interface SuivisBuilder {
    PreparationBuilder suivis(SuiviDAtelierRepository value);
  }

  public interface PreparationBuilder {
    ClockBuilder preparation(PreparationDesActes value);
  }

  public interface ClockBuilder {
    ApercusDeResolution clock(Clock value);
  }
}
