package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class JournalDAtelierOrdreMetierTest {

  @Test
  void shouldConserverLOrdreMetierParIdentifiantEtJamaisParReception() {
    var ids = List.of(
      "00000000-0000-0000-0000-000000000000",
      "00000000-0000-0000-8000-000000000000",
      "80000000-0000-0000-0000-000000000000",
      "80000000-0000-0000-8000-000000000000"
    );
    var faits = java.util.stream.IntStream.range(0, ids.size())
      .mapToObj(index ->
        debutIdentifieSurUnPosteDeMemeUuid(new EvenementDAtelierId(UUID.fromString(ids.get(index)))).horodatage(
          new Horodatage(LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_17H.plusSeconds(index))
        )
      )
      .toList();
    var ordreMetier = List.of(ids.get(3), ids.get(2), ids.get(1), ids.get(0));
    var journal = new JournalDAtelier(faits.reversed());

    assertThat(journal.evenements())
      .extracting(fait -> fait.id().uuid().toString())
      .containsExactlyElementsOf(ordreMetier);
    assertThat(journal.activites(Optional.empty()))
      .extracting(activite -> activite.ouvrant().id().uuid().toString())
      .containsExactlyElementsOf(ordreMetier);
    assertThat(new JournalDAtelier(faits).activites(Optional.empty())).isEqualTo(journal.activites(Optional.empty()));
  }

  @Test
  void shouldOrdonnerLesFinsAvantLesTransitionsPuisLesOuverturesSimultanees() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    var transition = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var ouverture = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    var journal = new JournalDAtelier(List.of(ouverture, transition, fin, travail));

    assertThat(journal.evenements()).containsExactly(travail, fin, transition, ouverture);
    assertThat(journal.activites(Optional.empty())).isEqualTo(
      new JournalDAtelier(List.of(travail, fin, transition, ouverture)).activites(Optional.empty())
    );
    assertThat(journal.diagnostics(Optional.empty())).isEqualTo(
      new JournalDAtelier(List.of(transition, travail, ouverture, fin)).diagnostics(Optional.empty())
    );
  }
}
