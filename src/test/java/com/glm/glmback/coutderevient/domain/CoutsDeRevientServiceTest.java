package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Les regles du rapport par son service public, depuis les bornes interpretees d'atelier. */
@UnitTest
class CoutsDeRevientServiceTest {

  @Test
  void shouldRefuseAnUnknownElement() {
    assertThatThrownBy(() -> service(new AtelierEnMemoire(), LE_11_MAI_A_17H).rapport(ELEMENT_ID_OF))
      .isExactlyInstanceOf(ElementInconnuException.class)
      .hasMessageContaining(ELEMENT_ID_OF.uuid().toString());
  }

  @Test
  void shouldRenderAnEmptyRapportForAnElementNeverEngaged() {
    CoutDeRevient rapport = service(new AtelierEnMemoire().connait(ELEMENT_VALORISE_OF), LE_11_MAI_A_17H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.element()).isEqualTo(ELEMENT_VALORISE_OF);
    assertThat(rapport.lignes()).isEmpty();
    assertThat(rapport.cout()).isEqualTo(Cout.AUCUN);
  }

  @Test
  void shouldExcludeAnOpenActivityFromTheHumanDivisor() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H))))
      .aMeneDeFront(activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_9H, Optional.empty())));
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_10H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.cout().mainDOeuvre().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("40.00")));
    assertThat(rapport.cout().machine().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("90.00")));
  }

  @Test
  void shouldDivideWhenTheOtherActivityEnds() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H))))
      .aMeneDeFront(activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_11H))));
    assertThat(service(atelier, LE_11_MAI_A_11H).rapport(ELEMENT_ID_OF).cout().mainDOeuvre().valeur().orElseThrow()).isEqualTo(
      new Montant(new BigDecimal("30.00"))
    );
  }

  @Test
  void shouldExcludeEveryCostOfAnActivityBeforeItsDeadline() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.empty())));
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_21H.minusSeconds(1)).rapport(ELEMENT_ID_OF);
    assertThat(rapport.lignes()).isEmpty();
    assertThat(rapport.temps()).isEqualTo(TempsPasse.AUCUN);
    assertThat(rapport.cout()).isEqualTo(Cout.AUCUN);
  }

  @Test
  void shouldValoriseThirteenHoursAtTheAutomaticEnd() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.empty())));
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_21H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.temps().travail().valeur().orElseThrow()).isEqualTo(Duration.ofHours(13));
    assertThat(rapport.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("585.00")), new Montant(new BigDecimal("260.00"))));
    assertThat(service(atelier, LE_12_MAI_A_18H).rapport(ELEMENT_ID_OF).temps()).isEqualTo(rapport.temps());
  }

  @Test
  void shouldKeepAKnownRealEndAfterEvaluationAndTheDeadline() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_21H.plusSeconds(7200)))));
    assertThat(service(atelier, LE_11_MAI_A_17H).rapport(ELEMENT_ID_OF).temps().travail().valeur().orElseThrow()).isEqualTo(
      Duration.ofHours(15)
    );
  }

  @Test
  void shouldPreserveWorkAndAnOpenNonConformity() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(
        ELEMENT_ID_OF,
        activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H))),
        new ActiviteInterpretee(
          new ActiviteId(java.util.UUID.randomUUID()),
          ACTIVITE_NC_FRAISAGE,
          new Plage(LE_11_MAI_A_12H, Optional.empty()),
          LE_12_MAI_A_8H.minusSeconds(25200),
          Optional.empty()
        )
      );
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_21H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.temps()).isEqualTo(new TempsPasse(Duration.ofHours(4), Duration.ZERO));
    assertThat(rapport.cout().mainDOeuvre().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("80.00")));
  }

  @Test
  void shouldLeaveHumanCostUnresolvedWhenAnotherPosteMayBeOccupied() {
    ActiviteInterpretee incertaine = activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.empty()));
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_8H.minusSeconds(7200), Optional.of(LE_11_MAI_A_10H))))
      .aMeneDeFront(
        ActiviteInterpretee.builder()
          .id(incertaine.id())
          .activite(incertaine.activite())
          .plage(incertaine.plage())
          .echeance(incertaine.echeance())
          .finAuPlusTard(Optional.of(LE_11_MAI_A_21H))
      );

    CoutDeRevient rapport = service(atelier, LE_12_MAI_A_18H).rapport(ELEMENT_ID_OF);

    assertThat(rapport.cout().mainDOeuvre().valeur()).isEmpty();
    assertThat(rapport.cout().machine().valeur()).contains(new Montant(new BigDecimal("240.00")));
    assertThat(rapport.temps().travail().valeur()).contains(Duration.ofHours(4));
  }

  @ParameterizedTest
  @CsvSource(
    {
      "2026-05-11T06:00:00Z, 2026-05-11T08:00:00Z, 2026-05-12T18:00:00Z, 2026-05-11T21:00:00Z, 40.00",
      "2026-05-11T21:00:00Z, 2026-05-11T22:00:00Z, 2026-05-12T18:00:00Z, 2026-05-11T21:00:00Z, 20.00",
      "2026-05-11T08:00:00Z, 2026-05-11T10:00:00Z, 2026-05-11T08:00:00Z, 2026-05-11T21:00:00Z, 40.00",
      "2026-05-11T08:00:00Z, 2026-05-11T10:00:00Z, 2026-05-11T09:00:00Z, 2026-05-11T21:00:00Z, incomplet",
      "2026-05-11T22:00:00Z, 2026-05-11T23:00:00Z, 2026-05-12T18:00:00Z, 2026-05-11T23:00:00Z, incomplet",
      "2026-05-11T23:00:00Z, 2026-05-12T00:00:00Z, 2026-05-12T18:00:00Z, 2026-05-11T23:00:00Z, 20.00",
    }
  )
  void shouldUseTheWholePossibleRangeBoundedByEvaluation(String debut, String fin, String evaluation, String borne, String attendu) {
    ActiviteInterpretee source = activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.empty()));
    ActiviteInterpretee incertaine = new ActiviteInterpretee(
      source.id(),
      source.activite(),
      source.plage(),
      source.echeance(),
      Optional.of(Instant.parse(borne))
    );
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeTournage(new Plage(Instant.parse(debut), Optional.of(Instant.parse(fin)))))
      .aMeneDeFront(incertaine);
    MontantTotal humain = service(atelier, Instant.parse(evaluation)).rapport(ELEMENT_ID_OF).cout().mainDOeuvre();
    if ("incomplet".equals(attendu)) {
      assertThat(humain.valeur()).isEmpty();
    } else {
      assertThat(humain.valeur()).contains(new Montant(new BigDecimal(attendu)));
    }
  }

  @Test
  void shouldKnowTheDivisorWhenTheUncertainPosteIsAlreadyCertainlyOccupied() {
    ActiviteInterpretee source = activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.empty()));
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H))))
      .aMeneDeFront(
        activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H))),
        new ActiviteInterpretee(source.id(), source.activite(), source.plage(), source.echeance(), Optional.of(LE_11_MAI_A_21H))
      );
    assertThat(service(atelier, LE_12_MAI_A_18H).rapport(ELEMENT_ID_OF).cout().mainDOeuvre().valeur()).contains(
      new Montant(new BigDecimal("20.00"))
    );
  }

  @Test
  void shouldNotDependOnAnUncertainDivisorWithoutAnOperatorRate() {
    ActiviteInterpretee source = activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.empty()));
    ActiviteInterpretee cible = activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H)));
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(
        ELEMENT_ID_OF,
        new ActiviteInterpretee(cible.id(), ACTIVITE_TOURNAGE_SANS_TAUX, cible.plage(), cible.echeance(), cible.finAuPlusTard())
      )
      .aMeneDeFront(
        new ActiviteInterpretee(source.id(), source.activite(), source.plage(), source.echeance(), Optional.of(LE_11_MAI_A_21H))
      );
    CoutDeRevient rapport = service(atelier, LE_12_MAI_A_18H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.cout().mainDOeuvre().valeur()).contains(Montant.ZERO);
    assertThat(rapport.cout().total().valeur()).contains(new Montant(new BigDecimal("120.00")));
  }

  @Test
  void shouldKeepTheOwnTranchesInTheDivisorWhenOccupationReturnsNothing() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, activiteInterpreteeDeFraisage(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H))));
    CoutsDeRevientService service = CoutsDeRevientService.builder()
      .elements(atelier)
      .travaux(atelier)
      .occupations((operateurs, periode) -> java.util.List.of())
      .conflits(atelier)
      .clock(() -> LE_11_MAI_A_17H);
    assertThat(service.rapport(ELEMENT_ID_OF).cout().mainDOeuvre().valeur()).contains(new Montant(new BigDecimal("40.00")));
  }

  @Test
  void shouldSampleTheClockOnceIncludingAnEmptyRapport() {
    java.util.concurrent.atomic.AtomicInteger lectures = new java.util.concurrent.atomic.AtomicInteger();
    AtelierEnMemoire atelier = new AtelierEnMemoire().connait(ELEMENT_VALORISE_OF);
    CoutsDeRevientService service = CoutsDeRevientService.builder()
      .elements(atelier)
      .travaux(atelier)
      .occupations(atelier)
      .conflits(atelier)
      .clock(() -> {
        lectures.incrementAndGet();
        return LE_11_MAI_A_17H;
      });
    service.rapport(ELEMENT_ID_OF);
    assertThat(lectures).hasValue(1);
  }

  private static CoutsDeRevientService service(AtelierEnMemoire atelier, Instant evaluation) {
    return CoutsDeRevientService.builder()
      .elements(atelier)
      .travaux(atelier)
      .occupations(atelier)
      .conflits(atelier)
      .clock(() -> evaluation);
  }
}
