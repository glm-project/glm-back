package com.glm.glmback.pupitre.application;

import com.glm.glmback.pupitre.domain.OperateursDuPupitre;
import com.glm.glmback.pupitre.domain.ReferentielDuPupitre;
import com.glm.glmback.pupitre.domain.ReferentielsDuPupitreService;
import com.glm.glmback.pupitre.domain.SuivisOuvertsDuPupitre;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture du referentiel du pupitre.
 *
 * <p>
 * Rien a ecrire : le contexte ne fait que relire les tables des referentiels et le journal de l'atelier.
 * </p>
 */
@Service
public class ReferentielsDuPupitreApplicationService {

  private final ReferentielsDuPupitreService referentiels;

  public ReferentielsDuPupitreApplicationService(OperateursDuPupitre operateurs, SuivisOuvertsDuPupitre suivis, Clock clock) {
    this.referentiels = new ReferentielsDuPupitreService(operateurs, suivis, clock);
  }

  /**
   * L'instantane tient a l'isolation, pas a la transaction.
   *
   * <p>
   * Sous {@code READ COMMITTED}, chaque requete prend son propre instantane : la lecture des operateurs pourrait
   * ignorer un operateur que la lecture suivante des suivis designe dans une activite en cours. {@code REPEATABLE
   * READ} est ce qui fait de cette reponse la version instantanee unique que le cache du pupitre vient chercher, et
   * c'est la seule raison pour laquelle il est demande ici.
   * </p>
   *
   * <p>
   * La lecture est ouverte aux deux roles metier : le pupitre porte un compte d'appareil {@code USER}, et le
   * back-office doit pouvoir verifier ce que l'atelier voit. {@code ADMIN} n'a aucun acces metier.
   * </p>
   */
  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ReferentielDuPupitre referentiel() {
    return referentiels.referentiel();
  }
}
