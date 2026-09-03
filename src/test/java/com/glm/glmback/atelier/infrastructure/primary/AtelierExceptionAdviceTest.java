package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AucuneJourneeDeTravailEnCoursException;
import com.glm.glmback.atelier.domain.ElementDejaEngageException;
import com.glm.glmback.atelier.domain.ElementEngageableIntrouvableException;
import com.glm.glmback.atelier.domain.EtatDActivite;
import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDePresenceDejaAnnuleException;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.EvenementDePresenceIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDejaAnnuleException;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.TransitionDAtelierInterditeException;
import com.glm.glmback.atelier.domain.TransitionDePresenceInterditeException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class AtelierExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new AtelierExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new SuiviDAtelierIntrouvableException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:suivi-d-atelier-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new JourneeDeTravailIntrouvableException(JourneeDeTravailId.newId()),
        "urn:glm:erreur:atelier:journee-de-travail-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new EvenementDAtelierIntrouvableException(EvenementDAtelierId.newId()),
        "urn:glm:erreur:atelier:evenement-d-atelier-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new EvenementDePresenceIntrouvableException(EvenementDePresenceId.newId()),
        "urn:glm:erreur:atelier:evenement-de-presence-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new ElementEngageableIntrouvableException(ELEMENT_OF_2026_000042),
        "urn:glm:erreur:atelier:element-de-fabrication-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new OperateurDAtelierIntrouvableException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:atelier:operateur-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new PosteDAtelierIntrouvableException(POSTE_ID_FRAISEUSE_1),
        "urn:glm:erreur:atelier:poste-de-travail-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new AucuneJourneeDeTravailEnCoursException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:atelier:aucune-journee-de-travail-en-cours",
        NOT_FOUND
      ),
      new PublishedProblem(
        new OperateurNonHabiliteException(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1),
        "urn:glm:erreur:atelier:operateur-non-habilite",
        CONFLICT
      ),
      new PublishedProblem(new ElementDejaEngageException(ELEMENT_OF_2026_000042), "urn:glm:erreur:atelier:element-deja-engage", CONFLICT),
      new PublishedProblem(
        new JourneeDeTravailDejaOuverteException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:atelier:journee-de-travail-deja-ouverte",
        CONFLICT
      ),
      new PublishedProblem(
        new EvenementDejaAnnuleException(EvenementDAtelierId.newId()),
        "urn:glm:erreur:atelier:evenement-deja-annule",
        CONFLICT
      ),
      new PublishedProblem(
        new EvenementDePresenceDejaAnnuleException(EvenementDePresenceId.newId()),
        "urn:glm:erreur:atelier:evenement-de-presence-deja-annule",
        CONFLICT
      ),
      new PublishedProblem(
        new SuiviDAtelierClotureException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:suivi-d-atelier-cloture",
        CONFLICT
      ),
      new PublishedProblem(
        new TransitionDAtelierInterditeException(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), EtatDActivite.EN_COURS),
        "urn:glm:erreur:atelier:transition-d-atelier-interdite",
        CONFLICT
      ),
      new PublishedProblem(
        new TransitionDePresenceInterditeException(arriveeDeDupontA(LE_10_MAI_2026_A_7H), EtatDePresence.PRESENT),
        "urn:glm:erreur:atelier:transition-de-presence-interdite",
        CONFLICT
      ),
      new PublishedProblem(
        new EvenementAvantEngagementException(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)),
        "urn:glm:erreur:atelier:evenement-anterieur-a-l-engagement",
        CONFLICT
      ),
      new PublishedProblem(new SaisieConcurrenteException(SuiviDAtelierId.newId()), "urn:glm:erreur:atelier:saisie-concurrente", CONFLICT)
    );
  }
}
