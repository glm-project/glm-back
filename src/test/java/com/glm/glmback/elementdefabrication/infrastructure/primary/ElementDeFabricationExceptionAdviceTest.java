package com.glm.glmback.elementdefabrication.infrastructure.primary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ReferenceDejaUtiliseeException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class ElementDeFabricationExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new ElementDeFabricationExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new ElementDeFabricationIntrouvableException(ElementDeFabricationId.newId()),
        "urn:glm:erreur:element-de-fabrication:element-de-fabrication-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new ReferenceDejaUtiliseeException(reference1015()),
        "urn:glm:erreur:element-de-fabrication:reference-deja-utilisee",
        CONFLICT
      )
    );
  }
}
