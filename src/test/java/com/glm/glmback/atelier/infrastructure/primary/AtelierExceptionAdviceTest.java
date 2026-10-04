package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.ActiviteViseeIncoherenteException;
import com.glm.glmback.atelier.domain.ActiviteViseeIntrouvableException;
import com.glm.glmback.atelier.domain.DateDeSurvenueFutureException;
import com.glm.glmback.atelier.domain.ElementDejaEngageException;
import com.glm.glmback.atelier.domain.ElementEngageableIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDejaAnnuleException;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.gestionconflits.domain.ApercuInvalideException;
import com.glm.glmback.atelier.gestionconflits.domain.ApercuObsoleteException;
import com.glm.glmback.atelier.gestionconflits.domain.ConfirmationReutiliseeException;
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
      new PublishedProblem(new ApercuInvalideException(), "urn:glm:erreur:atelier:apercu-invalide", BAD_REQUEST),
      new PublishedProblem(new ApercuObsoleteException(), "urn:glm:erreur:atelier:apercu-obsolete", CONFLICT),
      new PublishedProblem(
        new ConfirmationReutiliseeException(java.util.UUID.randomUUID()),
        "urn:glm:erreur:atelier:confirmation-reutilisee",
        CONFLICT
      ),
      new PublishedProblem(
        new SuiviDAtelierIntrouvableException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:suivi-d-atelier-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new EvenementDAtelierIntrouvableException(EvenementDAtelierId.newId()),
        "urn:glm:erreur:atelier:evenement-d-atelier-introuvable",
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
        new ActiviteViseeIntrouvableException(
          debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H),
          new ActiviteId(java.util.UUID.randomUUID())
        ),
        "urn:glm:erreur:atelier:activite-visee-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new ActiviteViseeIncoherenteException(
          debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H),
          new ActiviteId(java.util.UUID.randomUUID())
        ),
        "urn:glm:erreur:atelier:activite-visee-incoherente",
        CONFLICT
      ),
      new PublishedProblem(
        new OperateurNonHabiliteException(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1),
        "urn:glm:erreur:atelier:operateur-non-habilite",
        CONFLICT
      ),
      new PublishedProblem(new ElementDejaEngageException(ELEMENT_OF_2026_000042), "urn:glm:erreur:atelier:element-deja-engage", CONFLICT),
      new PublishedProblem(
        new EvenementDejaAnnuleException(EvenementDAtelierId.newId()),
        "urn:glm:erreur:atelier:evenement-deja-annule",
        CONFLICT
      ),
      new PublishedProblem(
        new SuiviDAtelierClotureException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:suivi-d-atelier-cloture",
        CONFLICT
      ),
      new PublishedProblem(
        new EvenementAvantEngagementException(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)),
        "urn:glm:erreur:atelier:evenement-anterieur-a-l-engagement",
        CONFLICT
      ),
      new PublishedProblem(new SaisieConcurrenteException(SuiviDAtelierId.newId()), "urn:glm:erreur:atelier:saisie-concurrente", CONFLICT),
      new PublishedProblem(
        new IdentifiantDEvenementReutiliseException(java.util.UUID.randomUUID()),
        "urn:glm:erreur:atelier:identifiant-evenement-reutilise",
        CONFLICT
      ),
      new PublishedProblem(
        new DateDeSurvenueFutureException(LE_10_MAI_2026_A_8H),
        "urn:glm:erreur:atelier:date-de-survenue-future",
        BAD_REQUEST
      )
    );
  }
}
