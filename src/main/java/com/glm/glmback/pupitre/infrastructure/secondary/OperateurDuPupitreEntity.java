package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.Nom;
import com.glm.glmback.pupitre.domain.OperateurDuPupitre;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.Prenom;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des operateurs et de leurs habilitations.
 *
 * <p>
 * Le taux horaire n'est volontairement pas mappe : il ne doit pas atteindre un ecran d'atelier partage, et un champ
 * qu'on ne lit pas ne peut pas fuir par megarde. La table de jointure est lue directement, sans entite propre :
 * l'habilitation n'a ici aucune existence hors de la liste des postes d'un operateur.
 * </p>
 */
@Entity
@Immutable
@Table(name = "operateur")
class OperateurDuPupitreEntity {

  @Id
  private UUID id;

  private String nom;

  private String prenom;

  private String matricule;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "operateur_poste",
    joinColumns = @JoinColumn(name = "operateur_id"),
    inverseJoinColumns = @JoinColumn(name = "poste_id")
  )
  @OrderBy("libelle asc, id asc")
  private List<PosteDuPupitreEntity> postes = new ArrayList<>();

  protected OperateurDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  OperateurDuPupitre toDomain() {
    return OperateurDuPupitre.builder()
      .id(new OperateurId(id))
      .nom(new Nom(nom))
      .prenom(new Prenom(prenom))
      .matricule(matricule)
      .postes(postes.stream().map(PosteDuPupitreEntity::toDomain).toList());
  }
}
