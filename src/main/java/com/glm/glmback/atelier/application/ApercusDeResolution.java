package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.*;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApercusDeResolution {

  private final SuiviDAtelierRepository suivis;
  private final PreparationDesActes preparation;
  private final ReferencesDApercu references;
  private final Clock clock;
  private final ValiditeDesApercus validite;

  ApercusDeResolution(
    SuiviDAtelierRepository suivis,
    PreparationDesActes preparation,
    ReferencesDApercu references,
    Clock clock,
    ValiditeDesApercus validite
  ) {
    this.suivis = suivis;
    this.preparation = preparation;
    this.references = references;
    this.clock = clock;
    this.validite = validite;
  }

  public static SuivisBuilder builder() {
    return suivis ->
      preparation -> references -> clock -> validite -> new ApercusDeResolution(suivis, preparation, references, clock, validite);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public ApercuDeResolution apercu(
    UUID commande,
    AdresseDossierConflit adresse,
    RevisionDuSuivi revision,
    ActeDeResolution acte,
    ContexteDeResolution contexte
  ) {
    var suivi = suivis.get(adresse.suivi()).orElseThrow(() -> new SuiviDAtelierIntrouvableException(adresse.suivi()));
    if (!suivi.revision().equals(revision)) {
      throw new ApercuObsoleteException();
    }
    var maintenant = clock.now();
    var avant = new LectureDossierConflit(adresse, new LectureDuSuivi(suivi, maintenant));
    if (avant.kind() != EtatDAdresseDossier.EN_CONFLIT) {
      throw new ApercuObsoleteException();
    }
    Optional<EvenementDAtelierId> evenement =
      acte instanceof ActeDeResolution.Annulation ? Optional.empty() : Optional.of(EvenementDAtelierId.newId());
    var prepare = preparation.prepare(suivi, acte, evenement, contexte.gestionnaire().auteur(), maintenant);
    var preuve = PreuveDApercu.builder()
      .commande(commande)
      .adresse(adresse)
      .revision(suivi.revision())
      .contexte(contexte)
      .acte(acte)
      .evenement(evenement)
      .evaluation(maintenant)
      .expireLe(maintenant.plus(validite.validite()))
      .empreinteConsequences(prepare.empreinteConsequences());
    return new ApercuDeResolution(
      new ReferenceDApercu(preuve, references.issue(preuve)),
      avant,
      avant.apresActe(new LectureDuSuivi(prepare.apres(), maintenant))
    );
  }

  public interface SuivisBuilder {
    PreparationBuilder suivis(SuiviDAtelierRepository value);
  }

  public interface PreparationBuilder {
    ReferencesBuilder preparation(PreparationDesActes value);
  }

  public interface ReferencesBuilder {
    ClockBuilder references(ReferencesDApercu value);
  }

  public interface ClockBuilder {
    ValiditeBuilder clock(Clock value);
  }

  public interface ValiditeBuilder {
    ApercusDeResolution validite(ValiditeDesApercus value);
  }
}
