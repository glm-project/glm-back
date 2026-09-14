package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class EtatDActiviteTest {

  @Test
  void shouldOuvrirUneActiviteAvecUnDebut() {
    assertThat(EtatDActivite.ABSENTE.apres(TypeDePointage.DEBUT)).contains(EtatDActivite.EN_COURS);
  }

  @Test
  void shouldOuvrirUneActiviteDirectementEnNonConformite() {
    assertThat(EtatDActivite.ABSENTE.apres(TypeDePointage.NON_CONFORMITE)).contains(EtatDActivite.EN_NON_CONFORMITE);
  }

  @Test
  void shouldRefuserUneFinSansActivite() {
    assertThat(EtatDActivite.ABSENTE.apres(TypeDePointage.FIN)).isEmpty();
  }

  @Test
  void shouldBasculerEnNonConformiteSansFermerLActivite() {
    assertThat(EtatDActivite.EN_COURS.apres(TypeDePointage.NON_CONFORMITE)).contains(EtatDActivite.EN_NON_CONFORMITE);
  }

  @Test
  void shouldFermerUneActiviteEnCours() {
    assertThat(EtatDActivite.EN_COURS.apres(TypeDePointage.FIN)).contains(EtatDActivite.ABSENTE);
  }

  @Test
  void shouldRefuserDeuxDebutsConsecutifs() {
    assertThat(EtatDActivite.EN_COURS.apres(TypeDePointage.DEBUT)).isEmpty();
  }

  /**
   * Il n'existe pas de type « reprise » : reprendre du bon travail apres une non conformite se pointe comme un debut.
   */
  @Test
  void shouldReprendreLeBonTravailParUnDebut() {
    assertThat(EtatDActivite.EN_NON_CONFORMITE.apres(TypeDePointage.DEBUT)).contains(EtatDActivite.EN_COURS);
  }

  @Test
  void shouldFermerUneActiviteEnNonConformite() {
    assertThat(EtatDActivite.EN_NON_CONFORMITE.apres(TypeDePointage.FIN)).contains(EtatDActivite.ABSENTE);
  }

  @Test
  void shouldRefuserDeuxNonConformitesConsecutives() {
    assertThat(EtatDActivite.EN_NON_CONFORMITE.apres(TypeDePointage.NON_CONFORMITE)).isEmpty();
  }

  @Test
  void shouldNAvoirAucuneCategorieSansActivite() {
    assertThat(EtatDActivite.ABSENTE.categorie()).isEmpty();
  }

  @Test
  void shouldCompterUneActiviteEnCoursCommeDuTravail() {
    assertThat(EtatDActivite.EN_COURS.categorie()).contains(CategorieDActivite.TRAVAIL);
  }

  @Test
  void shouldCompterUneActiviteEnNonConformiteAPart() {
    assertThat(EtatDActivite.EN_NON_CONFORMITE.categorie()).contains(CategorieDActivite.NON_CONFORMITE);
  }
}
