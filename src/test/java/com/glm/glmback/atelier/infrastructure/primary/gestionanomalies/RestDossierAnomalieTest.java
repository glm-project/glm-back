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
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestDossierAnomalieTest {

  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");

  @Test
  void shouldConserverLAncreDemandeeEtLePerimetreDUneSeuleSequence() {
    var dupont = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var martin = debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H);
    var transitionMartin = passageEnNonConformiteDe(martin).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage()
      .enregistre(dupont)
      .enregistre(passageEnNonConformiteDe(dupont).a(LE_10_MAI_2026_A_12H))
      .enregistre(finDe(dupont).a(LE_10_MAI_2026_A_17H))
      .enregistre(martin)
      .enregistre(transitionMartin)
      .enregistre(finDe(martin).a(LE_10_MAI_2026_A_17H));
    var dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), transitionMartin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );
    var json = JsonMapper.builder().build().valueToTree(RestDossierAnomalie.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.at("/adresse/pointage").asString()).isEqualTo(transitionMartin.id().uuid().toString());
    assertThat(json.at("/sequence/operateurId").asString()).isEqualTo(OPERATEUR_ID_MARTIN.uuid().toString());
    assertThat(json.at("/sequence/datePremierPointage").asString()).isEqualTo("2026-05-10T09:00:00Z");
    assertThat(json.at("/sequence/nombrePointages").asInt()).isEqualTo(3);
    assertThat(json.at("/sequence/pointages/0").asString()).isEqualTo(martin.id().uuid().toString());
    assertThat(json.path("continuations").size()).isEqualTo(1);
    assertThat(json.at("/continuations/0/adresse/pointage").asString()).isEqualTo(dupont.id().uuid().toString());
  }

  @Test
  void shouldPublierLeGesteSaCibleEtLeFaitQuiLARemplacee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(fin);
    LectureDossierAnomalie dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), fin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    var json = JsonMapper.builder().build().valueToTree(RestDossierAnomalie.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.at("/diagnostics/0/raison").asString()).isEqualTo("CIBLE_REMPLACEE");
    assertThat(json.at("/diagnostics/0/pointage").asString()).isEqualTo(fin.id().uuid().toString());
    assertThat(json.at("/diagnostics/0/cible/activite").asString()).isEqualTo(travail.activite().orElseThrow().uuid().toString());
    assertThat(json.at("/diagnostics/0/cible/ouvrant").asString()).isEqualTo(travail.id().uuid().toString());
    assertThat(json.at("/diagnostics/0/cible/termineePar").asString()).isEqualTo(nonConformite.id().uuid().toString());
    assertThat(json.at("/activites/0/etat").asString()).isEqualTo("A_RESOUDRE");
    assertThat(json.at("/activites/0/duree").isNull()).isTrue();
    assertThat(json.at("/sequence/operateurId").asString()).isEqualTo(OPERATEUR_ID_DUPONT.uuid().toString());
    assertThat(json.at("/sequence/posteId").asString()).isEqualTo(POSTE_ID_FRAISEUSE_1.uuid().toString());
    assertThat(json.at("/sequence/pointages").size()).isEqualTo(3);
    assertThat(json.at("/sequence/datePremierPointage").asString()).isEqualTo("2026-05-10T08:00:00Z");
    assertThat(json.at("/sequence/nombrePointages").asInt()).isEqualTo(3);
  }

  @Test
  void shouldPublierLAdresseLaRevisionEtLHistoriqueDuMemeSuivi() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(fin);
    LectureDossierAnomalie dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), fin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    var json = JsonMapper.builder().build().valueToTree(RestDossierAnomalie.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.path("kind").asString()).isEqualTo("EN_CONFLIT");
    assertThat(json.at("/adresse/suivi").asString()).isEqualTo(suivi.id().uuid().toString());
    assertThat(json.at("/adresse/pointage").asString()).isEqualTo(fin.id().uuid().toString());
    assertThat(json.path("revision").asLong()).isZero();
    assertThat(json.path("evaluation").asString()).isEqualTo("2026-05-10T17:00:00Z");
    assertThat(json.at("/suivi/journal").size()).isEqualTo(3);
    assertThat(json.at("/suivi/journal/0/operateurId").asString()).isEqualTo(OPERATEUR_ID_DUPONT.uuid().toString());
  }

  @Test
  void shouldPublierLeDossierDUneFinAutomatique() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    var dossier = new LectureDossierAnomalie(new AdresseDossierAnomalie(suivi.id(), travail.id()), new LectureDuSuivi(suivi, A_22H));

    var json = JsonMapper.builder().build().valueToTree(RestDossierAnomalie.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.path("kind").asString()).isEqualTo("FIN_AUTOMATIQUE");
    assertThat(json.path("finAutomatique").asBoolean()).isTrue();
    assertThat(json.path("enConflit").asBoolean()).isFalse();
    assertThat(json.path("sequence").isNull()).isTrue();
    assertThat(json.at("/perimetre/pointages/0").asString()).isEqualTo(travail.id().uuid().toString());
    assertThat(json.at("/activites/0/activite").asString()).isEqualTo(travail.activite().orElseThrow().uuid().toString());
    assertThat(json.at("/activites/0/etat").asString()).isEqualTo("ECHUE");
    assertThat(json.at("/activites/0/fin").asString()).isEqualTo("2026-05-10T21:00:00Z");
    assertThat(json.at("/activites/0/duree").asString()).isEqualTo("PT13H");
  }

  @Test
  void shouldPublierUneActiviteSansPosteQuandLOuvrantNEnAPas() {
    var travail = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    var dossier = new LectureDossierAnomalie(new AdresseDossierAnomalie(suivi.id(), travail.id()), new LectureDuSuivi(suivi, A_22H));

    var json = JsonMapper.builder().build().valueToTree(RestDossierAnomalie.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.at("/activites/0/posteId").isNull()).isTrue();
  }
}
