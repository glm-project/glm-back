package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.OperateurDuPupitre;
import com.glm.glmback.pupitre.domain.OperateursDuPupitre;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Les operateurs de l'entreprise courante et leurs habilitations, en une requete.
 *
 * <p>
 * Une jointure ramenee d'un coup, jamais une requete par operateur : le pupitre relit ce referentiel entier a chaque
 * synchronisation.
 * </p>
 */
@Repository
class OperateursDuReferentielDuPupitre implements OperateursDuPupitre {

  private final SpringDataOperateursDuPupitreRepository operateurs;

  OperateursDuReferentielDuPupitre(SpringDataOperateursDuPupitreRepository operateurs) {
    this.operateurs = operateurs;
  }

  @Override
  public List<OperateurDuPupitre> tous() {
    return operateurs.tous().stream().map(OperateurDuPupitreEntity::toDomain).toList();
  }
}
