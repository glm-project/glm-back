package com.glm.glmback.postedetravail.infrastructure.primary;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.postedetravail.domain.LibelleDejaUtiliseException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailIntrouvableException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailPointeException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailUtiliseException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class PosteDeTravailExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new PosteDeTravailExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new PosteDeTravailIntrouvableException(PosteDeTravailId.newId()),
        "urn:glm:erreur:poste-de-travail:poste-de-travail-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new LibelleDejaUtiliseException(LIBELLE_TOUR_1),
        "urn:glm:erreur:poste-de-travail:libelle-deja-utilise",
        CONFLICT
      ),
      new PublishedProblem(
        new PosteDeTravailPointeException(PosteDeTravailId.newId()),
        "urn:glm:erreur:poste-de-travail:poste-de-travail-pointe",
        CONFLICT
      ),
      new PublishedProblem(
        new PosteDeTravailUtiliseException(PosteDeTravailId.newId()),
        "urn:glm:erreur:poste-de-travail:poste-de-travail-utilise",
        CONFLICT
      )
    );
  }
}
