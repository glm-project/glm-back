package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

public final class ResolutionFixture {

  public static final String TENANT_IMPECCMOLD = "impeccmold";
  public static final String SUJET_LEROY = "user-leroy-uuid";
  public static final String EMETTEUR_GLM = "https://identity.glm.example/realms/glm";
  public static final IdentiteDuGestionnaire GESTIONNAIRE_LEROY = new IdentiteDuGestionnaire(AUTEUR_LEROY, SUJET_LEROY, EMETTEUR_GLM);
  public static final ContexteDeResolution CONTEXTE_LEROY_IMPECCMOLD = new ContexteDeResolution(TENANT_IMPECCMOLD, GESTIONNAIRE_LEROY);
  public static final IdentiteDuGestionnaire GESTIONNAIRE_LEROY_RENOMME = new IdentiteDuGestionnaire(
    AUTEUR_MARTIN,
    SUJET_LEROY,
    EMETTEUR_GLM
  );
  public static final ContexteDeResolution CONTEXTE_LEROY_RENOMME_IMPECCMOLD = new ContexteDeResolution(
    TENANT_IMPECCMOLD,
    GESTIONNAIRE_LEROY_RENOMME
  );
  public static final ContexteDeResolution CONTEXTE_MARTIN_IMPECCMOLD = new ContexteDeResolution(
    TENANT_IMPECCMOLD,
    new IdentiteDuGestionnaire(AUTEUR_LEROY, "user-martin-uuid", EMETTEUR_GLM)
  );
  public static final ContexteDeResolution CONTEXTE_LEROY_AUTRE_EMETTEUR = new ContexteDeResolution(
    TENANT_IMPECCMOLD,
    new IdentiteDuGestionnaire(AUTEUR_LEROY, SUJET_LEROY, "https://identity.other.example/realms/glm")
  );
  public static final ContexteDeResolution CONTEXTE_LEROY_KATILYS = new ContexteDeResolution("katilys", GESTIONNAIRE_LEROY);

  private ResolutionFixture() {}
}
