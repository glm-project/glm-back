package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurDuPupitreTest {

  @Test
  void shouldNotBuildWithoutPostes() {
    assertThatThrownBy(() ->
      new OperateurDuPupitre(OPERATEUR_ID_DUPONT, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), EtatDePresence.ABSENT, null)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldNotBuildWithoutEtatDePresence() {
    assertThatThrownBy(() -> new OperateurDuPupitre(OPERATEUR_ID_DUPONT, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("etat de presence");
  }

  @Test
  void shouldBuildDepuisLaPersistance() {
    OperateurDuPupitre operateur = OperateurDuPupitre.builder()
      .id(OPERATEUR_ID_DUPONT)
      .nom(new Nom("Dupont"))
      .prenom(new Prenom("Jean"))
      .matricule("049")
      .etat(EtatDePresence.PRESENT)
      .postes(List.of(POSTE_HABILITE_FRAISEUSE_1));

    assertThat(operateur.id()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(operateur.nom()).isEqualTo(NOM_DUPONT);
    assertThat(operateur.prenom()).isEqualTo(PRENOM_JEAN);
    assertThat(operateur.matricule()).contains(MATRICULE_049);
    assertThat(operateur.postes()).containsExactly(POSTE_HABILITE_FRAISEUSE_1);
  }

  /**
   * L'etat de presence entre au referentiel avec l'operateur : sans lui, le pupitre offrirait ses trois commandes de
   * presence en aveugle, et ne decouvrirait qu'a la reconnexion que le serveur a refuse la transition impossible.
   */
  @Test
  void shouldPorterSonEtatDePresence() {
    OperateurDuPupitre operateur = OperateurDuPupitre.builder()
      .id(OPERATEUR_ID_DUPONT)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .matricule(MATRICULE_049.value())
      .etat(EtatDePresence.EN_PAUSE)
      .postes(List.of(POSTE_HABILITE_FRAISEUSE_1));

    assertThat(operateur.etat()).isEqualTo(EtatDePresence.EN_PAUSE);
  }

  /**
   * Un operateur sans matricule n'est simplement pas designable au pupitre : le referentiel le rend quand meme, il
   * apparaitra des que l'entreprise lui en donnera un.
   */
  @Test
  void shouldBuildSansMatricule() {
    OperateurDuPupitre operateur = OperateurDuPupitre.builder()
      .id(OPERATEUR_ID_MARTIN)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .matricule(null)
      .etat(EtatDePresence.ABSENT)
      .postes(List.of());

    assertThat(operateur.matricule()).isEmpty();
    assertThat(operateur.postes()).isEmpty();
  }
}
