package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.gestionconflits.domain.AdresseDossierConflit;
import com.glm.glmback.atelier.gestionconflits.domain.LectureDossierConflit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestDossierConflitTest {

  @Test
  void shouldProposerLaContinuationSansChangerLAdresseDeLAncreCorrigee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nc = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nc).enregistre(fin);
    var avant = new LectureDossierConflit(new AdresseDossierConflit(suivi.id(), fin.id()), new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H));
    var corrige = suivi.corrige(fin.id(), annulationParLeroy(), finDe(travail).a(LE_10_MAI_2026_A_17H.minusSeconds(3600)));
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_17H));
    var json = JsonMapper.builder().build().valueToTree(RestDossierConflit.from(apres, annuaireDeDupontEtMartin()));
    assertThat(json.path("kind").asString()).isEqualTo("ANCRE_ANNULEE");
    assertThat(json.path("enConflit").asBoolean()).isTrue();
    assertThat(json.at("/adresse/pointage").asString()).isEqualTo(fin.id().uuid().toString());
    assertThat(json.path("continuations").size()).isEqualTo(1);
    assertThat(json.at("/continuations/0/adresse/pointage").asString()).isEqualTo(travail.id().uuid().toString());
    assertThat(json.at("/continuations/0/datePremierPointage").asString()).isEqualTo("2026-05-10T08:00:00Z");
    assertThat(json.at("/continuations/0/nombrePointages").asInt()).isEqualTo(3);
  }

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
    var dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), transitionMartin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );
    var json = JsonMapper.builder().build().valueToTree(RestDossierConflit.from(dossier, annuaireDeDupontEtMartin()));

    assertThat(json.at("/adresse/pointage").asString()).isEqualTo(transitionMartin.id().uuid().toString());
    assertThat(json.at("/sequence/operateurId").asString()).isEqualTo(OPERATEUR_ID_MARTIN.uuid().toString());
    assertThat(json.at("/sequence/datePremierPointage").asString()).isEqualTo("2026-05-10T09:00:00Z");
    assertThat(json.at("/sequence/nombrePointages").asInt()).isEqualTo(3);
    assertThat(json.at("/sequence/pointages/0").asString()).isEqualTo(martin.id().uuid().toString());
    assertThat(json.path("continuations").size()).isEqualTo(1);
    assertThat(json.at("/continuations/0/adresse/pointage").asString()).isEqualTo(dupont.id().uuid().toString());
  }

  @Test
  void shouldConserverLActiviteEnCoursSansDureeDefinitiveApresAnnulation() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(transition);
    var evaluation = LE_10_MAI_2026_A_12H.plusSeconds(3600);
    var avant = new LectureDossierConflit(new AdresseDossierConflit(suivi.id(), transition.id()), new LectureDuSuivi(suivi, evaluation));
    var apres = avant.apresActe(new LectureDuSuivi(suivi.annule(transition.id(), annulationParLeroy()), evaluation));

    var json = JsonMapper.builder().build().valueToTree(RestDossierConflit.from(apres, annuaireDeDupontEtMartin()));

    assertThat(json.path("kind").asString()).isEqualTo("ANCRE_ANNULEE");
    assertThat(json.at("/activites/0/activite").asString()).isEqualTo(travail.activite().orElseThrow().uuid().toString());
    assertThat(json.at("/activites/0/etat").asString()).isEqualTo("EN_COURS");
    assertThat(json.at("/activites/0/duree").isNull()).isTrue();

    assertThat(json.at("/activites/0/fin").isNull()).isTrue();
    assertThat(json.at("/perimetre/operateurId").asString()).isEqualTo(OPERATEUR_ID_DUPONT.uuid().toString());
    assertThat(json.at("/perimetre/datePremierPointage").asString()).isEqualTo("2026-05-10T08:00:00Z");
    assertThat(json.at("/perimetre/nombrePointages").asInt()).isEqualTo(2);
    assertThat(json.at("/perimetre/pointages/1").asString()).isEqualTo(transition.id().uuid().toString());
    assertThat(json.path("sequence").isNull()).isTrue();
  }

  @Test
  void shouldPublierLeGesteSaCibleEtLeFaitQuiLARemplacee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(fin);
    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), fin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    var json = JsonMapper.builder().build().valueToTree(RestDossierConflit.from(dossier, annuaireDeDupontEtMartin()));

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
    assertThat(json.path("choix").size()).isEqualTo(2);
    assertThat(json.at("/choix/0/code").asString()).isEqualTo("RATTACHER_FIN_A_ACTIVITE_REMPLACANTE");
    assertThat(json.at("/choix/0/kind").asString()).isEqualTo("CORRECTION");
    assertThat(json.at("/choix/0/pointage").asString()).isEqualTo(fin.id().uuid().toString());
    assertThat(json.at("/choix/0/fait/activiteVisee").asString()).isEqualTo(nonConformite.activite().orElseThrow().uuid().toString());
    assertThat(json.at("/choix/0/fait/instant").asString()).isEqualTo("2026-05-10T17:00:00Z");
    assertThat(json.at("/choix/1/code").asString()).isEqualTo("ANNULER_TRANSITION");
    assertThat(json.at("/choix/1/kind").asString()).isEqualTo("ANNULATION");
    assertThat(json.at("/choix/1/pointage").asString()).isEqualTo(nonConformite.id().uuid().toString());
    assertThat(json.at("/choix/0/motif").isMissingNode()).isTrue();
    assertThat(json.at("/choix/1/motif").isMissingNode()).isTrue();
  }

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
