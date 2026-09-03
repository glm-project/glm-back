package com.glm.glmback.postedetravail.infrastructure.primary;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdvices.*;
import static com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.postedetravail.domain.LibelleDejaUtiliseException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailIntrouvableException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailPointeException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailUtiliseException;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

@UnitTest
class PosteDeTravailExceptionAdviceTest {

  private static final PosteDeTravailExceptionAdvice ADVICE = new PosteDeTravailExceptionAdvice();

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
    assertThat(exceptionsTranslatedBy(PosteDeTravailExceptionAdvice.class)).isEqualTo(exceptionsOf(erreursPubliees()));
  }

  @Test
  void shouldReporterLeMessageDeLErreur() {
    ProblemDetail probleme = translatedBy(ADVICE, new LibelleDejaUtiliseException(LIBELLE_TOUR_1));

    assertThat(probleme.getProperties()).extracting("message").asString().contains("deja utilise par un autre poste de travail");
  }

  private static Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new PosteDeTravailIntrouvableException(PosteDeTravailId.newId()),
        "urn:glm:erreur:poste-de-travail:poste-de-travail-introuvable",
        NOT_FOUND,
        "poste de travail introuvable"
      ),
      new PublishedProblem(
        new LibelleDejaUtiliseException(LIBELLE_TOUR_1),
        "urn:glm:erreur:poste-de-travail:libelle-deja-utilise",
        CONFLICT,
        "libelle deja utilise"
      ),
      new PublishedProblem(
        new PosteDeTravailPointeException(PosteDeTravailId.newId()),
        "urn:glm:erreur:poste-de-travail:poste-de-travail-pointe",
        CONFLICT,
        "poste de travail pointe"
      ),
      new PublishedProblem(
        new PosteDeTravailUtiliseException(PosteDeTravailId.newId()),
        "urn:glm:erreur:poste-de-travail:poste-de-travail-utilise",
        CONFLICT,
        "poste de travail utilise"
      )
    );
  }
}
