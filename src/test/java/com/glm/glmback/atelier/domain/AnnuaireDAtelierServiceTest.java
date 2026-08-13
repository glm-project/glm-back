package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class AnnuaireDAtelierServiceTest {

  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final AnnuaireDAtelierService annuaires = new AnnuaireDAtelierService(ressources.operateurs(), ressources.postes());

  @Test
  void shouldResoudreLesRessourcesDUnSuivi() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    AnnuaireDAtelier annuaire = annuaires.pour(suivi);

    assertThat(annuaire.operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
    assertThat(annuaire.poste(POSTE_ID_FRAISEUSE_1)).contains(POSTE_CONNU_FRAISEUSE_1);
  }

  @Test
  void shouldResoudreLesRessourcesDUneListeDeSuivis() {
    SuiviDAtelier premier = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    SuiviDAtelier second = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H));

    AnnuaireDAtelier annuaire = annuaires.pourSuivis(List.of(premier, second));

    assertThat(annuaire.operateurs()).containsOnlyKeys(OPERATEUR_ID_DUPONT, OPERATEUR_ID_MARTIN);
    assertThat(annuaire.postes()).containsOnlyKeys(POSTE_ID_FRAISEUSE_1);
  }

  @Test
  void shouldResoudreLesRessourcesDesIntervalles() {
    List<IntervalleDActivite> intervalles = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).activites();

    AnnuaireDAtelier annuaire = annuaires.pourIntervalles(intervalles);

    assertThat(annuaire.operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
    assertThat(annuaire.poste(POSTE_ID_FRAISEUSE_1)).contains(POSTE_CONNU_FRAISEUSE_1);
  }

  /**
   * La presence ne connait aucun poste : seul l'operateur de la journee est a resoudre.
   */
  @Test
  void shouldResoudreLeSeulOperateurDUneJournee() {
    AnnuaireDAtelier annuaire = annuaires.pour(journeeDeDupontOuverteA7H());

    assertThat(annuaire.operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
    assertThat(annuaire.postes()).isEmpty();
  }

  @Test
  void shouldResoudreLesOperateursDUneListeDeJournees() {
    AnnuaireDAtelier annuaire = annuaires.pourJournees(List.of(journeeDeDupontOuverteA7H(), journeeDeDupontDe7HA17HAvecPauseDeMidi()));

    assertThat(annuaire.operateurs()).containsOnlyKeys(OPERATEUR_ID_DUPONT);
  }
}
