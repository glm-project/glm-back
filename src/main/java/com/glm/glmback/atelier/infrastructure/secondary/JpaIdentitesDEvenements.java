package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.application.AgregatDEvenement;
import com.glm.glmback.atelier.application.EmpreinteDEvenement;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.ReservationDEvenement;
import com.glm.glmback.atelier.application.TypeDAgregatDEvenement;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaIdentitesDEvenements implements IdentitesDEvenements {

  private final EntityManager entityManager;

  JpaIdentitesDEvenements(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public ReservationDEvenement reserve(UUID evenement, EmpreinteDEvenement empreinte) {
    if (insere(evenement, empreinte)) {
      return ReservationDEvenement.inedite();
    }

    Object[] ligne = ligne(evenement);
    if (!Boolean.TRUE.equals(ligne[0]) || !correspond(ligne, empreinte)) {
      throw new IdentifiantDEvenementReutiliseException(evenement);
    }
    return ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.valueOf((String) ligne[7]), (UUID) ligne[8]));
  }

  @Override
  public boolean reserveHorsPupitre(UUID evenement) {
    return (
      entityManager
        .createNativeQuery("insert into identite_evenement_atelier (id, rejouable) values (?, false) on conflict (id) do nothing")
        .setParameter(1, evenement)
        .executeUpdate()
      == 1
    );
  }

  @Override
  public void associe(UUID evenement, AgregatDEvenement agregat) {
    entityManager
      .createNativeQuery("update identite_evenement_atelier set type_agregat = ?, agregat_id = ? where id = ?")
      .setParameter(1, agregat.type().name())
      .setParameter(2, agregat.id())
      .setParameter(3, evenement)
      .executeUpdate();
  }

  private boolean insere(UUID evenement, EmpreinteDEvenement empreinte) {
    return (
      entityManager
        .createNativeQuery(
          """
          insert into identite_evenement_atelier
            (id, rejouable, nature, cible_id, operateur_id, type_evenement, poste_id, date_de_survenue_fournie, date_de_survenue)
          values (?, true, ?, ?, ?, ?, ?, ?, ?)
          on conflict (id) do nothing
          """
        )
        .setParameter(1, evenement)
        .setParameter(2, empreinte.nature().name())
        .setParameter(3, empreinte.cible().orElse(null))
        .setParameter(4, empreinte.operateur())
        .setParameter(5, empreinte.type())
        .setParameter(6, empreinte.poste().orElse(null))
        .setParameter(7, empreinte.dateDeSurvenue().isPresent())
        .setParameter(8, empreinte.dateDeSurvenue().orElse(null))
        .executeUpdate()
      == 1
    );
  }

  private Object[] ligne(UUID evenement) {
    @SuppressWarnings("unchecked")
    List<Object[]> lignes = entityManager
      .createNativeQuery(
        """
        select rejouable, nature, cible_id, operateur_id, type_evenement, poste_id, date_de_survenue_fournie,
               type_agregat, agregat_id, date_de_survenue
        from identite_evenement_atelier where id = ?
        """
      )
      .setParameter(1, evenement)
      .getResultList();
    return lignes.getFirst();
  }

  private static boolean correspond(Object[] ligne, EmpreinteDEvenement empreinte) {
    return empreinte.equals(
      new EmpreinteDEvenement(
        com.glm.glmback.atelier.application.NatureDeGesteDuPupitre.valueOf((String) ligne[1]),
        Optional.ofNullable((UUID) ligne[2]),
        (UUID) ligne[3],
        (String) ligne[4],
        Optional.ofNullable((UUID) ligne[5]),
        Optional.ofNullable((java.time.Instant) ligne[9])
      )
    );
  }
}
