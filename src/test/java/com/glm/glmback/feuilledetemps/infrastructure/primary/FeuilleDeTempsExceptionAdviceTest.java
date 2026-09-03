package com.glm.glmback.feuilledetemps.infrastructure.primary;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdvices.*;
import static com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.feuilledetemps.domain.OperateurInconnuException;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

@UnitTest
class FeuilleDeTempsExceptionAdviceTest {

  private static final FeuilleDeTempsExceptionAdvice ADVICE = new FeuilleDeTempsExceptionAdvice();

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
    assertThat(exceptionsTranslatedBy(FeuilleDeTempsExceptionAdvice.class)).isEqualTo(exceptionsOf(erreursPubliees()));
  }

  @Test
  void shouldReporterLeMessageDeLErreur() {
    ProblemDetail probleme = translatedBy(ADVICE, new OperateurInconnuException(OPERATEUR_ID_DUPONT));

    assertThat(probleme.getProperties()).extracting("message").asString().contains("n'existe pas dans le referentiel");
  }

  private static Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new OperateurInconnuException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:feuille-de-temps:operateur-introuvable",
        NOT_FOUND,
        "operateur introuvable"
      )
    );
  }
}
