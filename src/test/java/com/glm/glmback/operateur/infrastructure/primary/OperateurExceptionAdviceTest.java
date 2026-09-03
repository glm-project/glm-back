package com.glm.glmback.operateur.infrastructure.primary;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.operateur.domain.IdentiteDejaUtiliseeException;
import com.glm.glmback.operateur.domain.MatriculeDejaUtiliseException;
import com.glm.glmback.operateur.domain.OperateurAPointeException;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateurIntrouvableException;
import com.glm.glmback.operateur.domain.PosteHabilitableIntrouvableException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class OperateurExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new OperateurExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new OperateurIntrouvableException(OperateurId.newId()),
        "urn:glm:erreur:operateur:operateur-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new PosteHabilitableIntrouvableException(ID_TOUR_1),
        "urn:glm:erreur:operateur:poste-de-travail-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(new OperateurAPointeException(OperateurId.newId()), "urn:glm:erreur:operateur:operateur-ayant-pointe", CONFLICT),
      new PublishedProblem(
        new IdentiteDejaUtiliseeException(NOM_DUPONT, PRENOM_JEAN),
        "urn:glm:erreur:operateur:identite-deja-utilisee",
        CONFLICT
      ),
      new PublishedProblem(new MatriculeDejaUtiliseException(MATRICULE_049), "urn:glm:erreur:operateur:matricule-deja-utilise", CONFLICT)
    );
  }
}
