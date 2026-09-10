package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ChargesDesOperateursTest {

  /**
   * Deux personnes ne se divisent pas l'une l'autre : le parallelisme se compte par operateur, jamais sur l'atelier
   * entier.
   */
  @Test
  void shouldNotDivideOneOperateurByAnother() {
    ChargesDesOperateurs charges = ChargesDesOperateurs.de(
      List.of(
        surFraiseuse(OPERATEUR_ID_DUPONT, LE_11_MAI_A_9H, LE_11_MAI_A_11H),
        surTour(OPERATEUR_ID_MARTIN, LE_11_MAI_A_9H, LE_11_MAI_A_11H)
      )
    );

    List<TrancheValorisable> parts = charges.decoupe(surFraiseuse(OPERATEUR_ID_DUPONT, LE_11_MAI_A_9H, LE_11_MAI_A_11H));

    assertThat(parts).extracting(TrancheValorisable::diviseur).containsExactly(new Diviseur(1));
  }

  @Test
  void shouldDivideByThePostesOfItsOwnOperateur() {
    ChargesDesOperateurs charges = ChargesDesOperateurs.de(
      List.of(
        surFraiseuse(OPERATEUR_ID_DUPONT, LE_11_MAI_A_9H, LE_11_MAI_A_11H),
        surTour(OPERATEUR_ID_DUPONT, LE_11_MAI_A_9H, LE_11_MAI_A_11H)
      )
    );

    List<TrancheValorisable> parts = charges.decoupe(surFraiseuse(OPERATEUR_ID_DUPONT, LE_11_MAI_A_9H, LE_11_MAI_A_11H));

    assertThat(parts).extracting(TrancheValorisable::diviseur).containsExactly(new Diviseur(2));
  }

  @Test
  void shouldIgnoreATrancheOfAnUnknownOperateur() {
    ChargesDesOperateurs charges = ChargesDesOperateurs.de(List.of(surFraiseuse(OPERATEUR_ID_DUPONT, LE_11_MAI_A_9H, LE_11_MAI_A_11H)));

    assertThat(charges.decoupe(surTour(OPERATEUR_ID_MARTIN, LE_11_MAI_A_9H, LE_11_MAI_A_11H))).isEmpty();
  }

  private static TrancheDActivite surFraiseuse(OperateurId operateur, Instant debut, Instant fin) {
    return sur(operateur, Optional.of(POSTE_ID_FRAISEUSE), debut, fin);
  }

  private static TrancheDActivite surTour(OperateurId operateur, Instant debut, Instant fin) {
    return sur(operateur, Optional.of(POSTE_ID_TOUR), debut, fin);
  }

  private static TrancheDActivite sur(OperateurId operateur, Optional<PosteDeTravailId> poste, Instant debut, Instant fin) {
    Activite activite = Activite.builder()
      .operateur(operateur)
      .poste(poste)
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .categorie(CategorieDActivite.TRAVAIL);

    return new TrancheDActivite(activite, new Periode(debut, fin));
  }
}
