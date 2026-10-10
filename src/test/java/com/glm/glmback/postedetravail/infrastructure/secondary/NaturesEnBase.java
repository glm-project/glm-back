package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La cle etrangere du poste impose que sa nature existe dans le referentiel. Les tests partagent le schema de leur
 * entreprise : la nature n'y est inseree que si elle manque.
 */
public final class NaturesEnBase {

  private NaturesEnBase() {}

  public static NatureDuPoste nature(EntityManager entities, TransactionTemplate transactions, String libelle) {
    return transactions.execute(status -> {
      entities
        .createNativeQuery("insert into nature_de_travail (id, libelle, cle) values (:id, :libelle, :cle) on conflict (cle) do nothing")
        .setParameter("id", UUID.randomUUID())
        .setParameter("libelle", libelle)
        .setParameter("cle", libelle.toLowerCase(Locale.ROOT))
        .executeUpdate();
      Tuple ligne = (Tuple) entities
        .createNativeQuery("select id, libelle from nature_de_travail where cle = :cle", Tuple.class)
        .setParameter("cle", libelle.toLowerCase(Locale.ROOT))
        .getSingleResult();

      return new NatureDuPoste(new NatureDeTravailId(ligne.get("id", UUID.class)), new NatureDeTravail(ligne.get("libelle", String.class)));
    });
  }
}
