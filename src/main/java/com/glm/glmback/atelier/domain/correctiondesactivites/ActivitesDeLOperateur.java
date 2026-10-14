package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.OperateurId;
import java.time.Instant;
import java.util.List;

/**
 * Les activites d'un operateur que la projection de l'atelier porte, et qui touchent une plage : celles dont le debut
 * precede la fin de la plage et dont la fin, reelle ou a defaut l'echeance, suit son debut. Elles sont rendues par debut,
 * puis par identite. Aucun journal n'est rejoue.
 */
public interface ActivitesDeLOperateur {
  List<ActiviteProjetee> touchant(OperateurId operateur, Instant debut, Instant finExclue);
}
