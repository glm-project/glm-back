package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class AnnuaireDAtelierTest {

  private final AnnuaireDAtelier annuaire = AnnuaireDAtelier.de(List.of(OPERATEUR_CONNU_DUPONT), List.of(POSTE_CONNU_FRAISEUSE_1));

  @Test
  void shouldNotBuildWithoutOperateurs() {
    Map<PosteDeTravailId, PosteConnu> postes = Map.of();

    assertThatThrownBy(() -> new AnnuaireDAtelier(null, postes))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateurs");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    Map<OperateurId, OperateurConnu> operateurs = Map.of();

    assertThatThrownBy(() -> new AnnuaireDAtelier(operateurs, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldResoudreUnOperateurEtUnPosteConnus() {
    assertThat(annuaire.operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
    assertThat(annuaire.poste(POSTE_ID_FRAISEUSE_1)).contains(POSTE_CONNU_FRAISEUSE_1);
  }

  /**
   * Une ressource absente ne fait pas echouer une lecture : le referentiel refuse de supprimer ce qui a servi a
   * pointer, mais l'affichage d'un journal n'est pas l'endroit ou le verifier.
   */
  @Test
  void shouldRendreVidePourUneRessourceAbsente() {
    assertThat(annuaire.operateur(new OperateurId(UUID.randomUUID()))).isEmpty();
    assertThat(annuaire.poste(new PosteDeTravailId(UUID.randomUUID()))).isEmpty();
  }
}
