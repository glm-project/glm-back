package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Le cout d'un operateur sur une fenetre de partage, arrondi une fois puis reparti au centime (ADR 0004).
 *
 * <p>
 * L'operateur vaut 20 EUR de l'heure. Les instants restent en clair : ce sont eux qui fabriquent les restes.
 * </p>
 */
@UnitTest
class RepartitionDeMainDOeuvreTest {

  private static final Instant A_8H = Instant.parse("2026-05-11T08:00:00Z");
  private static final Instant A_9H = Instant.parse("2026-05-11T09:00:00Z");
  private static final Instant A_9H01 = Instant.parse("2026-05-11T09:01:00Z");
  private static final Instant A_9H20 = Instant.parse("2026-05-11T09:20:00Z");
  private static final Instant A_10H = Instant.parse("2026-05-11T10:00:00Z");
  private static final Instant A_9H_ET_10_SECONDES = Instant.parse("2026-05-11T09:00:10Z");

  @Test
  void shouldNotBuildWithoutParts() {
    assertThatThrownBy(() -> new RepartitionDeMainDOeuvre(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("parts");
  }

  /**
   * Une heure sur trois postes vaut 6,666... EUR par poste : 6,67 + 6,67 + 6,66 = 20,00 EUR, l'heure exacte. Les
   * restes sont egaux et les debuts aussi : l'ordre des postes departage, sans poste en dernier.
   */
  @Test
  void shouldSplitAnHourOnThreePostesToItsExactCost() {
    TrancheDActivite fraisage = sur(Optional.of(POSTE_ID_FRAISEUSE), A_9H, A_10H);
    TrancheDActivite tournage = sur(Optional.of(POSTE_ID_TOUR), A_9H, A_10H);
    TrancheDActivite sansPoste = sur(Optional.empty(), A_9H, A_10H);

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(
      List.of(sansPoste, tournage, fraisage),
      new Periode(A_9H, A_10H),
      new Diviseur(3)
    );

    assertThat(repartition.de(fraisage)).isEqualTo(euros("6.67"));
    assertThat(repartition.de(tournage)).isEqualTo(euros("6.67"));
    assertThat(repartition.de(sansPoste)).isEqualTo(euros("6.66"));
  }

  /**
   * Une minute sur deux postes vaut 0,1666... EUR par poste. Le centime restant va au tour, commence a 8 h, avant la
   * fraiseuse commencee a 9 h : le debut de l'activite passe avant l'ordre des postes.
   */
  @Test
  void shouldGiveTheRemainingCentToTheActiviteStartedFirst() {
    TrancheDActivite tournage = sur(Optional.of(POSTE_ID_TOUR), A_8H, A_9H01);
    TrancheDActivite fraisage = sur(Optional.of(POSTE_ID_FRAISEUSE), A_9H, A_10H);
    Periode fenetre = new Periode(A_9H, A_9H01);

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(List.of(fraisage, tournage), fenetre, new Diviseur(2));

    assertThat(repartition.de(tournage.reduiteA(fenetre).orElseThrow())).isEqualTo(euros("0.17"));
    assertThat(repartition.de(fraisage.reduiteA(fenetre).orElseThrow())).isEqualTo(euros("0.16"));
  }

  /**
   * Le tour vaut 10,00 EUR ; sur la fraiseuse, 3,333... EUR puis 6,666... EUR. Le centime va au plus fort reste,
   * la seconde fraiseuse, bien que la premiere ait commence avant elle.
   */
  @Test
  void shouldGiveTheRemainingCentToTheLargestRemainderFirst() {
    TrancheDActivite tournage = sur(Optional.of(POSTE_ID_TOUR), A_9H, A_10H);
    TrancheDActivite premierFraisage = sur(Optional.of(POSTE_ID_FRAISEUSE), A_9H, A_9H20);
    TrancheDActivite secondFraisage = sur(Optional.of(POSTE_ID_FRAISEUSE), A_9H20, A_10H);

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(
      List.of(tournage, premierFraisage, secondFraisage),
      new Periode(A_9H, A_10H),
      new Diviseur(2)
    );

    assertThat(repartition.de(tournage)).isEqualTo(euros("10.00"));
    assertThat(repartition.de(premierFraisage)).isEqualTo(euros("3.33"));
    assertThat(repartition.de(secondFraisage)).isEqualTo(euros("6.67"));
  }

  /**
   * Deux elements sur le meme poste au meme moment ne font qu'une part : chacun la paie entiere, au meme montant.
   */
  @Test
  void shouldAttribuerLaMemePartDePosteAuxElementsDistincts() {
    var postePrioritaire = new PosteDeTravailId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    var posteCommun = new PosteDeTravailId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    var periode = new Periode(A_9H, A_9H.plusSeconds(2700));
    var a = partATrenteCinqEuros(ELEMENT_ID_OF, posteCommun, periode);
    var aPrime = partATrenteCinqEuros(ELEMENT_ID_OF_2, posteCommun, periode);
    var b = partATrenteCinqEuros(ELEMENT_ID_OF, postePrioritaire, periode);

    var repartition = RepartitionDeMainDOeuvre.de(List.of(a, aPrime, b), periode, new Diviseur(2));

    assertThat(repartition.de(a)).isEqualTo(euros("13.12"));
    assertThat(repartition.de(aPrime)).isEqualTo(euros("13.12"));
    assertThat(repartition.de(b)).isEqualTo(euros("13.13"));
  }

  @Test
  void shouldGiveTheSameAmountToIdenticalParts() {
    TrancheDActivite premierElement = sur(Optional.of(POSTE_ID_FRAISEUSE), A_9H, A_9H_ET_10_SECONDES);
    TrancheDActivite secondElement = sur(Optional.of(POSTE_ID_FRAISEUSE), A_9H, A_9H_ET_10_SECONDES);

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(
      List.of(premierElement, secondElement),
      new Periode(A_9H, A_9H_ET_10_SECONDES),
      new Diviseur(1)
    );

    assertThat(repartition.parts()).hasSize(1);
    assertThat(repartition.de(secondElement)).isEqualTo(euros("0.06"));
  }

  /**
   * La fin automatique est une anomalie de l'activite, sans nouveau poste ni nouveau tarif : les deux parts
   * appartiennent au meme partage humain.
   */
  @Test
  void shouldConserverLaMemePartMalgreUneFinAutomatique() {
    Periode periode = new Periode(A_9H, A_9H_ET_10_SECONDES);
    TrancheDActivite finReelle = new TrancheDActivite(
      activite(Optional.of(POSTE_ID_FRAISEUSE), Optional.of(TAUX_HORAIRE_DE_20_EUROS)),
      periode,
      false
    );
    TrancheDActivite finAutomatique = new TrancheDActivite(
      activite(Optional.of(POSTE_ID_FRAISEUSE), Optional.of(TAUX_HORAIRE_DE_20_EUROS)),
      periode,
      true
    );

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(List.of(finAutomatique, finReelle), periode, new Diviseur(1));

    assertThat(repartition.de(finReelle)).isEqualTo(euros("0.06"));
    assertThat(repartition.de(finAutomatique)).isEqualTo(euros("0.06"));
  }

  @Test
  void shouldLeaveAPartWithoutTauxOutOfTheRepartition() {
    TrancheDActivite sansTaux = new TrancheDActivite(activite(Optional.of(POSTE_ID_TOUR), Optional.empty()), new Periode(A_9H, A_10H));

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(List.of(sansTaux), new Periode(A_9H, A_10H), new Diviseur(1));

    assertThat(repartition.de(sansTaux)).isEqualTo(Montant.ZERO);
  }

  @Test
  void shouldIgnoreATrancheOutsideTheFenetre() {
    TrancheDActivite avant = sur(Optional.of(POSTE_ID_FRAISEUSE), A_8H, A_9H);

    RepartitionDeMainDOeuvre repartition = RepartitionDeMainDOeuvre.de(List.of(avant), new Periode(A_9H, A_10H), new Diviseur(1));

    assertThat(repartition.parts()).isEmpty();
  }

  private static TrancheDActivite partATrenteCinqEuros(ElementId element, PosteDeTravailId poste, Periode periode) {
    var activite = Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .element(element)
      .poste(Optional.of(poste))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(new TauxHoraire(new BigDecimal("35.00"))))
      .categorie(CategorieDActivite.TRAVAIL);
    return new TrancheDActivite(activite, periode);
  }

  private static Montant euros(String montant) {
    return new Montant(new BigDecimal(montant));
  }

  private static TrancheDActivite sur(Optional<PosteDeTravailId> poste, Instant debut, Instant fin) {
    return new TrancheDActivite(activite(poste, Optional.of(TAUX_HORAIRE_DE_20_EUROS)), new Periode(debut, fin));
  }

  private static Activite activite(Optional<PosteDeTravailId> poste, Optional<TauxHoraire> taux) {
    return Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .element(ELEMENT_ID_OF)
      .poste(poste)
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(taux)
      .categorie(CategorieDActivite.TRAVAIL);
  }
}
