package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SequenceEnConflitTest {

  private static final EvenementDAtelierId POINTAGE = EvenementDAtelierId.newId();
  private static final ActiviteId ACTIVITE = ActiviteId.ouvertePar(POINTAGE);

  @Test
  void shouldNotBuildWithoutCle() {
    assertThatThrownBy(() -> new SequenceEnConflit(null, List.of(ACTIVITE), List.of(POINTAGE)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cle");
  }

  @Test
  void shouldNotBuildWithoutActivites() {
    CleDActivite cle = cleDeFraiseuse1DeDupont();

    assertThatThrownBy(() -> new SequenceEnConflit(cle, null, List.of(POINTAGE)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activites");
  }

  @Test
  void shouldNotBuildWithNullActivite() {
    CleDActivite cle = cleDeFraiseuse1DeDupont();
    List<ActiviteId> avecNull = Arrays.asList(ACTIVITE, null);

    assertThatThrownBy(() -> new SequenceEnConflit(cle, avecNull, List.of(POINTAGE)))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("activites");
  }

  @Test
  void shouldNotBuildWithoutPointage() {
    CleDActivite cle = cleDeFraiseuse1DeDupont();
    List<EvenementDAtelierId> aucun = List.of();

    assertThatThrownBy(() -> new SequenceEnConflit(cle, List.of(ACTIVITE), aucun))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("pointages");
  }

  /**
   * Un geste qui vise une ouverture annulee ne laisse aucune activite a resoudre : la sequence n'en porte pas moins
   * son pointage contradictoire.
   */
  @Test
  void shouldAdmettreUneSequenceSansActiviteAResoudre() {
    SequenceEnConflit sequence = new SequenceEnConflit(cleDeFraiseuse1DeDupont(), List.of(), List.of(POINTAGE));

    assertThat(sequence.activites()).isEmpty();
    assertThat(sequence.pointages()).containsExactly(POINTAGE);
  }

  @Test
  void shouldLireLOperateurEtLePosteDeSaCle() {
    SequenceEnConflit sequence = new SequenceEnConflit(cleDeFraiseuse1DeDupont(), List.of(ACTIVITE), List.of(POINTAGE));

    assertThat(sequence.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(sequence.poste()).isEqualTo(Optional.of(POSTE_ID_FRAISEUSE_1));
  }

  @Test
  void shouldNePasSuivreLesListesDOrigine() {
    List<ActiviteId> activites = new ArrayList<>(List.of(ACTIVITE));
    SequenceEnConflit sequence = new SequenceEnConflit(cleDeFraiseuse1DeDupont(), activites, List.of(POINTAGE));

    activites.clear();

    assertThat(sequence.activites()).containsExactly(ACTIVITE);
  }
}
