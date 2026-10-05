package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDesConflits;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDesFinsAutomatiques;
import com.glm.glmback.shared.pagination.domain.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "RestPageDesAnomalies", description = "Une page et son total acquis ensemble dans les projections courantes.")
final class RestPageDesAnomalies {

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final List<RestAnomalieEnListe> lignes;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final long total;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final boolean complete;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final int page;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final int size;

  private RestPageDesAnomalies(List<RestAnomalieEnListe> lignes, Page<?> page) {
    this.lignes = lignes;
    total = page.totalElementsCount();
    complete = true;
    this.page = page.currentPage();
    size = page.pageSize();
  }

  static RestPageDesAnomalies from(LectureDesConflits lecture) {
    return new RestPageDesAnomalies(
      lecture
        .page()
        .content()
        .stream()
        .<RestAnomalieEnListe>map(ligne -> RestConflitEnListe.from(ligne, lecture.annuaire()))
        .toList(),
      lecture.page()
    );
  }

  static RestPageDesAnomalies from(LectureDesFinsAutomatiques lecture) {
    return new RestPageDesAnomalies(
      lecture
        .page()
        .content()
        .stream()
        .<RestAnomalieEnListe>map(ligne -> RestFinAutomatiqueEnListe.from(ligne, lecture.annuaire()))
        .toList(),
      lecture.page()
    );
  }
}
