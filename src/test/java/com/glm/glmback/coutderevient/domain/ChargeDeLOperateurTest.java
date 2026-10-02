package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Le diviseur du taux horaire, sous-periode par sous-periode.
 *
 * <p>
 * La regle vient du verbatim client, enonce deux fois de suite : le cout horaire de chaque machine active court en
 * entier, le taux horaire de l'operateur se divise par le nombre de machines qu'il utilise. Le diviseur compte donc
 * des postes, jamais des elements ni des activites.
 * </p>
 */
@UnitTest
class ChargeDeLOperateurTest {

  @Test
  void shouldHaveNoSousPeriodeWithoutTranche() {
    assertThat(ChargeDeLOperateur.de(List.of()).sousPeriodes()).isEmpty();
  }

  @Test
  void shouldNotDivideASingleTranche() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H)));

    assertThat(charge.sousPeriodes()).containsExactly(new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), new Diviseur(1)));
  }

  @Test
  void shouldDivideByTwoPostesMenesDeFront() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H), surTour(LE_11_MAI_A_9H, LE_11_MAI_A_11H))
    );

    assertThat(charge.sousPeriodes()).containsExactly(new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), new Diviseur(2)));
  }

  /**
   * C'est le cas que le client detaille : un operateur sur trois elements avec une seule machine n'est pas divise.
   * Le diviseur compte des postes, et il n'y en a qu'un.
   */
  @Test
  void shouldNotDivideSeveralElementsOnASinglePoste() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(
        surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H),
        surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H),
        surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H)
      )
    );

    assertThat(charge.sousPeriodes()).containsExactly(new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), new Diviseur(1)));
  }

  /**
   * Seul le chevauchement se divise : les bords, ou l'operateur n'etait que sur une machine, gardent leur heure
   * entiere. C'est ce que le decoupage aux bornes garantit, et ce qu'un diviseur calcule sur l'intervalle entier
   * aurait faux des que deux pointages ne commencent pas ensemble.
   */
  @Test
  void shouldDivideOnlyTheOverlappingPart() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H), surTour(LE_11_MAI_A_10H, LE_11_MAI_A_12H))
    );

    assertThat(charge.sousPeriodes()).containsExactly(
      new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H), new Diviseur(1)),
      new SousPeriode(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H), new Diviseur(2)),
      new SousPeriode(new Periode(LE_11_MAI_A_11H, LE_11_MAI_A_12H), new Diviseur(1))
    );
  }

  @Test
  void shouldLeaveAGapWhereNothingRuns() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_10H), surFraiseuse(LE_11_MAI_A_11H, LE_11_MAI_A_12H))
    );

    assertThat(charge.sousPeriodes()).containsExactly(
      new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H), new Diviseur(1)),
      new SousPeriode(new Periode(LE_11_MAI_A_11H, LE_11_MAI_A_12H), new Diviseur(1))
    );
  }

  /**
   * Une activite sans poste compte pour un poste : une entreprise sans parc machine retrouve un diviseur de un.
   */
  @Test
  void shouldCountAnActiviteWithoutPosteAsOne() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(sansPoste(LE_11_MAI_A_9H, LE_11_MAI_A_11H)));

    assertThat(charge.sousPeriodes()).containsExactly(new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), new Diviseur(1)));
  }

  @Test
  void shouldDivideAPointageSansPosteByThePosteMenesDeFront() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(sansPoste(LE_11_MAI_A_9H, LE_11_MAI_A_11H), surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H))
    );

    assertThat(charge.sousPeriodes()).containsExactly(new SousPeriode(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), new Diviseur(2)));
  }

  @Test
  void shouldSplitATrancheOverSousPeriodes() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H), surTour(LE_11_MAI_A_10H, LE_11_MAI_A_12H))
    );

    List<TrancheValorisable> parts = charge.decoupe(surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H));

    assertThat(parts)
      .extracting(part -> part.diviseur().orElseThrow())
      .containsExactly(new Diviseur(1), new Diviseur(2));
    assertThat(parts)
      .extracting(part -> part.tranche().periode())
      .containsExactly(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H), new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H));
  }

  /**
   * Un second element pointe sur la meme fraiseuse de 9 h 20 a 9 h 40 : les sous-periodes se coupent, mais l'ensemble
   * des postes ne change pas. La fenetre reste entiere, et l'heure de 9 h a 10 h vaut 20,00 EUR, pas 3 x 6,67 EUR.
   */
  @Test
  void shouldNotCutAnUnsharedHourIntoRoundedPieces() {
    TrancheDActivite premierElement = surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_10H);
    TrancheDActivite secondElement = surFraiseuse(Instant.parse("2026-05-11T09:20:00Z"), Instant.parse("2026-05-11T09:40:00Z"));

    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(premierElement, secondElement));

    assertThat(charge.sousPeriodes()).hasSize(3);
    assertThat(charge.decoupe(premierElement))
      .singleElement()
      .satisfies(part -> assertThat(part.coutDeMainDOeuvre()).contains(new Montant(new BigDecimal("20.00"))));
    assertThat(charge.decoupe(secondElement))
      .singleElement()
      .satisfies(part -> assertThat(part.coutDeMainDOeuvre()).contains(new Montant(new BigDecimal("6.67"))));
  }

  @Test
  void shouldOpenANewFenetreWhereThePostesChange() {
    TrancheDActivite fraisage = surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H);

    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(fraisage, surTour(LE_11_MAI_A_10H, LE_11_MAI_A_12H)));

    assertThat(charge.fenetres())
      .extracting(FenetreDePartage::periode)
      .containsExactly(
        new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H),
        new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H),
        new Periode(LE_11_MAI_A_11H, LE_11_MAI_A_12H)
      );
    assertThat(charge.decoupe(fraisage))
      .extracting(TrancheValorisable::coutDeMainDOeuvre)
      .containsExactly(Optional.of(new Montant(new BigDecimal("20.00"))), Optional.of(new Montant(new BigDecimal("10.00"))));
  }

  /**
   * Un tour peut-etre occupe de 10 h a 12 h rend le diviseur inconnu de 10 h a 11 h : cette sous-periode reste seule,
   * sans repartition, et l'heure certaine qui la precede garde son montant.
   */
  @Test
  void shouldKeepAnUncertainSousPeriodeAfterACertainOneAlone() {
    TrancheDActivite fraisage = surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H);
    ZoneIncertaine tour = new ZoneIncertaine(
      activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_10H, Optional.empty())),
      new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_12H)
    );

    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(fraisage), List.of(tour));

    assertThat(charge.decoupe(fraisage))
      .extracting(TrancheValorisable::coutDeMainDOeuvre)
      .containsExactly(Optional.of(new Montant(new BigDecimal("20.00"))), Optional.empty());
  }

  @Test
  void shouldKeepAnUncertainSousPeriodeBeforeACertainOneAlone() {
    TrancheDActivite fraisage = surFraiseuse(LE_11_MAI_A_9H, LE_11_MAI_A_11H);
    ZoneIncertaine tour = new ZoneIncertaine(
      activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_8H, Optional.empty())),
      new Periode(LE_11_MAI_A_8H, LE_11_MAI_A_10H)
    );

    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(fraisage), List.of(tour));

    assertThat(charge.decoupe(fraisage))
      .extracting(TrancheValorisable::coutDeMainDOeuvre)
      .containsExactly(Optional.empty(), Optional.of(new Montant(new BigDecimal("20.00"))));
  }

  private static TrancheDActivite surFraiseuse(Instant debut, Instant fin) {
    return sur(Optional.of(POSTE_ID_FRAISEUSE), debut, fin);
  }

  private static TrancheDActivite surTour(Instant debut, Instant fin) {
    return sur(Optional.of(POSTE_ID_TOUR), debut, fin);
  }

  private static TrancheDActivite sansPoste(Instant debut, Instant fin) {
    return sur(Optional.empty(), debut, fin);
  }

  private static TrancheDActivite sur(Optional<PosteDeTravailId> poste, Instant debut, Instant fin) {
    Activite activite = Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(poste)
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .categorie(CategorieDActivite.TRAVAIL);

    return new TrancheDActivite(activite, new Periode(debut, fin));
  }
}
