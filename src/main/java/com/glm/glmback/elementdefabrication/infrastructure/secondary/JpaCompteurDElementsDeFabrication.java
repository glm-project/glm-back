package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Annee;
import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * L'increment est fait en une seule instruction, sans lecture prealable : deux creations concurrentes
 * ne peuvent pas obtenir le meme numero. La table n'etant pas qualifiee, elle est resolue dans le
 * schema du tenant courant, positionne par Hibernate sur la connexion.
 */
@Component
class JpaCompteurDElementsDeFabrication implements CompteurDElementsDeFabrication {

  private static final String PROCHAIN_NUMERO = """
    INSERT INTO compteur_d_elements_de_fabrication (type, annee, numero) VALUES (?, ?, 1) \
    ON CONFLICT (type, annee) DO UPDATE SET numero = compteur_d_elements_de_fabrication.numero + 1 \
    RETURNING numero\
    """;

  private final EntityManager entityManager;

  JpaCompteurDElementsDeFabrication(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public long prochainNumero(TypeDElementDeFabrication type, Annee annee) {
    return entityManager.unwrap(Session.class).doReturningWork(connection -> prochainNumero(connection, type, annee));
  }

  private static long prochainNumero(Connection connection, TypeDElementDeFabrication type, Annee annee) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(PROCHAIN_NUMERO)) {
      statement.setString(1, type.name());
      statement.setInt(2, annee.value());

      try (ResultSet numeros = statement.executeQuery()) {
        numeros.next();

        return numeros.getLong(1);
      }
    }
  }
}
