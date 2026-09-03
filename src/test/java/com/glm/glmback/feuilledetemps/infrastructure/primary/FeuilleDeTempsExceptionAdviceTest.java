package com.glm.glmback.feuilledetemps.infrastructure.primary;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.feuilledetemps.domain.OperateurInconnuException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class FeuilleDeTempsExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new FeuilleDeTempsExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new OperateurInconnuException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:feuille-de-temps:operateur-introuvable",
        NOT_FOUND
      )
    );
  }
}
