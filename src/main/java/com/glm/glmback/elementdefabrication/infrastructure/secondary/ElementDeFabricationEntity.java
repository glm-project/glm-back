package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Categorie;
import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.Nom;
import com.glm.glmback.elementdefabrication.domain.Reference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "element_de_fabrication")
class ElementDeFabricationEntity {

  @Id
  private UUID id;

  @Column(length = 30)
  private String categorie;

  private String nom;

  private String reference;

  private String description;

  private Instant dateDeCreation;

  private Instant dateDeModification;

  protected ElementDeFabricationEntity() {
    // Constructeur requis par JPA.
  }

  private ElementDeFabricationEntity(ElementDeFabrication element) {
    id = element.id().uuid();
    categorie = element.categorie().value();
    nom = element.nom().value();
    reference = element.reference().map(Reference::value).orElse(null);
    description = element.description().map(Description::value).orElse(null);
    dateDeCreation = element.dateDeCreation();
    dateDeModification = element.dateDeModification();
  }

  static ElementDeFabricationEntity from(ElementDeFabrication element) {
    return new ElementDeFabricationEntity(element);
  }

  ElementDeFabrication toDomain() {
    return ElementDeFabrication.builder()
      .id(new ElementDeFabricationId(id))
      .categorie(new Categorie(categorie))
      .nom(new Nom(nom))
      .reference(reference)
      .description(description)
      .dateDeCreation(dateDeCreation)
      .dateDeModification(dateDeModification);
  }
}
