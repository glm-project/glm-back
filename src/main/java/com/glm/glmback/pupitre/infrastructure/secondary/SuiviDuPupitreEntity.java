package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.JournalDuPupitre;
import com.glm.glmback.pupitre.domain.NomDElement;
import com.glm.glmback.pupitre.domain.SuiviDuPupitre;
import com.glm.glmback.pupitre.domain.SuiviDuPupitreId;
import com.glm.glmback.pupitre.domain.TypeDElementEngage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des suivis d'atelier.
 *
 * <p>
 * La colonne de projection {@code etat} n'est pas mappee : la cloture est un fait, pas une projection, et
 * {@code clotureDateDeSurvenue} suffit a ecarter des la requete les elements qui n'acceptent plus de pointage. L'etat
 * rendu au pupitre, lui, se deduit du journal comme chez l'atelier.
 * </p>
 *
 * <p>
 * {@code elementId} est un identifiant nu, jamais une association : une entite voisine qui joindrait la table des
 * elements sur une colonne deja mappee par l'atelier ferait refuser le demarrage a Hibernate, qui n'admet pas deux
 * noms logiques pour une meme colonne physique. La reference est donc relue a part.
 * </p>
 */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviDuPupitreEntity {

  @Id
  private UUID id;

  private UUID elementId;

  private String elementNom;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private TypeDElementEngage elementType;

  private Instant clotureDateDeSurvenue;

  protected SuiviDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  UUID elementId() {
    return elementId;
  }

  SuiviDuPupitre toDomain(JournalDuPupitre journal, String reference) {
    return SuiviDuPupitre.builder()
      .id(new SuiviDuPupitreId(id))
      .nom(new NomDElement(elementNom))
      .reference(reference)
      .type(elementType)
      .journal(journal);
  }
}
