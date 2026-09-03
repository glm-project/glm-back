package com.glm.glmback.elementdefabrication.infrastructure.primary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdvices.*;
import static com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ReferenceDejaUtiliseeException;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

@UnitTest
class ElementDeFabricationExceptionAdviceTest {

  private static final ElementDeFabricationExceptionAdvice ADVICE = new ElementDeFabricationExceptionAdvice();

  @ParameterizedTest
  @MethodSource("erreursPubliees")
  void shouldPublierUnCodeStable(PublishedProblem attendu) {
    ProblemDetail probleme = translatedBy(ADVICE, attendu.exception());

    assertThat(probleme.getType()).hasToString(attendu.code());
    assertThat(probleme.getStatus()).isEqualTo(attendu.status().value());
    assertThat(probleme.getTitle()).isEqualTo(attendu.title());
  }

  @Test
  void shouldEprouverChaqueErreurTraduite() {
    assertThat(exceptionsTranslatedBy(ElementDeFabricationExceptionAdvice.class)).isEqualTo(exceptionsOf(erreursPubliees()));
  }

  @Test
  void shouldReporterLeMessageDeLErreur() {
    ProblemDetail probleme = translatedBy(ADVICE, new ReferenceDejaUtiliseeException(reference1015()));

    assertThat(probleme.getProperties()).extracting("message").asString().contains("deja utilisee par un autre element de fabrication");
  }

  private static Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new ElementDeFabricationIntrouvableException(ElementDeFabricationId.newId()),
        "urn:glm:erreur:element-de-fabrication:element-de-fabrication-introuvable",
        NOT_FOUND,
        "element de fabrication introuvable"
      ),
      new PublishedProblem(
        new ReferenceDejaUtiliseeException(reference1015()),
        "urn:glm:erreur:element-de-fabrication:reference-deja-utilisee",
        CONFLICT,
        "reference deja utilisee"
      )
    );
  }
}
