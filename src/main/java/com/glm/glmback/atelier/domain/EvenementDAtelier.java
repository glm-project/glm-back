package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un fait du journal d'atelier : tel operateur a fait telle action sur tel poste de travail, a telle heure.
 *
 * <p>
 * Son type dit ce qu'il fait d'une activite. Un debut et une non conformite ouvrent une activite, dont ils portent
 * l'identite : la leur. Une fin n'en ouvre aucune et ferme l'activite en cours de sa cle, sans la designer ; seule la
 * fin que le gestionnaire regularise porte une cible, l'activite echue dont elle etablit la fin.
 * </p>
 *
 * <p>
 * L'operateur et l'auteur sont deux choses differentes : un gestionnaire qui rattrape un oubli saisit un evenement dont
 * l'operateur est celui dont le temps est affecte, et dont l'auteur est lui-meme.
 * </p>
 *
 * <p>
 * La nature de l'operation est recopiee du poste au moment de la saisie. Elle ne porte aucun invariant et ne
 * participe pas a l'identite de l'activite : elle n'est qu'un axe d'agregation, fige pour que la synthese d'un element
 * ne change pas le jour ou un poste est requalifie. C'est avec le cout horaire du poste et le taux horaire de
 * l'operateur, sur le meme patron, tout ce que le journal copie du referentiel : tout le reste n'y est que reference.
 * </p>
 */
