package com.glm.glmback.pupitre.domain;

import java.util.List;

/**
 * Les elements engages sur lesquels on peut encore pointer, journal replie.
 *
 * <p>
 * Les elements clotures en sont absents : ils n'acceptent plus de pointage, la tuile n'aurait aucun usage. Aucune
 * pagination, pour la meme raison que {@link OperateursDuPupitre}.
 * </p>
 */
@FunctionalInterface
public interface SuivisOuvertsDuPupitre {
  List<SuiviDuPupitre> tous();
}
