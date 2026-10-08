package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDossierAnomalie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestDossierAnomalieTest {

  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");

  @Test
  void shouldPublierLAdresseLaRevisionLEvaluationEtLActiviteEchue() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    var json = dossier(suivi, travail);

    assertThat(json.at("/adresse/suivi").asString()).isEqualTo(suivi.id().uuid().toString());
    assertThat(json.at("/adresse/pointage").asString()).isEqualTo(travail.id().uuid().toString());
    assertThat(json.path("revision").asLong()).isZero();
    assertThat(json.path("evaluation").asString()).isEqualTo("2026-05-10T22:00:00Z");
    assertThat(json.at("/activite/activite").asString()).isEqualTo(travail.activite().orElseThrow().uuid().toString());
    assertThat(json.at("/activite/fin").asString()).isEqualTo("2026-05-10T21:00:00Z");
    assertThat(json.at("/activite/duree").asString()).isEqualTo("PT13H");
  }

  @Test
  void shouldPublierLesPointagesDeLaCleSansLeSuiviNiLesAutresCles() {
    EvenementDAtelier dupont = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier martin = debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(dupont).enregistre(martin);

    var json = dossier(suivi, dupont);

    assertThat(json.path("pointages").size()).isEqualTo(1);
    assertThat(json.at("/pointages/0/id").asString()).isEqualTo(dupont.id().uuid().toString());
    assertThat(json.at("/pointages/0/operateurId").asString()).isEqualTo(OPERATEUR_ID_DUPONT.uuid().toString());
    assertThat(json.at("/pointages/0/posteId").asString()).isEqualTo(POSTE_ID_FRAISEUSE_1.uuid().toString());
    assertThat(json.has("suivi")).isFalse();
  }

  @Test
  void shouldNePlusPublierNiNatureNiConflitNiSequence() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    var json = dossier(suiviDAtelierEngage().enregistre(travail), travail);

    assertThat(json.propertyNames()).containsExactlyInAnyOrder("adresse", "revision", "evaluation", "activite", "pointages", "borneDeFin");
  }

  @Test
  void shouldPublierLaBorneDeFinQuandUnDebutSuitSurLaCle() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivant = debutSurFraiseuse1ParDupontA(A_22H);

    assertThat(dossier(suiviDAtelierEngage().enregistre(travail).enregistre(suivant), travail).path("borneDeFin").asString()).isEqualTo(
      "2026-05-10T22:00:00Z"
    );
    assertThat(dossier(suiviDAtelierEngage().enregistre(travail), travail).path("borneDeFin").isNull()).isTrue();
  }

  @Test
  void shouldPublierUneActiviteSansPosteQuandLOuvrantNEnAPas() {
    var travail = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);

    var json = dossier(suiviDAtelierEngage().enregistre(travail), travail);

    assertThat(json.at("/activite/posteId").isNull()).isTrue();
  }

  private static JsonNode dossier(SuiviDAtelier suivi, EvenementDAtelier ouvrant) {
    var dossier = LectureDossierAnomalie.de(new AdresseDossierAnomalie(suivi.id(), ouvrant.id()), new LectureDuSuivi(suivi, A_22H));

    return JsonMapper.builder().build().valueToTree(RestDossierAnomalie.from(dossier, annuaireDeDupontEtMartin()));
  }
}
