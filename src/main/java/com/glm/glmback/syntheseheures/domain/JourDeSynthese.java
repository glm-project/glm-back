package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Un jour du calendrier, son journal brut, le temps de presence et le temps operationnel qui lui reviennent.
 *
 * <p>
 * Les sept jours sont toujours rendus, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il
 * manque une journee ou si l'operateur n'etait pas la.
 * </p>
 */
public record JourDeSynthese(
  LocalDate jour,
  List<PointageDuJour> pointages,
  Duration duree,
  Duration dureePresumee,
  Duration dureeOperationnelle,
  Duration dureeOperationnellePresumee
) {
  public JourDeSynthese {
    Assert.notNull("jour", jour);
    Assert.field("pointages", pointages).notNull().noNullElement();
    Assert.notNull("duree", duree);
    Assert.notNull("duree presumee", dureePresumee);
    Assert.notNull("duree operationnelle", dureeOperationnelle);
    Assert.notNull("duree operationnelle presumee", dureeOperationnellePresumee);
  }

  private JourDeSynthese(JourDeSyntheseBuilder builder) {
    this(
      builder.jour,
      builder.pointages,
      builder.duree,
      builder.dureePresumee,
      builder.dureeOperationnelle,
      builder.dureeOperationnellePresumee
    );
  }

  static JourDeSyntheseJourBuilder builder() {
    return new JourDeSyntheseBuilder();
  }

  private static final class JourDeSyntheseBuilder
    implements
      JourDeSyntheseJourBuilder,
      JourDeSynthesePointagesBuilder,
      JourDeSyntheseDureeBuilder,
      JourDeSyntheseDureePresumeeBuilder,
      JourDeSyntheseDureeOperationnelleBuilder,
      JourDeSyntheseDureeOperationnellePresumeeBuilder
  {

    private LocalDate jour;
    private List<PointageDuJour> pointages;
    private Duration duree;
    private Duration dureePresumee;
    private Duration dureeOperationnelle;
    private Duration dureeOperationnellePresumee;

    @Override
    public JourDeSynthesePointagesBuilder jour(LocalDate jour) {
      this.jour = jour;

      return this;
    }

    @Override
    public JourDeSyntheseDureeBuilder pointages(List<PointageDuJour> pointages) {
      this.pointages = pointages;

      return this;
    }

    @Override
    public JourDeSyntheseDureePresumeeBuilder duree(Duration duree) {
      this.duree = duree;

      return this;
    }

    @Override
    public JourDeSyntheseDureeOperationnelleBuilder dureePresumee(Duration dureePresumee) {
      this.dureePresumee = dureePresumee;

      return this;
    }

    @Override
    public JourDeSyntheseDureeOperationnellePresumeeBuilder dureeOperationnelle(Duration dureeOperationnelle) {
      this.dureeOperationnelle = dureeOperationnelle;

      return this;
    }

    @Override
    public JourDeSynthese dureeOperationnellePresumee(Duration dureeOperationnellePresumee) {
      this.dureeOperationnellePresumee = dureeOperationnellePresumee;

      return new JourDeSynthese(this);
    }
  }

  interface JourDeSyntheseJourBuilder {
    JourDeSynthesePointagesBuilder jour(LocalDate jour);
  }

  interface JourDeSynthesePointagesBuilder {
    JourDeSyntheseDureeBuilder pointages(List<PointageDuJour> pointages);
  }

  interface JourDeSyntheseDureeBuilder {
    JourDeSyntheseDureePresumeeBuilder duree(Duration duree);
  }

  interface JourDeSyntheseDureePresumeeBuilder {
    JourDeSyntheseDureeOperationnelleBuilder dureePresumee(Duration dureePresumee);
  }

  interface JourDeSyntheseDureeOperationnelleBuilder {
    JourDeSyntheseDureeOperationnellePresumeeBuilder dureeOperationnelle(Duration dureeOperationnelle);
  }

  interface JourDeSyntheseDureeOperationnellePresumeeBuilder {
    JourDeSynthese dureeOperationnellePresumee(Duration dureeOperationnellePresumee);
  }
}
