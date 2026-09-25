package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deux saisies simultanees sur la meme journee ou le meme OF ne sont jamais un refus pour l'operateur (lot 8a de la
 * strategie « bornes de fin de journee ») : l'ecriture devancee est rejouee, chaque fois dans une nouvelle
 * transaction, qui relit l'agregat a jour.
 *
 * <p>
 * Trois essais suffisent : le verrou pose a l'ecriture met les redacteurs en file, et un troisieme echec trahirait
 * autre chose qu'une simple course. L'exception remonte alors telle quelle.
 * </p>
 */
final class SaisieConcurrenteRejouee {

  private static final int ESSAIS = 3;

  private SaisieConcurrenteRejouee() {}

  static <T> T executer(TransactionTemplate transactions, Supplier<T> ecriture) {
    for (int essai = 1; ; essai++) {
      try {
        return transactions.execute(status -> ecriture.get());
      } catch (SaisieConcurrenteException e) {
        if (essai == ESSAIS) {
          throw e;
        }
      }
    }
  }
}
