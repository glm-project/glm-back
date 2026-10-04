package com.glm.glmback.atelier.gestionconflits.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.DiagnosticDeConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.RaisonDuConflit;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDossierConflitTest {

  @Test
  void shouldInclureUneCiblePreexistanteToucheeHorsDuPerimetreInitial() {
    var ancien = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nc = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H.plusSeconds(3600));
    var reprise = passageEnTravailDe(nc).a(LE_10_MAI_2026_A_12H);
    var fin = finDe(nc).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage()
      .enregistre(ancien)
      .enregistre(finDe(ancien).a(LE_10_MAI_2026_A_9H))
      .enregistre(nc)
      .enregistre(reprise)
      .enregistre(fin);
    var avant = new LectureDossierConflit(new AdresseDossierConflit(suivi.id(), fin.id()), new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H));
    assertThat(avant.concernees()).doesNotContain(ancien.activite().orElseThrow());
    var corrige = suivi.corrige(fin.id(), annulationParLeroy(), finDe(ancien).a(LE_10_MAI_2026_A_17H));

    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_17H));

    assertThat(apres.concernees()).contains(ancien.activite().orElseThrow());
    assertThat(corrige.activites()).extracting(Activite::id).contains(ancien.activite().orElseThrow());
    assertThat(apres.activites())
      .extracting(IntervalleDActivite::activite)
      .containsExactly(ancien.activite().orElseThrow(), nc.activite().orElseThrow(), reprise.activite().orElseThrow());
    assertThat(apres.perimetre())
      .get()
      .satisfies(perimetre -> assertThat(perimetre.pointages()).contains(ancien.id()));
  }

  @Test
  void shouldConserverUnConflitSansIntervalleApresCorrectionDeLAncre() {
    var ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(ouvrant).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(ouvrant).enregistre(fin).annule(ouvrant.id(), annulationParLeroy());
    var avant = new LectureDossierConflit(new AdresseDossierConflit(suivi.id(), fin.id()), new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H));
    var corrige = suivi.corrige(fin.id(), annulationParLeroy(), finDe(ouvrant).a(LE_10_MAI_2026_A_17H.minusSeconds(3600)));

    var apres = avant.apresActe(new LectureDuSuivi(corrige, LE_10_MAI_2026_A_17H));

    assertThat(avant.enConflit()).isTrue();
    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(apres.activites()).isEmpty();
    assertThat(apres.enConflit()).isTrue();
  }

  @Test
  void shouldDistinguerLePerimetreResoluDUnAutreConflitIndependant() {
    var dupont = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transitionDupont = passageEnTravailDe(dupont).a(LE_10_MAI_2026_A_12H);
    var martin = debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H);
    var transitionMartin = passageEnTravailDe(martin).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(dupont).enregistre(transitionDupont).enregistre(martin).enregistre(transitionMartin);
    var avant = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), transitionDupont.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );
    var annule = suivi.annule(transitionDupont.id(), annulationParLeroy());

    var apres = avant.apresActe(new LectureDuSuivi(annule, LE_10_MAI_2026_A_17H));

    assertThat(apres.enConflit()).isFalse();
    assertThat(apres.continuations())
      .singleElement()
      .extracting(conflit -> conflit.adresse().pointage())
      .isEqualTo(martin.id());
    assertThat(apres.perimetre())
      .get()
      .satisfies(perimetre -> assertThat(perimetre.pointages()).doesNotContain(martin.id(), transitionMartin.id()));
  }

  @Test
  void shouldNePasProposerDeGuidePourUneTransitionVersUneCibleRemplacee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var premiereTransition = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var transitionContradictoire = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(premiereTransition).enregistre(transitionContradictoire);
    var dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), transitionContradictoire.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.diagnostics()).singleElement().extracting(DiagnosticDeConflit::raison).isEqualTo(RaisonDuConflit.CIBLE_REMPLACEE);
    assertThat(dossier.choix()).isEmpty();
  }

  @Test
  void shouldNePasProposerLAnnulationDUneOuvertureIndependante() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nc = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nc).enregistre(fin);
    var dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), fin.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.diagnostics()).singleElement().extracting(DiagnosticDeConflit::raison).isEqualTo(RaisonDuConflit.CIBLE_REMPLACEE);
    assertThat(dossier.choix()).isEmpty();
  }

  @Test
  void shouldLireLaSequenceEtLeDiagnosticDepuisUnPointageActif() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(finDuTravail);

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), finDuTravail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.sequence()).contains(suivi.conflits().getFirst());
    assertThat(dossier.diagnostics()).singleElement().extracting(DiagnosticDeConflit::pointage).isEqualTo(finDuTravail.id());
    assertThat(dossier.lecture().suivi().journal().evenements()).containsExactly(travail, nonConformite, finDuTravail);
    assertThat(dossier.lecture().suivi().revision()).isEqualTo(suivi.revision());
  }

  @Test
  void shouldRendreUneAdresseIntrouvableAvecLeJournalAccessible() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier autrePointage = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), autrePointage.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.INTROUVABLE);
    assertThat(dossier.enConflit()).isFalse();
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.diagnostics()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenements()).containsExactly(travail);
  }

  @Test
  void shouldSignalerUneAncreAnnuleeSansOuvrirUneAutreSequence() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .annule(travail.id(), new Annulation(AUTEUR_LEROY, LE_10_MAI_2026_A_17H, MOTIF_ERREUR_DE_SAISIE));

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), travail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind().name()).isEqualTo("ANCRE_ANNULEE");
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenement(travail.id())).get().extracting(EvenementDAtelier::estAnnule).isEqualTo(true);
  }

  @Test
  void shouldSignalerUnPointageActifHorsConflit() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), travail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind().name()).isEqualTo("HORS_CONFLIT");
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenements()).containsExactly(travail);
  }

  @Test
  void shouldConserverLActiviteConcerneeEnCoursApresLAnnulationDeLAncre() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier transitionErronee = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(transitionErronee);
    LectureDossierConflit avant = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), transitionErronee.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_13H)
    );
    SuiviDAtelier annule = suivi.annule(transitionErronee.id(), new Annulation(AUTEUR_LEROY, LE_10_MAI_2026_A_13H, MOTIF_ERREUR_DE_SAISIE));

    LectureDossierConflit apres = avant.apresActe(new LectureDuSuivi(annule, LE_10_MAI_2026_A_13H));

    assertThat(apres.kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(apres.adresse()).isEqualTo(avant.adresse());
    assertThat(apres.activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.activite()).isEqualTo(travail.activite().orElseThrow());
        assertThat(activite.aResoudre()).isFalse();
        assertThat(activite.fin()).isEmpty();
      });
    assertThat(avant.lecture().suivi().conflits()).hasSize(1);
    assertThat(avant.choix()).isEmpty();
  }
}
