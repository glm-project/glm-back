package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdvices.*;
import static com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AucuneJourneeDeTravailEnCoursException;
import com.glm.glmback.atelier.domain.ElementDejaEngageException;
import com.glm.glmback.atelier.domain.ElementEngageableIntrouvableException;
import com.glm.glmback.atelier.domain.EtatDActivite;
import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDePresenceDejaAnnuleException;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.EvenementDePresenceIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDejaAnnuleException;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.TransitionDAtelierInterditeException;
import com.glm.glmback.atelier.domain.TransitionDePresenceInterditeException;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

@UnitTest
class AtelierExceptionAdviceTest {

  private static final AtelierExceptionAdvice ADVICE = new AtelierExceptionAdvice();

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
    assertThat(exceptionsTranslatedBy(AtelierExceptionAdvice.class)).isEqualTo(exceptionsOf(erreursPubliees()));
  }

  @Test
  void shouldReporterLeMessageDeLErreur() {
    ProblemDetail probleme = translatedBy(ADVICE, new SaisieConcurrenteException(SuiviDAtelierId.newId()));

    assertThat(probleme.getProperties()).extracting("message").asString().contains("modifie par une autre saisie");
  }

  private static Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new SuiviDAtelierIntrouvableException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:suivi-d-atelier-introuvable",
        NOT_FOUND,
        "suivi d'atelier introuvable"
      ),
      new PublishedProblem(
        new JourneeDeTravailIntrouvableException(JourneeDeTravailId.newId()),
        "urn:glm:erreur:atelier:journee-de-travail-introuvable",
        NOT_FOUND,
        "journee de travail introuvable"
      ),
      new PublishedProblem(
        new EvenementDAtelierIntrouvableException(EvenementDAtelierId.newId()),
        "urn:glm:erreur:atelier:evenement-d-atelier-introuvable",
        NOT_FOUND,
        "evenement d'atelier introuvable"
      ),
      new PublishedProblem(
        new EvenementDePresenceIntrouvableException(EvenementDePresenceId.newId()),
        "urn:glm:erreur:atelier:evenement-de-presence-introuvable",
        NOT_FOUND,
        "evenement de presence introuvable"
      ),
      new PublishedProblem(
        new ElementEngageableIntrouvableException(ELEMENT_OF_2026_000042),
        "urn:glm:erreur:atelier:element-de-fabrication-introuvable",
        NOT_FOUND,
        "element de fabrication introuvable"
      ),
      new PublishedProblem(
        new OperateurDAtelierIntrouvableException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:atelier:operateur-introuvable",
        NOT_FOUND,
        "operateur introuvable"
      ),
      new PublishedProblem(
        new PosteDAtelierIntrouvableException(POSTE_ID_FRAISEUSE_1),
        "urn:glm:erreur:atelier:poste-de-travail-introuvable",
        NOT_FOUND,
        "poste de travail introuvable"
      ),
      new PublishedProblem(
        new AucuneJourneeDeTravailEnCoursException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:atelier:aucune-journee-de-travail-en-cours",
        NOT_FOUND,
        "aucune journee de travail en cours"
      ),
      new PublishedProblem(
        new OperateurNonHabiliteException(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1),
        "urn:glm:erreur:atelier:operateur-non-habilite",
        CONFLICT,
        "operateur non habilite"
      ),
      new PublishedProblem(
        new ElementDejaEngageException(ELEMENT_OF_2026_000042),
        "urn:glm:erreur:atelier:element-deja-engage",
        CONFLICT,
        "element deja engage"
      ),
      new PublishedProblem(
        new JourneeDeTravailDejaOuverteException(OPERATEUR_ID_DUPONT),
        "urn:glm:erreur:atelier:journee-de-travail-deja-ouverte",
        CONFLICT,
        "journee de travail deja ouverte"
      ),
      new PublishedProblem(
        new EvenementDejaAnnuleException(EvenementDAtelierId.newId()),
        "urn:glm:erreur:atelier:evenement-deja-annule",
        CONFLICT,
        "evenement deja annule"
      ),
      new PublishedProblem(
        new EvenementDePresenceDejaAnnuleException(EvenementDePresenceId.newId()),
        "urn:glm:erreur:atelier:evenement-de-presence-deja-annule",
        CONFLICT,
        "evenement de presence deja annule"
      ),
      new PublishedProblem(
        new SuiviDAtelierClotureException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:suivi-d-atelier-cloture",
        CONFLICT,
        "suivi d'atelier cloture"
      ),
      new PublishedProblem(
        new TransitionDAtelierInterditeException(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), EtatDActivite.EN_COURS),
        "urn:glm:erreur:atelier:transition-d-atelier-interdite",
        CONFLICT,
        "transition d'atelier interdite"
      ),
      new PublishedProblem(
        new TransitionDePresenceInterditeException(arriveeDeDupontA(LE_10_MAI_2026_A_7H), EtatDePresence.PRESENT),
        "urn:glm:erreur:atelier:transition-de-presence-interdite",
        CONFLICT,
        "transition de presence interdite"
      ),
      new PublishedProblem(
        new EvenementAvantEngagementException(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)),
        "urn:glm:erreur:atelier:evenement-anterieur-a-l-engagement",
        CONFLICT,
        "evenement anterieur a l'engagement"
      ),
      new PublishedProblem(
        new SaisieConcurrenteException(SuiviDAtelierId.newId()),
        "urn:glm:erreur:atelier:saisie-concurrente",
        CONFLICT,
        "saisie concurrente"
      )
    );
  }
}
