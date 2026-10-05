package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Preuve durable d'un acte enregistre, independante de la validite ulterieure de l'apercu.
 *
 * <p>
 * Les evenements touches comprennent les faits modifies et les faits crees : une annulation modifie un fait,
 * une regularisation en cree un, et une correction modifie l'original et cree son remplacant. Cette liste est donc
 * toujours non vide pour les trois actes enregistres par {@link ConfirmerLesActes}.
 * </p>
 */
public record RecuDActe(
  PropositionAConfirmer proposition,
  ContexteDeResolution contexte,
  RevisionDuSuivi revisionEnregistree,
  Instant enregistreLe,
  Set<ActiviteId> activitesConcernees,
  List<EvenementDAtelierId> evenementsTouches
) {
  public RecuDActe {
    Assert.notNull("proposition", proposition);
    Assert.notNull("contexte", contexte);
    Assert.notNull("revision enregistree", revisionEnregistree);
    Assert.notNull("enregistre le", enregistreLe);
    Assert.field("activites concernees", activitesConcernees).notNull().noNullElement();
    Assert.field("evenements touches", evenementsTouches).notNull().noNullElement();
    activitesConcernees = Set.copyOf(activitesConcernees);
    evenementsTouches = List.copyOf(evenementsTouches);
  }

  public static PropositionBuilder builder() {
    return proposition ->
      contexte ->
        revision -> instant -> activites -> evenements -> new RecuDActe(proposition, contexte, revision, instant, activites, evenements);
  }

  public interface PropositionBuilder {
    ContexteBuilder proposition(PropositionAConfirmer proposition);
  }

  public interface ContexteBuilder {
    RevisionBuilder contexte(ContexteDeResolution contexte);
  }

  public interface RevisionBuilder {
    InstantBuilder revisionEnregistree(RevisionDuSuivi revision);
  }

  public interface InstantBuilder {
    ActivitesBuilder enregistreLe(Instant instant);
  }

  public interface ActivitesBuilder {
    EvenementsBuilder activitesConcernees(Set<ActiviteId> activites);
  }

  public interface EvenementsBuilder {
    RecuDActe evenementsTouches(List<EvenementDAtelierId> evenements);
  }
}
