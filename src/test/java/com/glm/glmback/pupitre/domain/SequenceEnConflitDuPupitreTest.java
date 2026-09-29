package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class SequenceEnConflitDuPupitreTest {

  @Test
  void shouldPorterLaCleEtLesIdentitesSansInterpreterLeConflit() {
    SequenceEnConflitDuPupitre conflit = SEQUENCE_DUPONT_SUR_FRAISEUSE_1;
    assertThat(conflit.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(conflit.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(conflit.activites()).containsExactly(ACTIVITE_ID_88888888);
    assertThat(conflit.pointages()).containsExactly(POINTAGE_ID_99999999);
  }

  @Test
  void shouldRefuserUneCleManquante() {
    assertThatThrownBy(() -> new SequenceEnConflitDuPupitre(null, List.of(), List.of(POINTAGE_ID_99999999))).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldRefuserLesCollectionsManquantesEtLeursTrous() {
    assertThatThrownBy(() ->
      new SequenceEnConflitDuPupitre(ACTIVITE_DUPONT_SANS_POSTE, null, List.of(POINTAGE_ID_99999999))
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new SequenceEnConflitDuPupitre(ACTIVITE_DUPONT_SANS_POSTE, Arrays.asList(ACTIVITE_ID_88888888, null), List.of(POINTAGE_ID_99999999))
    ).isExactlyInstanceOf(NullElementInCollectionException.class);
    assertThatThrownBy(() -> new SequenceEnConflitDuPupitre(ACTIVITE_DUPONT_SANS_POSTE, List.of(), null)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() ->
      new SequenceEnConflitDuPupitre(ACTIVITE_DUPONT_SANS_POSTE, List.of(), Arrays.asList(POINTAGE_ID_99999999, null))
    ).isExactlyInstanceOf(NullElementInCollectionException.class);
  }

  @Test
  void shouldRefuserUnConflitSansPointage() {
    assertThatThrownBy(() -> new SequenceEnConflitDuPupitre(ACTIVITE_DUPONT_SANS_POSTE, List.of(), List.of())).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldPorterUnConflitSansActiviteEtSansPoste() {
    SequenceEnConflitDuPupitre conflit = new SequenceEnConflitDuPupitre(
      ACTIVITE_DUPONT_SANS_POSTE,
      List.of(),
      List.of(POINTAGE_ID_99999999)
    );
    assertThat(conflit.activites()).isEmpty();
    assertThat(conflit.poste()).isEmpty();
  }

  @Test
  void shouldCopierLesIdentitesEtLesGarderImmuables() {
    List<ActiviteId> activites = new ArrayList<>(List.of(ACTIVITE_ID_88888888));
    List<PointageId> pointages = new ArrayList<>(List.of(POINTAGE_ID_99999999));
    SequenceEnConflitDuPupitre conflit = new SequenceEnConflitDuPupitre(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, activites, pointages);
    activites.clear();
    pointages.clear();
    assertThat(conflit.activites()).containsExactly(ACTIVITE_ID_88888888);
    assertThat(conflit.pointages()).containsExactly(POINTAGE_ID_99999999);
    assertThatThrownBy(() -> conflit.activites().clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> conflit.pointages().clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }
}
