package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un fait du journal d'atelier, reduit a ce que la valorisation a besoin d'en savoir.
 *
 * <p>
 * Ni auteur, ni date d'enregistrement, ni annulation : la correction est l'affaire de l'atelier, et les evenements
 * annules sont ecartes des la requete. Restent le fait, l'activite qu'il engage, et les trois valeurs que le journal
 * a figees du referentiel au moment de la saisie — nature, cout horaire du poste, taux horaire de l'operateur.
 * </p>
 *
 * <p>
 * C'est parce qu'elles sont figees la que ce rapport est reproductible : un poste requalifie ou un tarif revise ne
 * reecrit pas le cout des heures deja passees.
 * </p>
 */
public record EvenementDAtelier(
  TypeDEvenementDAtelier type,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  Optional<CoutHoraire> coutHoraire,
  Optional<TauxHoraire> tauxHoraire,
  Instant dateDeSurvenue
) {
  public EvenementDAtelier {
    Assert.notNull("type", type);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("cout horaire", coutHoraire);
    Assert.notNull("taux horaire", tauxHoraire);
    Assert.notNull("date de survenue", dateDeSurvenue);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static EvenementDAtelierTypeBuilder builder() {
    return type ->
      operateur ->
        poste ->
          nature ->
            coutHoraire ->
              tauxHoraire ->
                dateDeSurvenue -> new EvenementDAtelier(type, operateur, poste, nature, coutHoraire, tauxHoraire, dateDeSurvenue);
  }

  public CleDActivite cle() {
    return new CleDActivite(operateur, poste);
  }

  public interface EvenementDAtelierTypeBuilder {
    EvenementDAtelierOperateurBuilder type(TypeDEvenementDAtelier type);
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
    EvenementDAtelierDateDeSurvenueBuilder tauxHoraire(Optional<TauxHoraire> tauxHoraire);
  }

  public interface EvenementDAtelierDateDeSurvenueBuilder {
    EvenementDAtelier dateDeSurvenue(Instant dateDeSurvenue);
  }
}
