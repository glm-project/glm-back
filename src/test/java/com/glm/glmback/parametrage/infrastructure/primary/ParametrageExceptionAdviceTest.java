package com.glm.glmback.parametrage.infrastructure.primary;

import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.parametrage.domain.LogoInvalideException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class ParametrageExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new ParametrageExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new LogoInvalideException("Le fichier n'est pas une image lisible"),
        "urn:glm:erreur:parametrage:logo-invalide",
        BAD_REQUEST
      )
    );
  }
}
