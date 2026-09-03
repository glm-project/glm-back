package com.glm.glmback.operateur.infrastructure.primary;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdvices.*;
import static com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.operateur.domain.IdentiteDejaUtiliseeException;
import com.glm.glmback.operateur.domain.MatriculeDejaUtiliseException;
import com.glm.glmback.operateur.domain.OperateurAPointeException;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateurIntrouvableException;
import com.glm.glmback.operateur.domain.PosteHabilitableIntrouvableException;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

@UnitTest
class OperateurExceptionAdviceTest {

  private static final OperateurExceptionAdvice ADVICE = new OperateurExceptionAdvice();

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
    assertThat(exceptionsTranslatedBy(OperateurExceptionAdvice.class)).isEqualTo(exceptionsOf(erreursPubliees()));
  }

  @Test
  void shouldReporterLeMessageDeLErreur() {
    ProblemDetail probleme = translatedBy(ADVICE, new MatriculeDejaUtiliseException(MATRICULE_049));

    assertThat(probleme.getProperties()).extracting("message").asString().contains("deja utilise par un autre operateur");
  }

  private static Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new OperateurIntrouvableException(OperateurId.newId()),
        "urn:glm:erreur:operateur:operateur-introuvable",
        NOT_FOUND,
        "operateur introuvable"
      ),
      new PublishedProblem(
        new PosteHabilitableIntrouvableException(ID_TOUR_1),
        "urn:glm:erreur:operateur:poste-de-travail-introuvable",
        NOT_FOUND,
        "poste de travail introuvable"
      ),
      new PublishedProblem(
        new OperateurAPointeException(OperateurId.newId()),
        "urn:glm:erreur:operateur:operateur-ayant-pointe",
        CONFLICT,
        "operateur ayant pointe"
      ),
      new PublishedProblem(
        new IdentiteDejaUtiliseeException(NOM_DUPONT, PRENOM_JEAN),
        "urn:glm:erreur:operateur:identite-deja-utilisee",
        CONFLICT,
        "identite deja utilisee"
      ),
      new PublishedProblem(
        new MatriculeDejaUtiliseException(MATRICULE_049),
        "urn:glm:erreur:operateur:matricule-deja-utilise",
        CONFLICT,
        "matricule deja utilise"
      )
    );
  }
}
