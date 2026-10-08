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
  private final CategoriesDuPupitre categories;
  private final Clock clock;

  private ReferentielsDuPupitreService(
    OperateursDuPupitre operateurs,
    SuivisOuvertsDuPupitre suivis,
    CategoriesDuPupitre categories,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.suivis = suivis;
    this.categories = categories;
    this.clock = clock;
  }

  public static ReferentielsDuPupitreServiceOperateursBuilder builder() {
    return operateurs -> suivis -> categories -> clock -> new ReferentielsDuPupitreService(operateurs, suivis, categories, clock);
  }

  public ReferentielDuPupitre referentiel() {
    Instant maintenant = clock.now();

    return new ReferentielDuPupitre(maintenant, operateurs.tous(), suivis.tous(), categories.toutes());
  }

  public interface ReferentielsDuPupitreServiceOperateursBuilder {
    ReferentielsDuPupitreServiceSuivisBuilder operateurs(OperateursDuPupitre operateurs);
  }

  public interface ReferentielsDuPupitreServiceSuivisBuilder {
    ReferentielsDuPupitreServiceCategoriesBuilder suivis(SuivisOuvertsDuPupitre suivis);
  }

  public interface ReferentielsDuPupitreServiceCategoriesBuilder {
    ReferentielsDuPupitreServiceClockBuilder categories(CategoriesDuPupitre categories);
  }

  public interface ReferentielsDuPupitreServiceClockBuilder {
    ReferentielsDuPupitreService clock(Clock clock);
  }
}
