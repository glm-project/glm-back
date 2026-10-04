package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import com.glm.glmback.atelier.application.gestionconflits.RecuDActe;
import com.glm.glmback.atelier.application.gestionconflits.RecusDActes;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.gestionconflits.ConfirmationReutiliseeException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
class JpaRecusDActes implements RecusDActes {

  private final EntityManager entities;

  JpaRecusDActes(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  public void create(RecuDActe recu) {
    var proposition = recu.proposition();
    int enregistre = entities
      .createNativeQuery(
        """
        insert into recu_d_acte
          (commande, suivi_id, pointage_id, sujet, emetteur, proposition, revision_de_depart,
           revision_enregistree, enregistre_le, activites_concernees, evenements_touches)
          values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          on conflict (commande) do nothing
        """
      )
      .setParameter(1, proposition.commande())
      .setParameter(2, proposition.adresse().suivi().uuid())
      .setParameter(3, proposition.adresse().pointage().uuid())
      .setParameter(4, recu.contexte().gestionnaire().sujet())
      .setParameter(5, recu.contexte().gestionnaire().emetteur())
      .setParameter(6, FormatDePropositionConfirmee.serialise(proposition, recu.contexte()))
      .setParameter(7, proposition.revision().value())
      .setParameter(8, recu.revisionEnregistree().value())
      .setParameter(9, recu.enregistreLe().toString())
      .setParameter(
        10,
        recu
          .activitesConcernees()
          .stream()
          .map(activite -> activite.uuid().toString())
          .sorted()
          .collect(Collectors.joining(","))
      )
      .setParameter(
        11,
        recu
          .evenementsTouches()
          .stream()
          .map(evenement -> evenement.uuid().toString())
          .collect(Collectors.joining(","))
      )
      .executeUpdate();
    if (enregistre == 0) {
      throw new ConfirmationReutiliseeException(proposition.commande());
    }
  }

  @Override
  public Optional<RecuDActe> get(UUID commande) {
    @SuppressWarnings("unchecked")
    List<Object[]> lignes = entities
      .createNativeQuery(
        """
        select proposition, revision_enregistree, enregistre_le, activites_concernees, evenements_touches
        from recu_d_acte where commande = ?
        """
      )
      .setParameter(1, commande)
      .getResultList();
    return lignes.stream().map(JpaRecusDActes::recu).findFirst();
  }

  private static RecuDActe recu(Object[] ligne) {
    var confirmee = FormatDePropositionConfirmee.relit((String) ligne[0]);
    return RecuDActe.builder()
      .proposition(confirmee.proposition())
      .contexte(confirmee.contexte())
      .revisionEnregistree(new RevisionDuSuivi(((Number) ligne[1]).longValue()))
      .enregistreLe(Instant.parse((String) ligne[2]))
      .activitesConcernees(
        Arrays.stream(((String) ligne[3]).split(","))
          .filter(id -> !id.isEmpty())
          .map(UUID::fromString)
          .map(ActiviteId::new)
          .collect(Collectors.toSet())
      )
      .evenementsTouches(Arrays.stream(((String) ligne[4]).split(",")).map(UUID::fromString).map(EvenementDAtelierId::new).toList());
  }
}
