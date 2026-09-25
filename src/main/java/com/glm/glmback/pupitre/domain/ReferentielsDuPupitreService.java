package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;

/**
 * Assemble le referentiel du pupitre et le date.
 *
 * <p>
 * La date vient du port {@link Clock}, jamais d'un appel direct a l'horloge de la machine : c'est ce qui rend le
 * scenario Cucumber capable de la figer.
 * </p>
 */
public final class ReferentielsDuPupitreService {

  private final OperateursDuPupitre operateurs;
  private final SuivisOuvertsDuPupitre suivis;
  private final PresencesDuPupitre presences;
  private final SeuilDuPupitre seuil;
  private final Clock clock;

  private ReferentielsDuPupitreService(
    OperateursDuPupitre operateurs,
    SuivisOuvertsDuPupitre suivis,
    PresencesDuPupitre presences,
    SeuilDuPupitre seuil,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.suivis = suivis;
    this.presences = presences;
    this.seuil = seuil;
    this.clock = clock;
  }

  public static ReferentielsDuPupitreServiceOperateursBuilder builder() {
    return operateurs ->
      suivis -> presences -> seuil -> clock -> new ReferentielsDuPupitreService(operateurs, suivis, presences, seuil, clock);
  }

  public ReferentielDuPupitre referentiel() {
    Instant maintenant = clock.now();

    return new ReferentielDuPupitre(
      maintenant,
      operateurs.tous(presences.toutes().a(maintenant, seuil.amplitudeMaximale())),
      suivis.tous()
    );
  }

  public interface ReferentielsDuPupitreServiceOperateursBuilder {
    ReferentielsDuPupitreServiceSuivisBuilder operateurs(OperateursDuPupitre operateurs);
  }

  public interface ReferentielsDuPupitreServiceSuivisBuilder {
    ReferentielsDuPupitreServicePresencesBuilder suivis(SuivisOuvertsDuPupitre suivis);
  }

  public interface ReferentielsDuPupitreServicePresencesBuilder {
    ReferentielsDuPupitreServiceSeuilBuilder presences(PresencesDuPupitre presences);
  }

  public interface ReferentielsDuPupitreServiceSeuilBuilder {
    ReferentielsDuPupitreServiceClockBuilder seuil(SeuilDuPupitre seuil);
  }

  public interface ReferentielsDuPupitreServiceClockBuilder {
    ReferentielsDuPupitreService clock(Clock clock);
  }
}
