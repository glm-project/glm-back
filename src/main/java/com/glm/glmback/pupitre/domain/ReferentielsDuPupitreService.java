package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.time.domain.Clock;

/**
 * Assemble le referentiel du pupitre et le date.
 *
 * <p>
 * La date vient du port {@link Clock}, jamais d'un appel direct a l'horloge de la machine : c'est ce qui rend le
 * scenario Cucumber capable de la figer.
 * </p>
 */
public class ReferentielsDuPupitreService {

  private final OperateursDuPupitre operateurs;
  private final SuivisOuvertsDuPupitre suivis;
  private final Clock clock;

  public ReferentielsDuPupitreService(OperateursDuPupitre operateurs, SuivisOuvertsDuPupitre suivis, Clock clock) {
    this.operateurs = operateurs;
    this.suivis = suivis;
    this.clock = clock;
  }

  public ReferentielDuPupitre referentiel() {
    return new ReferentielDuPupitre(clock.now(), operateurs.tous(), suivis.tous());
  }
}
