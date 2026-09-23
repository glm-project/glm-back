package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.time.domain.Clock;

/**
 * Assemble le referentiel du pupitre et le date.
 *
 * <p>
 * La date vient du port {@link Clock}, jamais d'un appel direct a l'horloge de la machine : c'est ce qui rend le
 * scenario Cucumber capable de la figer.
 * </p>
 *
 * <p>
 * Les presences sont relevees une fois, puis remises a la lecture des operateurs : c'est ici, et nulle part ailleurs,
 * que les deux lectures se rejoignent — l'adapter des operateurs ne fait qu'interroger le releve.
 * </p>
 */
public final class ReferentielsDuPupitreService {

  private final OperateursDuPupitre operateurs;
  private final SuivisOuvertsDuPupitre suivis;
  private final PresencesDuPupitre presences;
  private final Clock clock;

  private ReferentielsDuPupitreService(
    OperateursDuPupitre operateurs,
    SuivisOuvertsDuPupitre suivis,
    PresencesDuPupitre presences,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.suivis = suivis;
    this.presences = presences;
    this.clock = clock;
  }

  public static ReferentielsDuPupitreServiceOperateursBuilder builder() {
    return operateurs -> suivis -> presences -> clock -> new ReferentielsDuPupitreService(operateurs, suivis, presences, clock);
  }

  public ReferentielDuPupitre referentiel() {
    return new ReferentielDuPupitre(clock.now(), operateurs.tous(presences.toutes()), suivis.tous());
  }

  public interface ReferentielsDuPupitreServiceOperateursBuilder {
    ReferentielsDuPupitreServiceSuivisBuilder operateurs(OperateursDuPupitre operateurs);
  }

  public interface ReferentielsDuPupitreServiceSuivisBuilder {
    ReferentielsDuPupitreServicePresencesBuilder suivis(SuivisOuvertsDuPupitre suivis);
  }

  public interface ReferentielsDuPupitreServicePresencesBuilder {
    ReferentielsDuPupitreServiceClockBuilder presences(PresencesDuPupitre presences);
  }

  public interface ReferentielsDuPupitreServiceClockBuilder {
    ReferentielsDuPupitreService clock(Clock clock);
  }
}
