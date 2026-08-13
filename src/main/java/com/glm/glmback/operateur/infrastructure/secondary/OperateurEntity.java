package com.glm.glmback.operateur.infrastructure.secondary;

import com.glm.glmback.operateur.domain.Matricule;
import com.glm.glmback.operateur.domain.Nom;
import com.glm.glmback.operateur.domain.Operateur;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.PosteHabilitableId;
import com.glm.glmback.operateur.domain.Prenom;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "operateur")
class OperateurEntity {

  @Id
  private UUID id;

  @Column(length = 100)
  private String nom;

  @Column(length = 100)
  private String prenom;

  @Column(length = 50)
  private String matricule;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "operateur_poste", joinColumns = @JoinColumn(name = "operateur_id"))
  @Column(name = "poste_id")
  private Set<UUID> postes = new HashSet<>();

  protected OperateurEntity() {
    // Constructeur requis par JPA.
  }

  private OperateurEntity(Operateur operateur) {
    id = operateur.id().uuid();
    nom = operateur.nom().value();
    prenom = operateur.prenom().value();
    matricule = operateur.matricule().map(Matricule::value).orElse(null);
    postes = operateur.postes().stream().map(PosteHabilitableId::uuid).collect(Collectors.toSet());
  }

  static OperateurEntity from(Operateur operateur) {
    return new OperateurEntity(operateur);
  }

  Operateur toDomain() {
    return Operateur.builder()
      .id(new OperateurId(id))
      .nom(new Nom(nom))
      .prenom(new Prenom(prenom))
      .matricule(matricule)
      .postes(postes.stream().map(PosteHabilitableId::new).collect(Collectors.toSet()));
  }
}
