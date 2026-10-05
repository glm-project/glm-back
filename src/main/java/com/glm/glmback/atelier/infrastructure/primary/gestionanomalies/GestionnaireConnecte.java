package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.ContexteDeResolution;
import com.glm.glmback.atelier.application.gestionanomalies.IdentiteDuGestionnaire;
import com.glm.glmback.atelier.infrastructure.primary.AuteurConnecte;
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
