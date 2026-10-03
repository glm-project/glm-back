package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.ContexteDeResolution;
import com.glm.glmback.atelier.application.IdentiteDuGestionnaire;
import com.glm.glmback.shared.authentication.application.AuthenticatedUser;
import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import java.util.Objects;

final class GestionnaireConnecte {

  private GestionnaireConnecte() {}

  static ContexteDeResolution get() {
    var attributes = AuthenticatedUser.attributes();
    return new ContexteDeResolution(
      CurrentTenant.tenant().value(),
      new IdentiteDuGestionnaire(AuteurConnecte.get(), (String) attributes.get("sub"), Objects.toString(attributes.get("iss"), null))
    );
  }
}
