package com.glm.glmback.naturedetravail.infrastructure.primary;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.naturedetravail.domain.NatureDejaExistanteException;
import com.glm.glmback.naturedetravail.domain.NatureIntrouvableException;
import com.glm.glmback.naturedetravail.domain.NatureUtiliseeException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class NatureDeTravailExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new NatureDeTravailExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new NatureIntrouvableException(NATURE_DE_TRAVAIL_ID_SOUDAGE),
        "urn:glm:erreur:nature-de-travail:nature-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new NatureDejaExistanteException(LIBELLE_SOUDAGE),
        "urn:glm:erreur:nature-de-travail:nature-deja-existante",
        CONFLICT
      ),
      new PublishedProblem(
        new NatureUtiliseeException(NATURE_DE_TRAVAIL_ID_SOUDAGE),
        "urn:glm:erreur:nature-de-travail:nature-utilisee",
        CONFLICT
      )
    );
  }
}
