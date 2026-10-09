package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/** Une acquisition de la projection et une resolution en lot de chacune des ressources. */
public final class ListeDesAnomaliesService {

  private final FinsAutomatiquesDAtelier finsAutomatiques;
  private final OperateursConnus operateurs;
  private final PostesConnus postes;

  public ListeDesAnomaliesService(FinsAutomatiquesDAtelier finsAutomatiques, OperateursConnus operateurs, PostesConnus postes) {
    this.finsAutomatiques = finsAutomatiques;
    this.operateurs = operateurs;
    this.postes = postes;
  }

  public LectureDesFinsAutomatiques listFinsAutomatiques(AnomaliesDAtelierCriteria criteria, Instant evaluation, Pageable pageable) {
    var page = finsAutomatiques.list(criteria, evaluation, pageable);
    return new LectureDesFinsAutomatiques(page, annuaire(page.content().stream().map(FinAutomatiqueEnListe::cle).toList()));
  }

  private AnnuaireDAtelier annuaire(List<CleDActivite> cles) {
    var operateursAResoudre = cles.stream().map(CleDActivite::operateur).collect(Collectors.toSet());
    var postesAResoudre = cles
      .stream()
      .flatMap(cle -> cle.poste().stream())
      .collect(Collectors.toSet());
    return AnnuaireDAtelier.de(operateurs.parIds(operateursAResoudre), postes.parIds(postesAResoudre));
  }
}
