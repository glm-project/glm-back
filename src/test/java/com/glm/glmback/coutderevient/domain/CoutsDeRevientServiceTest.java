package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static com.glm.glmback.coutderevient.domain.TypeDEvenementDAtelier.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Le rapport de bout en bout, des journaux de l'atelier aux lignes valorisees.
 *
 * <p>
 * Les montants se verifient a la main : la fraiseuse vaut 45 EUR de l'heure, le tour 60 EUR, l'operateur 20 EUR.
 * </p>
 */
@UnitTest
class CoutsDeRevientServiceTest {

  private static final Instant MAINTENANT = LE_11_MAI_A_17H;

  @Test
  void shouldRefuseAnUnknownElement() {
    CoutsDeRevientService service = service(new AtelierEnMemoire());

    assertThatThrownBy(() -> service.rapport(ELEMENT_ID_OF))
      .isExactlyInstanceOf(ElementInconnuException.class)
      .hasMessageContaining(ELEMENT_ID_OF.uuid().toString());
  }

  /**
   * Un element connu mais jamais engage rend un rapport vide, pas une erreur : rien n'a ete fait dessus, et c'est une
   * reponse.
   */
  @Test
  void shouldRenderAnEmptyRapportForAnElementNeverEngaged() {
    CoutDeRevient rapport = service(new AtelierEnMemoire().connait(ELEMENT_VALORISE_OF)).rapport(ELEMENT_ID_OF);

    assertThat(rapport.element()).isEqualTo(ELEMENT_VALORISE_OF);
    assertThat(rapport.lignes()).isEmpty();
    assertThat(rapport.cout()).isEqualTo(Cout.AUCUN);
  }