public record EvenementDAtelier(
  EvenementDAtelierId id,
  TypeDEvenementDAtelier type,
  Optional<ActiviteId> activite,
  Optional<ActiviteId> activiteVisee,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  Optional<CoutHoraire> coutHoraire,
  Optional<TauxHoraire> tauxHoraire,
  Auteur auteur,
  OrigineDuPointage origine,
  Horodatage horodatage
) {
  public EvenementDAtelier {
    Assert.notNull("id", id);
    Assert.notNull("type", type);
    Assert.notNull("activite", activite);
    Assert.notNull("activite visee", activiteVisee);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("cout horaire", coutHoraire);
    Assert.notNull("taux horaire", tauxHoraire);
    Assert.notNull("auteur", auteur);
    Assert.notNull("origine", origine);
    Assert.notNull("horodatage", horodatage);
    exigeLesActivitesDuType(type, activite, activiteVisee, origine);
  }

  private EvenementDAtelier(EvenementDAtelierBuilder builder) {
    this(
      builder.id,
      builder.type,
      builder.activite,
      builder.activiteVisee,
      builder.operateur,
      builder.poste,
      builder.nature,
      builder.coutHoraire,
      builder.tauxHoraire,
      builder.auteur,
      builder.origine,
      builder.horodatage
    );
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}. La creation, elle, reste l'affaire de ce domaine.
   */
  public static EvenementDAtelierIdBuilder builder() {
    return new EvenementDAtelierBuilder();
  }

  /**
   * Vrai si le fait a ete porte au journal par un acte du gestionnaire — une regularisation. C'est son origine qui le dit, jamais l'ecart entre les deux dates, qui caracterise aussi un pointage
   * rejoue hors ligne, ni l'identite de l'auteur.
   */
  public boolean estUneRegularisation() {
    return origine == OrigineDuPointage.REGULARISATION;
  }

  public CleDActivite cle() {
    return new CleDActivite(operateur, poste);
  }

  public Instant dateDeSurvenue() {
    return horodatage.dateDeSurvenue();
  }

  public Instant dateDEnregistrement() {
    return horodatage.dateDEnregistrement();
  }

  /**
   * Seul un debut ou une non conformite ouvre une activite, et seule la fin d'une regularisation en cible une : un
   * pointage ne designe jamais l'activite qu'il ferme, la cle la donne.
   */
  private static void exigeLesActivitesDuType(
    TypeDEvenementDAtelier type,
    Optional<ActiviteId> activite,
    Optional<ActiviteId> activiteVisee,
    OrigineDuPointage origine
  ) {
    boolean ouvre = type.ouvreUneActivite();
    if (activite.isPresent() != ouvre) {
      throw new EvenementDAtelierIncoherentException(type, origine);
    }
    if (activiteVisee.isPresent() != (!ouvre && origine == OrigineDuPointage.REGULARISATION)) {
      throw new EvenementDAtelierIncoherentException(type, origine);
    }
  }

  private static final class EvenementDAtelierBuilder
    implements
      EvenementDAtelierIdBuilder,
      EvenementDAtelierTypeBuilder,
      EvenementDAtelierActiviteBuilder,
      EvenementDAtelierActiviteViseeBuilder,
      EvenementDAtelierOperateurBuilder,
      EvenementDAtelierPosteBuilder,
      EvenementDAtelierNatureBuilder,
      EvenementDAtelierCoutHoraireBuilder,
      EvenementDAtelierTauxHoraireBuilder,
      EvenementDAtelierAuteurBuilder,
      EvenementDAtelierOrigineBuilder,
      EvenementDAtelierHorodatageBuilder
  {

    private EvenementDAtelierId id;
    private TypeDEvenementDAtelier type;
    private Optional<ActiviteId> activite;
    private Optional<ActiviteId> activiteVisee;
    private OperateurId operateur;
    private Optional<PosteDeTravailId> poste;
    private Optional<NatureDOperation> nature;
    private Optional<CoutHoraire> coutHoraire;
    private Optional<TauxHoraire> tauxHoraire;
    private Auteur auteur;
    private OrigineDuPointage origine;
    private Horodatage horodatage;

    @Override
    public EvenementDAtelierTypeBuilder id(EvenementDAtelierId id) {
      this.id = id;

      return this;
    }

    @Override
    public EvenementDAtelierActiviteBuilder type(TypeDEvenementDAtelier type) {
      this.type = type;

      return this;
    }

    @Override
    public EvenementDAtelierActiviteViseeBuilder activite(Optional<ActiviteId> activite) {
      this.activite = activite;

      return this;
    }

    @Override
    public EvenementDAtelierOperateurBuilder activiteVisee(Optional<ActiviteId> activiteVisee) {
      this.activiteVisee = activiteVisee;

      return this;
    }

    @Override
    public EvenementDAtelierPosteBuilder operateur(OperateurId operateur) {
      this.operateur = operateur;

      return this;
    }

    @Override
    public EvenementDAtelierNatureBuilder poste(Optional<PosteDeTravailId> poste) {
      this.poste = poste;

      return this;
    }

    @Override
    public EvenementDAtelierCoutHoraireBuilder nature(Optional<NatureDOperation> nature) {
      this.nature = nature;

      return this;
    }

    @Override
    public EvenementDAtelierTauxHoraireBuilder coutHoraire(Optional<CoutHoraire> coutHoraire) {
      this.coutHoraire = coutHoraire;

      return this;
    }

    @Override
    public EvenementDAtelierAuteurBuilder tauxHoraire(Optional<TauxHoraire> tauxHoraire) {
      this.tauxHoraire = tauxHoraire;

      return this;
    }

    @Override
    public EvenementDAtelierOrigineBuilder auteur(Auteur auteur) {
      this.auteur = auteur;

      return this;
    }

    @Override
    public EvenementDAtelierHorodatageBuilder origine(OrigineDuPointage origine) {
      this.origine = origine;

      return this;
    }

    @Override
    public EvenementDAtelier horodatage(Horodatage horodatage) {
      this.horodatage = horodatage;

      return new EvenementDAtelier(this);
    }
  }

  public interface EvenementDAtelierIdBuilder {
    EvenementDAtelierTypeBuilder id(EvenementDAtelierId id);
  }

  public interface EvenementDAtelierTypeBuilder {
    EvenementDAtelierActiviteBuilder type(TypeDEvenementDAtelier type);
  }

  public interface EvenementDAtelierActiviteBuilder {
    EvenementDAtelierActiviteViseeBuilder activite(Optional<ActiviteId> activite);
  }

  public interface EvenementDAtelierActiviteViseeBuilder {
    EvenementDAtelierOperateurBuilder activiteVisee(Optional<ActiviteId> activiteVisee);
  }

  public interface EvenementDAtelierOperateurBuilder {
    EvenementDAtelierPosteBuilder operateur(OperateurId operateur);
  }

  public interface EvenementDAtelierPosteBuilder {
    EvenementDAtelierNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface EvenementDAtelierNatureBuilder {
    EvenementDAtelierCoutHoraireBuilder nature(Optional<NatureDOperation> nature);
  }

  public interface EvenementDAtelierCoutHoraireBuilder {
    EvenementDAtelierTauxHoraireBuilder coutHoraire(Optional<CoutHoraire> coutHoraire);
  }

  public interface EvenementDAtelierTauxHoraireBuilder {
    EvenementDAtelierAuteurBuilder tauxHoraire(Optional<TauxHoraire> tauxHoraire);
  }

  public interface EvenementDAtelierAuteurBuilder {
    EvenementDAtelierOrigineBuilder auteur(Auteur auteur);
  }

  public interface EvenementDAtelierOrigineBuilder {
    EvenementDAtelierHorodatageBuilder origine(OrigineDuPointage origine);
  }

  public interface EvenementDAtelierHorodatageBuilder {
    EvenementDAtelier horodatage(Horodatage horodatage);
  }
}
