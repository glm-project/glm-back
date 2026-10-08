package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce que la regle de reception decide d'un pointage : l'accepter, ou l'ignorer pour l'une de ses quatre raisons.
 */
public sealed interface VerdictDeReception {
  /**
   * Un pointage accepte entre au journal. Une fin termine l'activite en cours de sa cle, que ce verdict designe ; un
   * debut ou une non conformite en ouvre une nouvelle et n'en designe aucune.
   */
  record Accepte(Optional<ActiviteId> activiteTerminee) implements VerdictDeReception {
    public Accepte {
      Assert.notNull("activite terminee", activiteTerminee);
    }
  }

  /**
   * Un pointage ignore ne change rien au journal. Il garde la raison, et le dernier pointage accepte de sa cle auquel la
   * regle l'a compare, absent quand la cle n'a encore aucun pointage accepte.
   */
  record Ignore(RaisonDePointageIgnore raison, Optional<EvenementDAtelierId> dernierAccepte) implements VerdictDeReception {
    public Ignore {
      Assert.notNull("raison", raison);
      Assert.notNull("dernier pointage accepte", dernierAccepte);
    }
  }
}