  @Test
  void shouldValoriseTwoHoursOfFraisage() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_9H), fraisage(FIN, LE_11_MAI_A_11H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.lignes())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.nature()).contains(NATURE_FRAISAGE);
        assertThat(ligne.temps().travail()).isEqualTo(Duration.ofHours(2));
        assertThat(ligne.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("90.00")), new Montant(new BigDecimal("40.00"))));
      });
  }

  /**
   * La pause de midi scinde le travail : quatre heures pointees d'affilee n'en font que trois de presence effective.
   */
  @Test
  void shouldReduceToFenetresDePresence() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_10H), fraisage(FIN, LE_11_MAI_A_14H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.temps().travail()).isEqualTo(Duration.ofHours(3));
  }

  /**
   * Un travail jamais arrete est ferme au depart de l'operateur, pas a l'instant present : c'est la journee qui le
   * borne.
   */
  @Test
  void shouldCloseAnOpenIntervalleAtDepart() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_14H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.temps().travail()).isEqualTo(Duration.ofHours(3));
  }

  /**
   * Sans aucune presence connue, l'intervalle est rendu intact puis arrete a l'horloge : l'anomalie reste visible
   * plutot que masquee derriere un temps ampute.
   */
  @Test
  void shouldCloseAnOpenIntervalleAtTheClockWithoutPresence() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_14H)));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.temps().travail()).isEqualTo(Duration.ofHours(3));
  }

  @Test
  void shouldCountNonConformiteApart() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(
        ELEMENT_ID_OF,
        suivi(fraisage(DEBUT, LE_11_MAI_A_9H), fraisage(NON_CONFORMITE, LE_11_MAI_A_10H), fraisage(FIN, LE_11_MAI_A_11H))
      )
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.temps()).isEqualTo(new TempsPasse(Duration.ofHours(1), Duration.ofHours(1)));
    assertThat(rapport.lignes().getFirst().nonConformites()).containsExactly(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H));
  }

  /**
   * Le cas que le client detaille : deux machines menees de front. Chacune coute son heure entiere, l'operateur n'est
   * paye qu'une fois — 45 + 60 EUR de machines pour 20 EUR de main d'oeuvre sur l'heure commune.
   */
  @Test
  void shouldDivideOnlyMainDOeuvreBetweenTwoPostes() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(
        ELEMENT_ID_OF,
        suivi(
          fraisage(DEBUT, LE_11_MAI_A_9H),
          tournage(DEBUT, LE_11_MAI_A_9H),
          fraisage(FIN, LE_11_MAI_A_10H),
          tournage(FIN, LE_11_MAI_A_10H)
        )
      )
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("105.00")), new Montant(new BigDecimal("20.00"))));
  }

  /**
   * Le diviseur se lit sur tout ce que l'operateur menait de front, y compris sur un autre element : c'est ce qui
   * fait du temps reparti une projection, et non un fait du journal.
   */
  @Test
  void shouldDivideByAPosteMenesDeFrontOnAnotherElement() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_9H), fraisage(FIN, LE_11_MAI_A_10H)))
      .aMeneDeFront(suivi(tournage(DEBUT, LE_11_MAI_A_9H), tournage(FIN, LE_11_MAI_A_10H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("45.00")), new Montant(new BigDecimal("10.00"))));
  }

  /**
   * Le meme poste sur deux elements ne divise rien : le diviseur compte des machines, pas des elements.
   */
  @Test
  void shouldNotDivideBySamePosteOnAnotherElement() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_9H), fraisage(FIN, LE_11_MAI_A_10H)))
      .aMeneDeFront(suivi(fraisage(DEBUT, LE_11_MAI_A_9H), fraisage(FIN, LE_11_MAI_A_10H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.cout().mainDOeuvre()).isEqualTo(new Montant(new BigDecimal("20.00")));
  }

  @Test
  void shouldAskOccupationUpToTheEndOfTheElement() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_9H), fraisage(FIN, LE_11_MAI_A_10H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(atelier.borneDemandee()).isEqualTo(LE_11_MAI_A_10H);
  }

  /**
   * Un pointage sans poste n'a ni nature ni cout machine, et son operateur reste paye : c'est le comportement nominal
   * d'une entreprise sans parc machine.
   */
  @Test
  void shouldValoriseAPointageSansPoste() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(sansPoste(DEBUT, LE_11_MAI_A_9H), sansPoste(FIN, LE_11_MAI_A_10H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.lignes())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.nature()).isEmpty();
        assertThat(ligne.cout()).isEqualTo(new Cout(Montant.ZERO, new Montant(new BigDecimal("20.00"))));
      });
  }

  /**
   * Un operateur sans taux horaire ne coute rien en main d'oeuvre : le journal n'avait rien a figer, et le rapport ne
   * l'invente pas.
   */
  @Test
  void shouldValoriseNothingWithoutTauxHoraire() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, suivi(sansTaux(DEBUT, LE_11_MAI_A_9H), sansTaux(FIN, LE_11_MAI_A_10H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("45.00")), Montant.ZERO));
  }

  /**
   * Un element reengage apres cloture additionne ses deux passages, et la cloture referme ce que personne n'a arrete.
   */
  @Test
  void shouldSumEverySuiviOfTheElement() {
    AtelierEnMemoire atelier = new AtelierEnMemoire()
      .connait(ELEMENT_VALORISE_OF)
      .aTravaille(ELEMENT_ID_OF, clos(LE_11_MAI_A_10H, fraisage(DEBUT, LE_11_MAI_A_9H)))
      .aTravaille(ELEMENT_ID_OF, suivi(fraisage(DEBUT, LE_11_MAI_A_13H), fraisage(FIN, LE_11_MAI_A_14H)))
      .aEtePresent(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())));

    CoutDeRevient rapport = service(atelier).rapport(ELEMENT_ID_OF);

    assertThat(rapport.temps().travail()).isEqualTo(Duration.ofHours(2));
  }

  private static CoutsDeRevientService service(AtelierEnMemoire atelier) {
    return CoutsDeRevientService.builder()
      .elements(atelier)
      .travaux(atelier)
      .occupations(atelier)
      .presences(atelier)
      .clock(() -> MAINTENANT);
  }

  private static SuiviDuTravail suivi(EvenementDAtelier... evenements) {
    return new SuiviDuTravail(new JournalDAtelier(List.of(evenements)), Optional.empty());
  }

  private static SuiviDuTravail clos(Instant cloture, EvenementDAtelier... evenements) {
    return new SuiviDuTravail(new JournalDAtelier(List.of(evenements)), Optional.of(cloture));
  }

  private static EvenementDAtelier fraisage(TypeDEvenementDAtelier type, Instant date) {
    return evenement(type, Optional.of(POSTE_ID_FRAISEUSE), Optional.of(NATURE_FRAISAGE), Optional.of(COUT_HORAIRE_DE_45_EUROS), date);
  }

  private static EvenementDAtelier tournage(TypeDEvenementDAtelier type, Instant date) {
    return evenement(type, Optional.of(POSTE_ID_TOUR), Optional.of(NATURE_TOURNAGE), Optional.of(COUT_HORAIRE_DE_60_EUROS), date);
  }

  private static EvenementDAtelier sansPoste(TypeDEvenementDAtelier type, Instant date) {
    return evenement(type, Optional.empty(), Optional.empty(), Optional.empty(), date);
  }

  private static EvenementDAtelier sansTaux(TypeDEvenementDAtelier type, Instant date) {
    return EvenementDAtelier.builder()
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.empty())
      .dateDeSurvenue(date);
  }

  private static EvenementDAtelier evenement(
    TypeDEvenementDAtelier type,
    Optional<PosteDeTravailId> poste,
    Optional<NatureDOperation> nature,
    Optional<CoutHoraire> coutHoraire,
    Instant date
  ) {
    return EvenementDAtelier.builder()
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(poste)
      .nature(nature)
      .coutHoraire(coutHoraire)
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .dateDeSurvenue(date);
  }
}
