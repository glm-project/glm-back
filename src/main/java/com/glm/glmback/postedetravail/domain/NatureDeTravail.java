package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Le libelle du metier qui s'exerce sur un poste : soudage, tournage, fraisage, dessin.
 *
 * <p>
 * Elle appartient au poste, jamais a la personne : c'est le poste choisi au pointage qui dit quel metier est exerce a
 * cet instant. Les metiers d'un operateur se deduisent des postes sur lesquels il est habilite. Le poste n'en retient
 * que l'identifiant ({@link NatureDuPoste}) : le libelle est celui du referentiel des natures.
 * </p>
 */
public record NatureDeTravail(String value) {
  private static final int MAX_LENGTH = 50;
  private static final Pattern MARQUES_DIACRITIQUES = Pattern.compile("\\p{M}+");
  private static final Pattern ESPACES = Pattern.compile("\\s+");

  public NatureDeTravail {
    Assert.field("nature de travail", value).notBlank();
    value = value.strip();
    Assert.field("nature de travail", value).maxLength(MAX_LENGTH);
  }

  /**
   * La cle d'unicite du referentiel des natures : minuscules, sans accents, espaces reduits a un seul. Elle reproduit
   * {@code naturedetravail.domain.CleDeNature}, que ce contexte ne peut pas importer, pour le seul chemin de transition
   * du libelle saisi en texte.
   *
   * @deprecated retiree avec ce chemin, quand le front envoie l'identifiant (glm-back#130).
   */
  @Deprecated(forRemoval = true)
  public String cle() {
    String sansAccents = MARQUES_DIACRITIQUES.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");

    return ESPACES.matcher(sansAccents).replaceAll(" ").toLowerCase(Locale.ROOT);
  }
}
