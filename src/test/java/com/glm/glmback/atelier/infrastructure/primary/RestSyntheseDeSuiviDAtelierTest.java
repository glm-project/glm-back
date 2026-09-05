package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.shared.pagination.domain.Page;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@UnitTest
class RestSyntheseDeSuiviDAtelierTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @ParameterizedTest
  @MethodSource("suivis")
  void shouldKeepEveryDetailFieldExceptJournal(SuiviDAtelier suivi) {
    AnnuaireDAtelier annuaire = annuaireDeDupontEtMartin();
    ObjectNode detail = (ObjectNode) JSON.valueToTree(RestSuiviDAtelier.from(suivi, annuaire));
    assertThat(detail.has("journal")).isTrue();
    detail.remove("journal");

    ObjectNode grille = (ObjectNode) JSON.valueToTree(RestSyntheseDeSuiviDAtelier.from(suivi, annuaire));
    assertThat(grille).isEqualTo(detail);
  }

  static Stream<SuiviDAtelier> suivis() {
    return suivisDAtelierEnAttenteEnCoursInterrompuEtCloture().stream();
  }

  @Test
  void shouldReducePagePayloadByAtLeastOneOrderOfMagnitude() {
    List<SuiviDAtelier> suivis = IntStream.range(0, 100)
      .mapToObj(index -> suiviDAtelierAvecCentEvenements())
      .toList();
    AnnuaireDAtelier annuaire = annuaireDeDupontEtMartin();
    var detail = Page.<RestSuiviDAtelier>builder()
      .content(
        suivis
          .stream()
          .map(suivi -> RestSuiviDAtelier.from(suivi, annuaire))
          .toList()
      )
      .currentPage(0)
      .pageSize(100)
      .totalElementsCount(100);
    var grille = Page.<RestSyntheseDeSuiviDAtelier>builder()
      .content(
        suivis
          .stream()
          .map(suivi -> RestSyntheseDeSuiviDAtelier.from(suivi, annuaire))
          .toList()
      )
      .currentPage(0)
      .pageSize(100)
      .totalElementsCount(100);
    int detailBytes = JSON.writeValueAsBytes(detail).length;
    int grilleBytes = JSON.writeValueAsBytes(grille).length;

    assertThat(grilleBytes).isLessThan(detailBytes / 10);
    System.out.println("Page de 100 suivis : " + detailBytes + " octets avec journal, " + grilleBytes + " octets en grille.");
  }
}
