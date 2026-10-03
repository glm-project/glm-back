package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RestEvenementDAtelierTest {

  @Test
  void shouldRendreLeLienDeCorrectionEtLAbsenceDeLienHistorique() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier remplacant = finDe(debut).a(LE_10_MAI_2026_A_12H.plusSeconds(1800));
    JournalDAtelier corrige = new JournalDAtelier(List.of(debut, fin)).corrige(fin.id(), annulationParLeroy(), remplacant);

    RestEvenementDAtelier rendu = RestEvenementDAtelier.from(corrige.evenement(remplacant.id()).orElseThrow(), annuaireDeDupontEtMartin());

    assertThat(rendu.remplace()).isEqualTo(fin.id().uuid());
    assertThat(RestEvenementDAtelier.from(debut, annuaireDeDupontEtMartin()).remplace()).isNull();
  }
}
