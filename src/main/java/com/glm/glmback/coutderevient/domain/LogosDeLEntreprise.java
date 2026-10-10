package com.glm.glmback.coutderevient.domain;

import java.util.Optional;

/** Le logo courant de l'entreprise, lu au parametrage ; absent tant qu'aucun n'a ete depose. */
@FunctionalInterface
public interface LogosDeLEntreprise {
  Optional<LogoDeLEntreprise> logo();
}
