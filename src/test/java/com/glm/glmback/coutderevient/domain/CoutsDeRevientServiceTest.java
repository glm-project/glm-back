package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
      .aTravaille(ELEMENT_ID_OF, fraisage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H)))
      .aMeneDeFront(tournage(LE_11_MAI_A_9H, Optional.empty()));
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_10H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.cout().mainDOeuvre()).isEqualTo(new Montant(new BigDecimal("40.00")));
    assertThat(rapport.cout().machine()).isEqualTo(new Montant(new BigDecimal("90.00")));
  }

  @Test
  void shouldDivideWhenTheOtherActivityEnds() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, fraisage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_10H)))
      .aMeneDeFront(tournage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_11H)));
    assertThat(service(atelier, LE_11_MAI_A_11H).rapport(ELEMENT_ID_OF).cout().mainDOeuvre()).isEqualTo(
      new Montant(new BigDecimal("30.00"))
    );
  }

  @Test
  void shouldExcludeEveryCostOfAnActivityBeforeItsDeadline() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, fraisage(LE_11_MAI_A_8H, Optional.empty()));
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_21H.minusSeconds(1)).rapport(ELEMENT_ID_OF);
    assertThat(rapport.lignes()).isEmpty();
    assertThat(rapport.temps()).isEqualTo(TempsPasse.AUCUN);
    assertThat(rapport.cout()).isEqualTo(Cout.AUCUN);
  }

  @Test
  void shouldValoriseThirteenHoursAtTheAutomaticEnd() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, fraisage(LE_11_MAI_A_8H, Optional.empty()));
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_21H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.temps().travail()).isEqualTo(Duration.ofHours(13));
    assertThat(rapport.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("585.00")), new Montant(new BigDecimal("260.00"))));
    assertThat(service(atelier, LE_12_MAI_A_18H).rapport(ELEMENT_ID_OF).temps()).isEqualTo(rapport.temps());
  }

  @Test
  void shouldKeepTheRealEndEvenAfterTheDeadlineAndBeforeEvaluation() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, fraisage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_21H.plusSeconds(7200))));
    assertThat(service(atelier, LE_11_MAI_A_17H).rapport(ELEMENT_ID_OF).temps().travail()).isEqualTo(Duration.ofHours(15));
  }

  @Test
  void shouldPreserveWorkAndAnOpenNonConformity() {
    Activite nc = Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .categorie(CategorieDActivite.NON_CONFORMITE);
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(
        ELEMENT_ID_OF,
        fraisage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H)),
        new ActiviteInterpretee(nc, new Plage(LE_11_MAI_A_12H, Optional.empty()), LE_12_MAI_A_8H.minusSeconds(25200))
      );
    CoutDeRevient rapport = service(atelier, LE_11_MAI_A_21H).rapport(ELEMENT_ID_OF);
    assertThat(rapport.temps()).isEqualTo(new TempsPasse(Duration.ofHours(4), Duration.ZERO));
    assertThat(rapport.cout().mainDOeuvre()).isEqualTo(new Montant(new BigDecimal("80.00")));
  }

  @Test
  void shouldSampleTheClockOnceIncludingAnEmptyRapport() {
    java.util.concurrent.atomic.AtomicInteger lectures = new java.util.concurrent.atomic.AtomicInteger();
    AtelierEnMemoire atelier = new AtelierEnMemoire().connait(ELEMENT_VALORISE_OF);
    CoutsDeRevientService service = CoutsDeRevientService.builder()
      .elements(atelier)
      .travaux(atelier)
      .occupations(atelier)
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
      .clock(() -> evaluation);
  }

  private static ActiviteInterpretee fraisage(Instant debut, Optional<Instant> fin) {
    return interpretee(POSTE_ID_FRAISEUSE, NATURE_FRAISAGE, COUT_HORAIRE_DE_45_EUROS, new Plage(debut, fin));
  }

  private static ActiviteInterpretee tournage(Instant debut, Optional<Instant> fin) {
    return interpretee(POSTE_ID_TOUR, NATURE_TOURNAGE, COUT_HORAIRE_DE_60_EUROS, new Plage(debut, fin));
  }

  private static ActiviteInterpretee interpretee(PosteDeTravailId poste, NatureDOperation nature, CoutHoraire cout, Plage plage) {
    return new ActiviteInterpretee(
      Activite.builder()
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.of(poste))
        .nature(Optional.of(nature))
        .coutHoraire(Optional.of(cout))
        .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
        .categorie(CategorieDActivite.TRAVAIL),
      plage,
      plage.debut().plusSeconds(46800)
    );
  }
}
