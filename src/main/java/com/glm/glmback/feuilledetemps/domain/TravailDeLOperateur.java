package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.util.List;

/**
 * Les passages en atelier ou l'operateur a pointe sur la periode, chacun avec <strong>tout</strong> son journal
 * restreint a cet operateur, et sa cloture.
 *
 * <p>
 * Tout le journal, et non la seule periode : une non conformite de la semaine peut suivre un debut de la semaine
 * precedente. La periode ne sert qu'a choisir les suivis.
 * </p>
 */
@FunctionalInterface
public interface TravailDeLOperateur {
  List<SuiviDuTravail> suivis(OperateurId operateur, Instant depuis, Instant finExclusive);
}
