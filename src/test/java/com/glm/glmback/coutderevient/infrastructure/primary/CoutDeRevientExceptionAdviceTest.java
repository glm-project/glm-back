package com.glm.glmback.coutderevient.infrastructure.primary;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.coutderevient.domain.ElementInconnuException;
import com.glm.glmback.coutderevient.domain.RapportNonExportableException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class CoutDeRevientExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new CoutDeRevientExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new ElementInconnuException(ELEMENT_ID_OF),
        "urn:glm:erreur:cout-de-revient:element-de-fabrication-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new RapportNonExportableException(COUT_DE_REVIENT_VIDE),
        "urn:glm:erreur:cout-de-revient:rapport-non-exportable",
        CONFLICT
      )
    );
  }
}
