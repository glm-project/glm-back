package com.glm.glmback.shared.elementtype.infrastructure.primary;

import java.util.Map;
import java.util.Optional;

/**
 * Translation between the legacy element type (ORDRE_DE_FABRICATION or PRODUIT) and the product category that replaced
 * it, so that clients still sending or reading {@code type} keep working while they move to {@code categorie}.
 *
 * <p>
 * Transitional only: it maps onto the two categories of the reference client, and is removed with the deprecated
 * {@code type} fields once every client reads {@code categorie}.
 * </p>
 */
public final class LegacyElementType {

  public static final String ORDRE_DE_FABRICATION = "ORDRE_DE_FABRICATION";
  public static final String PRODUIT = "PRODUIT";

  private static final String CATEGORIE_OF = "OF";
  private static final Map<String, String> CATEGORIES = Map.of(ORDRE_DE_FABRICATION, CATEGORIE_OF, PRODUIT, "MOULE");

  private LegacyElementType() {}

  public static Optional<String> toCategory(String type) {
    return Optional.ofNullable(type).map(CATEGORIES::get);
  }

  public static String fromCategory(String categorie) {
    return CATEGORIE_OF.equals(categorie) ? ORDRE_DE_FABRICATION : PRODUIT;
  }
}
