package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Un jour du calendrier, ses pointages et le temps travaille qui lui revient.
 *
 * <p>
 * Les sept jours sont toujours rendus, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il
 * manque une journee ou si l'operateur n'etait pas la.
 * </p>
 */
public record JourDeSynthese(LocalDate jour, List<EvenementDePresence> pointages, Duration duree, Duration dureePresumee) {
  public JourDeSynthese {
    Assert.notNull("jour", jour);
    Assert.field("pointages", pointages).notNull().noNullElement();
    Assert.notNull("duree", duree);
    Assert.notNull("duree presumee", dureePresumee);
  }

  private JourDeSynthese(JourDeSyntheseBuilder builder) {
    this(builder.jour, builder.pointages, builder.duree, builder.dureePresumee);
  }

  static JourDeSyntheseJourBuilder builder() {
    return new JourDeSyntheseBuilder();
  }

  private static final class JourDeSyntheseBuilder
    implements JourDeSyntheseJourBuilder, JourDeSynthesePointagesBuilder, JourDeSyntheseDureeBuilder, JourDeSyntheseDureePresumeeBuilder
  {

    private LocalDate jour;
    private List<EvenementDePresence> pointages;
    private Duration duree;
    private Duration dureePresumee;

    @Override
    public JourDeSynthesePointagesBuilder jour(LocalDate jour) {
      this.jour = jour;

      return this;
    }

    @Override
    public JourDeSyntheseDureeBuilder pointages(List<EvenementDePresence> pointages) {
      this.pointages = pointages;

      return this;
    }

    @Override
    public JourDeSyntheseDureePresumeeBuilder duree(Duration duree) {
      this.duree = duree;

      return this;
    }

    @Override
    public JourDeSynthese dureePresumee(Duration dureePresumee) {
      this.dureePresumee = dureePresumee;

      return new JourDeSynthese(this);
    }
  }

  interface JourDeSyntheseJourBuilder {
    JourDeSynthesePointagesBuilder jour(LocalDate jour);
  }

  interface JourDeSynthesePointagesBuilder {
    JourDeSyntheseDureeBuilder pointages(List<EvenementDePresence> pointages);
  }

  interface JourDeSyntheseDureeBuilder {
    JourDeSyntheseDureePresumeeBuilder duree(Duration duree);
  }

  interface JourDeSyntheseDureePresumeeBuilder {
    JourDeSynthese dureePresumee(Duration dureePresumee);
  }
}
