package com.glm.glmback.atelier.application;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Strategie « bornes de fin de journee », lot 8a : deux saisies simultanees sur la meme journee ou le meme OF ne
 * sont jamais un refus pour l'operateur. Le serveur rejoue lui-meme l'ecriture, dans une nouvelle transaction.
 */
@UnitTest
class SaisieConcurrenteRejoueeTest {

  private final TransactionTemplate transactions = new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class));

  @Test
  void shouldRejouerUneEcritureDevanceeParUneAutre() {
    AtomicInteger essais = new AtomicInteger();

    String resultat = SaisieConcurrenteRejouee.executer(transactions, () -> {
      if (essais.incrementAndGet() == 1) {
        throw new SaisieConcurrenteException(JourneeDeTravailId.newId());
      }
      return "enregistre";
    });

    assertThat(resultat).isEqualTo("enregistre");
    assertThat(essais).hasValue(2);
  }

  @Test
  void shouldRenoncerApresTroisEssais() {
    AtomicInteger essais = new AtomicInteger();

    assertThatThrownBy(() ->
      SaisieConcurrenteRejouee.executer(transactions, () -> {
        essais.incrementAndGet();
        throw new SaisieConcurrenteException(JourneeDeTravailId.newId());
      })
    ).isExactlyInstanceOf(SaisieConcurrenteException.class);
    assertThat(essais).hasValue(3);
  }

  @Test
  void shouldNeRejouerAucuneAutreErreur() {
    AtomicInteger essais = new AtomicInteger();

    assertThatThrownBy(() ->
      SaisieConcurrenteRejouee.executer(transactions, () -> {
        essais.incrementAndGet();
        throw new IllegalStateException("autre");
      })
    ).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(essais).hasValue(1);
  }
}
