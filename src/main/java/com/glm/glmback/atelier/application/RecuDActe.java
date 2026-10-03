package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Preuve durable d'un acte enregistre, independante de la validite ulterieure de l'apercu. */
public record RecuDActe(
  PreuveDApercu preuve,
  String reference,
  RevisionDuSuivi revisionEnregistree,
  Instant enregistreLe,
  Set<ActiviteId> activitesConcernees,
  List<EvenementDAtelierId> evenementsTouches
) {
  public RecuDActe {
    Assert.notNull("preuve", preuve);
    Assert.notBlank("reference", reference);
    Assert.notNull("revision enregistree", revisionEnregistree);
    Assert.notNull("enregistre le", enregistreLe);
    Assert.field("activites concernees", activitesConcernees).notNull().noNullElement();
    Assert.field("evenements touches", evenementsTouches).notNull().noNullElement();
    activitesConcernees = Set.copyOf(activitesConcernees);
    evenementsTouches = List.copyOf(evenementsTouches);
  }

  public static PreuveBuilder builder() {
    return preuve ->
      reference ->
        revision -> instant -> activites -> evenements -> new RecuDActe(preuve, reference, revision, instant, activites, evenements);
  }

  public interface PreuveBuilder {
    ReferenceBuilder preuve(PreuveDApercu preuve);
  }

  public interface ReferenceBuilder {
    RevisionBuilder reference(String reference);
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
