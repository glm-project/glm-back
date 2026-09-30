package com.glm.glmback.syntheseheures.infrastructure.primary;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import com.glm.glmback.syntheseheures.domain.EvaluationFutureException;
import com.glm.glmback.syntheseheures.domain.OperateurInconnuException;
import java.util.stream.Stream;

@UnitTest
class SyntheseDesHeuresExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new SyntheseDesHeuresExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new OperateurInconnuException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:synthese-des-heures:operateur-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new EvaluationFutureException(LE_MARDI_12_MAI_2026_A_10H),
        "urn:glm:erreur:synthese-des-heures:evaluation-future",
        BAD_REQUEST
      )
    );
  }
}
