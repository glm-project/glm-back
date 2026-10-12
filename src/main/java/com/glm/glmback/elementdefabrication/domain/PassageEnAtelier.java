package com.glm.glmback.elementdefabrication.domain;

import java.time.Instant;

/**
 * Le passage d'un element en atelier, tel que son suivi le dit. Un element n'en a qu'un dans sa vie : ouvert, il dit
 * son etat ; clos, il dit quand et par qui.
 */
public sealed interface PassageEnAtelier permits PassageOuvert, PassageClos {
  SuiviId suivi();

  Instant misALAtelierLe();
}
