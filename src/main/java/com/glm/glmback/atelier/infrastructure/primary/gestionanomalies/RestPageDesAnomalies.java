package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDesConflits;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "RestPageDesAnomalies", description = "Une page et son total acquis ensemble dans les projections courantes.")
final class RestPageDesAnomalies {

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final List<RestConflitEnListe> lignes;

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

  private RestPageDesAnomalies(LectureDesConflits lecture) {
    lignes = lecture
      .page()
      .content()
      .stream()
      .map(ligne -> RestConflitEnListe.from(ligne, lecture.annuaire()))
      .toList();
    total = lecture.page().totalElementsCount();
    complete = true;
    page = lecture.page().currentPage();
    size = lecture.page().pageSize();
  }

  static RestPageDesAnomalies from(LectureDesConflits lecture) {
    return new RestPageDesAnomalies(lecture);
  }
}
