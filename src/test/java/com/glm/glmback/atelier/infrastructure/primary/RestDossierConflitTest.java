package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AdresseDossierConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestDossierConflitTest {

  @Test
  void shouldPublierLAdresseLaRevisionEtLHistoriqueDuMemeSuivi() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(fin);
    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), fin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    var json = JsonMapper.builder().build().valueToTree(RestDossierConflit.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.path("kind").asString()).isEqualTo("EN_CONFLIT");
    assertThat(json.at("/adresse/suivi").asString()).isEqualTo(suivi.id().uuid().toString());
    assertThat(json.at("/adresse/pointage").asString()).isEqualTo(fin.id().uuid().toString());
    assertThat(json.path("revision").asLong()).isZero();
    assertThat(json.path("evaluation").asString()).isEqualTo("2026-05-10T17:00:00Z");
    assertThat(json.at("/suivi/journal").size()).isEqualTo(3);
    assertThat(json.at("/suivi/journal/0/operateurId").asString()).isEqualTo(OPERATEUR_ID_DUPONT.uuid().toString());
  }
}
