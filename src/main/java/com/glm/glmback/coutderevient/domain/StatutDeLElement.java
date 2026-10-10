package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Ce que le compte rendu dit de l'avancement de l'element : en cours, ou termine a la date de sa derniere cloture.
 *
 * <p>
 * L'element est termine quand il a ete engage, que tous ses passages en atelier sont clos et qu'aucune activite n'est
 * encore en cours a l'instant d'evaluation. Sinon le compte rendu n'est qu'une photographie a cet instant.
 * </p>
 */
public record StatutDeLElement(Optional<Instant> termineLe) {
  public static final StatutDeLElement EN_COURS = new StatutDeLElement(Optional.empty());

  public StatutDeLElement {
    Assert.notNull("termine le", termineLe);
  }

  public static StatutDeLElement de(List<PassageEnAtelier> passages, EvaluationDuCout lecture) {
    Assert.field("passages", passages).notNull().noNullElement();
    Assert.notNull("lecture", lecture);
    if (passages.isEmpty() || lecture.activitesEnCours() > 0 || passages.stream().anyMatch(passage -> passage.cloture().isEmpty())) {
      return EN_COURS;
    }
    return new StatutDeLElement(
      passages
        .stream()
        .map(passage -> passage.cloture().orElseThrow())
        .max(Comparator.naturalOrder())
    );
  }
}
