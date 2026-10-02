package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * L'agregation par nature d'operation, et la valorisation de chaque ligne.
 *
 * <p>
 * Les montants des scenarios se verifient a la main : le poste vaut 45 EUR de l'heure, l'operateur 20 EUR.
 * </p>
 */
@UnitTest
class CoutDeRevientTest {

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() -> new CoutDeRevient(null, List.of(), new EvaluationDuCout(LE_11_MAI_A_17H, 0), List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutLignes() {
    assertThatThrownBy(() -> new CoutDeRevient(ELEMENT_VALORISE_OF, null, new EvaluationDuCout(LE_11_MAI_A_17H, 0), List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("lignes");
  }

  @Test
  void shouldHaveNoLigneWithoutTranche() {
    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(List.of())
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(List.of()))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes()).isEmpty();
    assertThat(rapport.temps()).isEqualTo(TempsPasse.AUCUN);
    assertThat(rapport.cout()).isEqualTo(Cout.AUCUN);
  }

  /**
   * Deux heures sur une fraiseuse, seul : la machine coute 90 EUR, l'operateur 40 EUR.
   */
  @Test
  void shouldValoriseASingleTranche() {
    List<TrancheDActivite> tranches = List.of(fraisage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H));

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.nature()).contains(NATURE_FRAISAGE);
        assertThat(ligne.temps()).isEqualTo(new TempsPasse(Duration.ofHours(2), Duration.ZERO));
        assertThat(ligne.periode()).isEqualTo(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_11H)));
        assertThat(ligne.cout().machine().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("90.00")));
        assertThat(ligne.cout().mainDOeuvre().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("40.00")));
      });
    assertThat(rapport.cout().total().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("130.00")));
  }

  /**
   * Le temps en non conformite se compte a part sur la meme ligne, et ses periodes sont datees : c'est ce que le
   * client veut lire a la cloture.
   */
  @Test
  void shouldCountNonConformiteApart() {
    List<TrancheDActivite> tranches = List.of(
      fraisage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_10H),
      fraisage(CategorieDActivite.NON_CONFORMITE, LE_11_MAI_A_10H, LE_11_MAI_A_11H)
    );

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.temps()).isEqualTo(new TempsPasse(Duration.ofHours(1), Duration.ofHours(1)));
        assertThat(ligne.nonConformites()).containsExactly(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H));
        assertThat(ligne.periode()).isEqualTo(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_11H)));
      });
  }

  @Test
  void shouldProduceOneLigneParNature() {
    List<TrancheDActivite> tranches = List.of(
      tournage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H),
      fraisage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H)
    );

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes())
      .extracting(LigneDeCout::nature)
      .containsExactly(Optional.of(NATURE_FRAISAGE), Optional.of(NATURE_TOURNAGE));
  }

  /**
   * Deux machines menees de front : chacune coute son heure entiere, mais l'operateur n'est paye qu'une fois. La ligne
   * de fraisage porte donc 90 EUR de machine et seulement 20 EUR de main d'oeuvre.
   */
  @Test
  void shouldDivideOnlyMainDOeuvreWhenTwoPostesRunTogether() {
    List<TrancheDActivite> tranches = List.of(
      fraisage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H),
      tournage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H)
    );

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes().getFirst().cout()).isEqualTo(
      new Cout(new Montant(new BigDecimal("90.00")), new Montant(new BigDecimal("20.00")))
    );
    assertThat(rapport.cout().mainDOeuvre().valeur().orElseThrow()).isEqualTo(new Montant(new BigDecimal("40.00")));
  }

  /**
   * Deux pointages de dix secondes a 45 EUR de l'heure valent chacun 0,125 EUR de machine. Chacun s'arrondit a
   * 0,13 EUR, et la ligne additionne ce qu'elle affichera pointage par pointage : 0,26 EUR.
   */
  @Test
  void shouldRoundTheMachineOncePerTranche() {
    List<TrancheDActivite> tranches = List.of(
      fraisage(CategorieDActivite.TRAVAIL, Instant.parse("2026-05-11T09:00:00Z"), Instant.parse("2026-05-11T09:00:10Z")),
      fraisage(CategorieDActivite.TRAVAIL, Instant.parse("2026-05-11T10:00:00Z"), Instant.parse("2026-05-11T10:00:10Z"))
    );

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes().getFirst().cout().machine().valeur()).contains(new Montant(new BigDecimal("0.26")));
  }

  /**
   * Sans poste, il n'y a ni nature ni cout machine : c'est le comportement nominal d'une entreprise sans parc
   * machine, pas un cas degrade. La ligne sans nature passe en dernier.
   */
  @Test
  void shouldPutTheLigneSansNatureLast() {
    List<TrancheDActivite> tranches = List.of(
      sansPoste(LE_11_MAI_A_9H, LE_11_MAI_A_11H),
      fraisage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H)
    );

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.lignes()).extracting(LigneDeCout::nature).containsExactly(Optional.of(NATURE_FRAISAGE), Optional.empty());
    assertThat(rapport.lignes().getLast().cout().machine().valeur().orElseThrow()).isEqualTo(Montant.ZERO);
  }

  @Test
  void shouldSumTempsOfEveryLigne() {
    List<TrancheDActivite> tranches = List.of(
      fraisage(CategorieDActivite.TRAVAIL, LE_11_MAI_A_9H, LE_11_MAI_A_11H),
      tournage(CategorieDActivite.NON_CONFORMITE, LE_11_MAI_A_11H, LE_11_MAI_A_12H)
    );

    CoutDeRevient rapport = CoutDeRevient.builder()
      .element(ELEMENT_VALORISE_OF)
      .tranches(tranches)
      .aResoudre(List.of())
      .charges(ChargesDesOperateurs.de(tranches))
      .lecture(new EvaluationDuCout(LE_11_MAI_A_17H, 0))
      .conflits(List.of());

    assertThat(rapport.temps()).isEqualTo(new TempsPasse(Duration.ofHours(2), Duration.ofHours(1)));
  }

  private static TrancheDActivite fraisage(CategorieDActivite categorie, Instant debut, Instant fin) {
    return tranche(Optional.of(POSTE_ID_FRAISEUSE), Optional.of(NATURE_FRAISAGE), categorie, debut, fin);
  }

  private static TrancheDActivite tournage(CategorieDActivite categorie, Instant debut, Instant fin) {
    return tranche(Optional.of(POSTE_ID_TOUR), Optional.of(NATURE_TOURNAGE), categorie, debut, fin);
  }

  private static TrancheDActivite sansPoste(Instant debut, Instant fin) {
    return tranche(Optional.empty(), Optional.empty(), CategorieDActivite.TRAVAIL, debut, fin);
  }

  private static TrancheDActivite tranche(
    Optional<PosteDeTravailId> poste,
    Optional<NatureDOperation> nature,
    CategorieDActivite categorie,
    Instant debut,
    Instant fin
  ) {
    Activite activite = Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(poste)
      .nature(nature)
      .coutHoraire(poste.map(ignore -> COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .categorie(categorie);

    return new TrancheDActivite(activite, new Periode(debut, fin));
  }
}
