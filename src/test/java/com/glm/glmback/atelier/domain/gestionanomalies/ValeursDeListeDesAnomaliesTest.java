package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.ConflitsFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import com.glm.glmback.shared.pagination.domain.Page;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ValeursDeListeDesAnomaliesTest {

  @Test
  void shouldExigerToutesLesReferencesDUneLigneSansJournal() {
    var adresse = new AdresseDossierAnomalie(SuiviDAtelierId.newId(), EvenementDAtelierId.newId());
    var revision = new RevisionDuSuivi(0);
    var element = elementFiltrePourcentA();
    var cle = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty());
    var repere = new RepereDeSequence(LE_10_MAI_2026_A_8H, 1);

    assertThatThrownBy(() -> new ConflitEnListe(null, revision, element, cle, repere)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new ConflitEnListe(adresse, null, element, cle, repere)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new ConflitEnListe(adresse, revision, null, cle, repere)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new ConflitEnListe(adresse, revision, element, null, repere)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new ConflitEnListe(adresse, revision, element, cle, null)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldExigerUnRepereDUnFaitReel() {
    assertThatThrownBy(() -> new RepereDeSequence(null, 1)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new RepereDeSequence(LE_10_MAI_2026_A_8H, 0)).isExactlyInstanceOf(NumberValueTooLowException.class);
    assertThat(new RepereDeSequence(LE_10_MAI_2026_A_8H.plusNanos(1), 1).premierPointage()).isEqualTo(LE_10_MAI_2026_A_8H.plusNanos(1));
  }

  @Test
  void shouldAdmettreLaRevisionInitialeEtLesGrandesRevisionsSansValeurNegative() {
    assertThat(new RevisionDuSuivi(0).value()).isZero();
    assertThat(new RevisionDuSuivi(Long.MAX_VALUE).value()).isEqualTo(Long.MAX_VALUE);
    assertThatThrownBy(() -> new RevisionDuSuivi(-1)).isExactlyInstanceOf(NumberValueTooLowException.class);
  }

  @Test
  void shouldExigerUnePageAcquiseEtSonAnnuaire() {
    var annuaire = new AnnuaireDAtelier(Map.of(), Map.of());
    var page = new Page<ConflitEnListe>(List.of(), 0, 20, 0);

    assertThatThrownBy(() -> new LectureDesConflits(null, annuaire)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new LectureDesConflits(page, null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThat(new LectureDesConflits(page, annuaire).page().totalElementsCount()).isZero();
  }

  @Test
  void shouldExigerToutesLesReferencesDUneFinAutomatiqueSansJournal() {
    var adresse = new AdresseDossierAnomalie(SuiviDAtelierId.newId(), EvenementDAtelierId.newId());
    var revision = new RevisionDuSuivi(0);
    var element = elementFiltrePourcentA();
    var cle = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty());
    var activite = new ActiviteId(UUID.randomUUID());

    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(null, revision, element, cle, activite, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H_UTC)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(adresse, null, element, cle, activite, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H_UTC)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(adresse, revision, null, cle, activite, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H_UTC)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(adresse, revision, element, null, activite, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H_UTC)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(adresse, revision, element, cle, null, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H_UTC)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(adresse, revision, element, cle, activite, null, LE_10_MAI_2026_A_21H_UTC)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new FinAutomatiqueEnListe(adresse, revision, element, cle, activite, LE_10_MAI_2026_A_8H, null)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldAccepterUneEcheanceEgaleAuDebutEtRefuserCelleQuiLePrecede() {
    var adresse = new AdresseDossierAnomalie(SuiviDAtelierId.newId(), EvenementDAtelierId.newId());
    var cle = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty());
    var activite = new ActiviteId(UUID.randomUUID());

    assertThatCode(() ->
      FinAutomatiqueEnListe.builder()
        .adresse(adresse)
        .revision(new RevisionDuSuivi(3))
        .element(elementFiltrePourcentA())
        .cle(cle)
        .activite(activite)
        .debut(LE_10_MAI_2026_A_8H)
        .echeance(LE_10_MAI_2026_A_8H)
    ).doesNotThrowAnyException();
    assertThatThrownBy(() ->
      FinAutomatiqueEnListe.builder()
        .adresse(adresse)
        .revision(new RevisionDuSuivi(3))
        .element(elementFiltrePourcentA())
        .cle(cle)
        .activite(activite)
        .debut(LE_10_MAI_2026_A_8H)
        .echeance(LE_10_MAI_2026_A_8H.minusNanos(1))
    ).isInstanceOf(com.glm.glmback.shared.error.domain.NotAfterTimeException.class);
  }

  @Test
  void shouldExigerUnePageDeFinsAutomatiquesEtSonAnnuaire() {
    var annuaire = new AnnuaireDAtelier(Map.of(), Map.of());
    var page = new Page<FinAutomatiqueEnListe>(List.of(), 0, 20, 0);

    assertThatThrownBy(() -> new LectureDesFinsAutomatiques(null, annuaire)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new LectureDesFinsAutomatiques(page, null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThat(new LectureDesFinsAutomatiques(page, annuaire).page().totalElementsCount()).isZero();
  }
}
