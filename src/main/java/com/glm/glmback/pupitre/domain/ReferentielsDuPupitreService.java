package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.activityduration.domain.MaximumActivityDurations;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;

/**
 * Assemble le referentiel du pupitre et le date.
 *
 * <p>
 * La date vient du port {@link Clock}, jamais d'un appel direct a l'horloge de la machine : c'est ce qui rend le
 * scenario Cucumber capable de la figer.
 * </p>
 *
 * <p>
 * La duree maximale d'une activite vient du port {@link MaximumActivityDurations}, lue a chaque referentiel : c'est un
 * reglage de l'entreprise, que le gestionnaire peut changer, et que le pupitre hors ligne lit ici plutot que de la
 * coder.
 * </p>
 */
public final class ReferentielsDuPupitreService {

  private final OperateursDuPupitre operateurs;
  private final SuivisOuvertsDuPupitre suivis;
  private final CategoriesDuPupitre categories;
  private final MaximumActivityDurations durees;
  private final Clock clock;

  private ReferentielsDuPupitreService(
    OperateursDuPupitre operateurs,
    SuivisOuvertsDuPupitre suivis,
    CategoriesDuPupitre categories,
    MaximumActivityDurations durees,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.suivis = suivis;
    this.categories = categories;
    this.durees = durees;
    this.clock = clock;
  }

  public static ReferentielsDuPupitreServiceOperateursBuilder builder() {
    return operateurs ->
      suivis -> categories -> durees -> clock -> new ReferentielsDuPupitreService(operateurs, suivis, categories, durees, clock);
  }

  public ReferentielDuPupitre referentiel() {
    Instant maintenant = clock.now();

    return new ReferentielDuPupitre(maintenant, operateurs.tous(), suivis.tous(), categories.toutes(), durees.current());
  }

  public interface ReferentielsDuPupitreServiceOperateursBuilder {
    ReferentielsDuPupitreServiceSuivisBuilder operateurs(OperateursDuPupitre operateurs);
  }

  public interface ReferentielsDuPupitreServiceSuivisBuilder {
    ReferentielsDuPupitreServiceCategoriesBuilder suivis(SuivisOuvertsDuPupitre suivis);
  }

  public interface ReferentielsDuPupitreServiceCategoriesBuilder {
    ReferentielsDuPupitreServiceDureesBuilder categories(CategoriesDuPupitre categories);
  }

  public interface ReferentielsDuPupitreServiceDureesBuilder {
    ReferentielsDuPupitreServiceClockBuilder dureeMaximaleDActivite(MaximumActivityDurations durees);
  }

  public interface ReferentielsDuPupitreServiceClockBuilder {
    ReferentielsDuPupitreService clock(Clock clock);
  }
}
