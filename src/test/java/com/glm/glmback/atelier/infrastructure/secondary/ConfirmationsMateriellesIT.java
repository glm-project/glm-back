package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.ApercusDeResolution;
import com.glm.glmback.atelier.application.ConfirmerLesActes;
import com.glm.glmback.atelier.application.RecusDActes;
import com.glm.glmback.atelier.domain.AdresseDossierConflit;
import com.glm.glmback.atelier.domain.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ConfirmationsMateriellesIT {

  @Autowired
  private ApercusDeResolution apercus;

  @Autowired
  private ConfirmerLesActes confirmations;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private RecusDActes recus;

  @Autowired
  private TransactionTemplate transactions;

  @MockitoBean
  private Clock clock;

  @MockitoBean
  private OperateursConnus operateurs;

  @MockitoBean
  private PostesConnus postes;

  @MockitoBean
  private Habilitations habilitations;

  @BeforeEach
  void referentiel() {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(Optional.of(POSTE_CONNU_FRAISEUSE_1));
    when(habilitations.estHabilite(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1)).thenReturn(true);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnApercuEncoreValideQuandLActiviteFranchitSonEcheanceSansEcriture() {
    // GIVEN
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T20:59:59Z"));
    var suivi = transactions.execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var acte = preuveDAnnulationDeTransition(suivi);
    var commande = UUID.randomUUID();
    var apercu = apercus.apercu(commande, acte.adresse(), suivi.revision(), acte.acte(), CONTEXTE_LEROY_IMPECCMOLD);
    assertThat(apercu.apres().activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).isEmpty();
        assertThat(activite.finAutomatique()).isFalse();
      });
    assertThat(apercu.reference().preuve().expireLe()).isEqualTo(Instant.parse("2026-05-10T21:14:59Z"));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T21:00:00Z"));
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), commande, apercu.reference().opaque(), CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    var relu = transactions.execute(status -> suivis.get(suivi.id()));
    var recu = transactions.execute(status -> recus.get(commande));
    assertThat(relu).contains(suivi);
    assertThat(recu).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(ReferenceRetiree.class)
  @WithTenant("impeccmold")
  void shouldDeclarerLApercuObsoleteQuandUneReferenceValideeDisparait(ReferenceRetiree reference) {
    // GIVEN
    var suivi = transactions.execute(status -> suivis.create(MaterielDesActesFixture.suiviAvecTransitionSurFraiseuse()));
    var commande = UUID.randomUUID();
    var adresse = new AdresseDossierConflit(suivi.id(), suivi.journal().evenements().getLast().id());
    var acte = MaterielDesActesFixture.correctionDeTransitionEnFin(suivi);
    var apercu = apercus.apercu(commande, adresse, suivi.revision(), acte, CONTEXTE_LEROY_IMPECCMOLD);
    if (reference == ReferenceRetiree.OPERATEUR) {
      when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.empty());
    } else {
      when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(Optional.empty());
    }
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), commande, apercu.reference().opaque(), CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    var relu = transactions.execute(status -> suivis.get(suivi.id()));
    var recu = transactions.execute(status -> recus.get(commande));
    assertThat(relu).contains(suivi);
    assertThat(recu).isEmpty();
    assertThatThrownBy(() ->
      apercus.apercu(UUID.randomUUID(), adresse, suivi.revision(), acte, CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(
      reference == ReferenceRetiree.OPERATEUR ? OperateurDAtelierIntrouvableException.class : PosteDAtelierIntrouvableException.class
    );
  }

  @ParameterizedTest
  @EnumSource(ValeurCopiee.class)
  @WithTenant("impeccmold")
  void shouldRefuserLaReferenceReelleSiUneValeurCopieeDuReferentielChange(ValeurCopiee valeur) {
    // GIVEN
    var suivi = transactions.execute(status -> suivis.create(MaterielDesActesFixture.suiviAvecTransitionSurFraiseuse()));
    var commande = UUID.randomUUID();
    var adresse = new AdresseDossierConflit(suivi.id(), suivi.journal().evenements().getLast().id());
    var apercu = apercus.apercu(
      commande,
      adresse,
      suivi.revision(),
      MaterielDesActesFixture.correctionDeTransitionEnFin(suivi),
      CONTEXTE_LEROY_IMPECCMOLD
    );
    switch (valeur) {
      case TAUX -> when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(
        Optional.of(MaterielDesActesFixture.dupontAvecTaux(new BigDecimal("23.00")))
      );
      case COUT -> when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(
        Optional.of(MaterielDesActesFixture.fraiseuseAvecCout(new BigDecimal("46.50")))
      );
      case NATURE -> when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(
        Optional.of(MaterielDesActesFixture.fraiseuseAvecNature(NATURE_TOURNAGE))
      );
    }
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), commande, apercu.reference().opaque(), CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    var relu = transactions.execute(status -> suivis.get(suivi.id()));
    var recu = transactions.execute(status -> recus.get(commande));
    assertThat(relu).contains(suivi);
    assertThat(recu).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldDeclarerLApercuObsoleteQuandLHabilitationEstRetireeApresSaPreparation() {
    // GIVEN
    var suivi = transactions.execute(status -> suivis.create(MaterielDesActesFixture.suiviAvecTransitionSurFraiseuse()));
    var commande = UUID.randomUUID();
    var adresse = new AdresseDossierConflit(suivi.id(), suivi.journal().evenements().getLast().id());
    var apercu = apercus.apercu(
      commande,
      adresse,
      suivi.revision(),
      MaterielDesActesFixture.correctionDeTransitionEnFin(suivi),
      CONTEXTE_LEROY_IMPECCMOLD
    );
    when(habilitations.estHabilite(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1)).thenReturn(false);
    assertThatThrownBy(() ->
      apercus.apercu(
        UUID.randomUUID(),
        adresse,
        suivi.revision(),
        MaterielDesActesFixture.correctionDeTransitionEnFin(suivi),
        CONTEXTE_LEROY_IMPECCMOLD
      )
    ).isExactlyInstanceOf(OperateurNonHabiliteException.class);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), commande, apercu.reference().opaque(), CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    var relu = transactions.execute(status -> suivis.get(suivi.id()));
    var recu = transactions.execute(status -> recus.get(commande));
    assertThat(relu).contains(suivi);
    assertThat(recu).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldGarderUnePanneReferentielleTechniqueLorsDeLaConfirmation() {
    // GIVEN
    var suivi = transactions.execute(status -> suivis.create(MaterielDesActesFixture.suiviAvecTransitionSurFraiseuse()));
    var commande = UUID.randomUUID();
    var adresse = new AdresseDossierConflit(suivi.id(), suivi.journal().evenements().getLast().id());
    var apercu = apercus.apercu(
      commande,
      adresse,
      suivi.revision(),
      MaterielDesActesFixture.correctionDeTransitionEnFin(suivi),
      CONTEXTE_LEROY_IMPECCMOLD
    );
    var indisponible = new IllegalStateException("Referentiel indisponible");
    when(habilitations.estHabilite(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1)).thenThrow(indisponible);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), commande, apercu.reference().opaque(), CONTEXTE_LEROY_IMPECCMOLD)
    ).isSameAs(indisponible);
    var relu = transactions.execute(status -> suivis.get(suivi.id()));
    var recu = transactions.execute(status -> recus.get(commande));
    assertThat(relu).contains(suivi);
    assertThat(recu).isEmpty();
  }

  private enum ValeurCopiee {
    TAUX,
    COUT,
    NATURE,
  }

  private enum ReferenceRetiree {
    OPERATEUR,
    POSTE,
  }
}
