package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDossierFinAutomatiqueTest {

  private static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_10_MAI_2026_A_21H30 = Instant.parse("2026-05-10T21:30:00Z");
  private static final Instant LE_10_MAI_2026_A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H = Instant.parse("2026-05-10T23:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H30 = Instant.parse("2026-05-10T23:30:00Z");

  @Test
  void shouldOuvrirUnDossierDeFinAutomatiqueExactementALEcheance() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_21H).kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_21H.minusNanos(1)).kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
  }

  @Test
  void shouldConcernerLActiviteEchueDeLAncreSansSequence() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_22H);

    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.concernees()).containsExactly(travail.activite().orElseThrow());
    assertThat(dossier.finAutomatique()).isTrue();
    assertThat(dossier.enConflit()).isFalse();
    assertThat(dossier.activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.finAutomatique()).isTrue();
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_21H);
      });
    assertThat(dossier.perimetre())
      .get()
      .satisfies(perimetre -> {
        assertThat(perimetre.cle()).isEqualTo(travail.cle());
        assertThat(perimetre.activites()).containsExactly(travail.activite().orElseThrow());
        assertThat(perimetre.pointages()).containsExactly(travail.id());
      });
  }

  @Test
  void shouldNeRienConcernerQuandLActiviteDeLAncreEstEncoreEnCours() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_20H);

    assertThat(dossier.concernees()).isEmpty();
    assertThat(dossier.finAutomatique()).isFalse();
    assertThat(dossier.activites()).isEmpty();
    assertThat(dossier.choix()).isEmpty();
  }

  @Test
  void shouldNeRienConcernerQuandLAncreEstUneFinActive() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    var dossier = dossier(suivi, fin, LE_10_MAI_2026_A_23H);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
    assertThat(dossier.concernees()).isEmpty();
    assertThat(dossier.finAutomatique()).isFalse();
    assertThat(dossier.choix()).isEmpty();
  }

  @Test
  void shouldNePasOuvrirDeDossierDeFinAutomatiqueSurUneAncreAnnulee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail).annule(travail.id(), annulationParLeroy());

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_23H);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(dossier.concernees()).isEmpty();
    assertThat(dossier.finAutomatique()).isFalse();
  }

  @Test
  void shouldProposerLaRegularisationSansHeureQuandAucunGesteTardifNeVise() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_22H).choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.REGULARISER_FIN, travail.id(), travail.activite())
    );
  }

  @Test
  void shouldProposerLaRegularisationDeLActiviteQueOuvreUneTransitionAncree() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite);

    var dossier = dossier(suivi, nonConformite, LE_11_MAI_2026_A_3H);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(dossier.concernees()).containsExactly(nonConformite.activite().orElseThrow());
    assertThat(dossier.choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.REGULARISER_FIN, nonConformite.id(), nonConformite.activite())
    );
  }

  @Test
  void shouldProposerLaCorrectionDeLaFinTardive() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_23H30);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(dossier.perimetre())
      .get()
      .satisfies(perimetre -> assertThat(perimetre.pointages()).containsExactly(travail.id(), fin.id()));
    assertThat(dossier.choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_FIN_TARDIVE, fin.id(), travail.activite())
    );
  }

  @Test
  void shouldNeProposerQueLaPlusTardiveDesFinsTardives() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var premiereFin = finDe(travail).a(LE_10_MAI_2026_A_22H);
    var derniereFin = finDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(derniereFin).enregistre(premiereFin);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_23H30).choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_FIN_TARDIVE, derniereFin.id(), travail.activite())
    );
  }

  @Test
  void shouldProposerLaCorrectionDeLaTransitionTardiveSansRegulariserLaFin() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_23H30);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(dossier.choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_TRANSITION_TARDIVE, nonConformite.id(), travail.activite())
    );
  }

  @Test
  void shouldPreferLaTransitionTardiveACelleDUneFinTardive() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_22H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin).enregistre(nonConformite);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_23H30).choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_TRANSITION_TARDIVE, nonConformite.id(), travail.activite())
    );
  }

  @Test
  void shouldRegulariserLaFinQuandLaFinTardiveEstAnnulee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin).annule(fin.id(), annulationParLeroy());

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_23H30);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(dossier.choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.REGULARISER_FIN, travail.id(), travail.activite())
    );
  }

  @Test
  void shouldCorrigerLaFinTardiveQuandLaTransitionTardiveEstAnnulee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_22H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(fin)
      .enregistre(nonConformite)
      .annule(nonConformite.id(), annulationParLeroy());

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_23H30).choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_FIN_TARDIVE, fin.id(), travail.activite())
    );
  }

  @Test
  void shouldMettreEnConflitLaFinTardiveQuandLaTransitionTardiveCorrigeeEnRegularisationLaPrecede() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_22H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(fin);
    var avant = dossier(suivi, travail, LE_10_MAI_2026_A_23H30);

    var corrige = suivi.corrige(
      nonConformite.id(),
      annulationParLeroy(),
      passageEnNonConformiteRegulariseParLeroyDe(travail).a(LE_10_MAI_2026_A_22H)
    );
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_23H30));

    assertThat(avant.kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(avant.choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_TRANSITION_TARDIVE, nonConformite.id(), travail.activite())
    );
    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.EN_CONFLIT);
    assertThat(apres.diagnostics())
      .extracting(diagnostic -> diagnostic.pointage())
      .contains(fin.id());
  }

  @Test
  void shouldMenerLaSeulePropositionGuideeAUnConflitQuandUneRelanceSuitLEcheanceEtPrecedeLaFinTardive() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_21H30);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_22H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(relance).enregistre(fin);
    var avant = dossier(suivi, travail, LE_10_MAI_2026_A_23H30);

    var corrige = suivi.corrige(fin.id(), annulationParLeroy(), finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_22H));
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_23H30));

    assertThat(avant.kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(avant.choix()).containsExactly(
      new PropositionDeResolution(CodeDeProposition.CORRIGER_FIN_TARDIVE, fin.id(), travail.activite())
    );
    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.EN_CONFLIT);
  }

  @Test
  void shouldNePasProposerDeFinRegulariseeQuandLaSequenceEstEnConflit() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(fin);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_23H).kind()).isEqualTo(EtatDAdresseDossier.EN_CONFLIT);
    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_23H).finAutomatique()).isFalse();
    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_23H).choix())
      .extracting(PropositionDeResolution::code)
      .containsExactly(CodeDeProposition.RATTACHER_FIN_A_ACTIVITE_REMPLACANTE, CodeDeProposition.ANNULER_TRANSITION);
  }

  @Test
  void shouldTerminerLAnomalieQuandUneFinRegulariseeTermineLActivite() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    var avant = dossier(suivi, travail, LE_10_MAI_2026_A_22H);

    var apres = avant.apresActe(
      new LectureDuSuivi(suivi.enregistre(finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_17H)), LE_10_MAI_2026_A_22H)
    );

    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
    assertThat(apres.finAutomatique()).isFalse();
    assertThat(apres.enConflit()).isFalse();
    assertThat(apres.activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.finAutomatique()).isFalse();
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  @Test
  void shouldTerminerLAnomalieQuandLaFinTardiveEstCorrigeeEnRegularisation() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);
    var avant = dossier(suivi, travail, LE_10_MAI_2026_A_23H30);

    var corrige = suivi.corrige(fin.id(), annulationParLeroy(), finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_23H));
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_23H30));

    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
    assertThat(apres.finAutomatique()).isFalse();
    assertThat(apres.enConflit()).isFalse();
    assertThat(apres.activites()).singleElement().extracting(IntervalleDActivite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_23H));
  }

  @Test
  void shouldAnnulerLAncreSansFinAutomatiqueQuandLeDebutCorrigeRepousseLEcheance() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    var avant = dossier(suivi, travail, LE_10_MAI_2026_A_22H);

    var corrige = suivi.corrige(travail.id(), annulationParLeroy(), debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_12H));
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_22H));

    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(apres.finAutomatique()).isFalse();
    assertThat(apres.enConflit()).isFalse();
    assertThat(apres.activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).isEmpty();
        assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_12H);
      });
    assertThat(apres.perimetre())
      .get()
      .satisfies(perimetre -> assertThat(perimetre.activites()).contains(travail.activite().orElseThrow()));
  }

  @Test
  void shouldGarderLaFinAutomatiqueQuandLOuvrantCorrigeResteEchu() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    var avant = dossier(suivi, travail, LE_10_MAI_2026_A_23H);

    var remplacant = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_9H);
    var corrige = suivi.corrige(travail.id(), annulationParLeroy(), remplacant);
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_23H));

    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(apres.finAutomatique()).isTrue();
    assertThat(dossier(corrige, remplacant, LE_10_MAI_2026_A_23H).kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
  }

  @Test
  void shouldNeJugerQueLActiviteDeLAncreQuandLaTransitionCorrigeeOuvreUneActiviteDejaEchue() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite);
    var avant = dossier(suivi, travail, LE_11_MAI_2026_A_20H);

    var corrige = suivi.corrige(
      nonConformite.id(),
      annulationParLeroy(),
      passageEnNonConformiteRegulariseParLeroyDe(travail).a(LE_10_MAI_2026_A_23H)
    );
    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_11_MAI_2026_A_20H));

    assertThat(avant.concernees()).containsExactly(travail.activite().orElseThrow());
    assertThat(apres.concernees()).containsExactly(travail.activite().orElseThrow());
    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
    assertThat(apres.finAutomatique()).isFalse();
    assertThat(apres.activites()).singleElement().extracting(IntervalleDActivite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_23H));
  }

  private static LectureDossierAnomalie dossier(SuiviDAtelier suivi, EvenementDAtelier ancre, Instant evaluation) {
    return new LectureDossierAnomalie(new AdresseDossierAnomalie(suivi.id(), ancre.id()), new LectureDuSuivi(suivi, evaluation));
  }
}
