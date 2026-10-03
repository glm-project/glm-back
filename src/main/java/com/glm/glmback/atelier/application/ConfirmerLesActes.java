package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.ApercuInvalideException;
import com.glm.glmback.atelier.domain.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.UUID;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmerLesActes {

  private final SuiviDAtelierRepository suivis;
  private final RecusDActes recus;
  private final ReferencesDApercu references;
  private final PreparationDesActes preparation;
  private final Clock clock;

  ConfirmerLesActes(
    SuiviDAtelierRepository suivis,
    RecusDActes recus,
    ReferencesDApercu references,
    PreparationDesActes preparation,
    Clock clock
  ) {
    this.suivis = suivis;
    this.recus = recus;
    this.references = references;
    this.preparation = preparation;
    this.clock = clock;
  }

  public static SuivisBuilder builder() {
    return suivis -> recus -> references -> preparation -> clock -> new ConfirmerLesActes(suivis, recus, references, preparation, clock);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ResultatDActe confirmer(SuiviDAtelierId suivi, UUID commande, String reference, ContexteDeResolution contexte) {
    var existant = recus.get(commande);
    if (existant.isPresent()) {
      return rejoue(existant.orElseThrow(), suivi, reference, contexte);
    }
    var preuve = references.read(reference);
    if (
      !preuve.commande().equals(commande) || !preuve.adresse().suivi().equals(suivi) || !preuve.contexte().correspondA(contexte)
    ) {
      throw new ApercuInvalideException();
    }
    var avant = verrouille(suivi);
    var terminePendantLAttente = recus.get(commande);
    if (terminePendantLAttente.isPresent()) {
      return rejoue(terminePendantLAttente.orElseThrow(), suivi, reference, contexte);
    }
    if (!avant.revision().equals(preuve.revision())) {
      throw new ApercuObsoleteException();
    }
    var maintenant = clock.now();
    if (!maintenant.isBefore(preuve.expireLe())) {
      throw new ApercuObsoleteException();
    }
    var dossierAvant = new LectureDossierConflit(preuve.adresse(), new LectureDuSuivi(avant, maintenant));
    var prepare = preparation.prepare(avant, preuve.acte(), preuve.evenement(), contexte.gestionnaire().auteur(), maintenant);
    if (!prepare.empreinteConsequences().equals(preuve.empreinteConsequences())) {
      throw new ApercuObsoleteException();
    }
    var enregistre = suivis.update(prepare.apres());
    var dossier = dossierAvant.apresActe(new LectureDuSuivi(enregistre, maintenant));
    var touches = enregistre
      .journal()
      .evenements()
      .stream()
      .filter(evenement -> !avant.journal().evenement(evenement.id()).filter(evenement::equals).isPresent())
      .map(EvenementDAtelier::id)
      .toList();
    var recu = RecuDActe.builder()
      .preuve(preuve)
      .reference(reference)
      .revisionEnregistree(enregistre.revision())
      .enregistreLe(maintenant)
      .activitesConcernees(dossier.concernees())
      .evenementsTouches(touches);
    recus.create(recu);
    return new ResultatDActe(recu, dossier);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public Optional<ResultatDActe> verifier(SuiviDAtelierId suivi, UUID commande, ContexteDeResolution contexte) {
    return recus.get(commande).map(recu -> rejoue(recu, suivi, recu.reference(), contexte));
  }

  private ResultatDActe rejoue(RecuDActe recu, SuiviDAtelierId suivi, String reference, ContexteDeResolution contexte) {
    if (
      !recu.preuve().adresse().suivi().equals(suivi) ||
      !recu.reference().equals(reference) ||
      !recu.preuve().contexte().correspondA(contexte)
    ) {
      throw new ConfirmationReutiliseeException(recu.preuve().commande());
    }
    return canonique(recu);
  }

  private SuiviDAtelier verrouille(SuiviDAtelierId suivi) {
    return suivis.getForUpdate(suivi).orElseThrow(() -> new SuiviDAtelierIntrouvableException(suivi));
  }

  private ResultatDActe canonique(RecuDActe recu) {
    var adresse = recu.preuve().adresse();
    var suivi = verrouille(adresse.suivi());
    return new ResultatDActe(recu, new LectureDossierConflit(adresse, new LectureDuSuivi(suivi, clock.now()), recu.activitesConcernees()));
  }

  public interface SuivisBuilder {
    RecusBuilder suivis(SuiviDAtelierRepository value);
  }

  public interface RecusBuilder {
    ReferencesBuilder recus(RecusDActes value);
  }

  public interface ReferencesBuilder {
    PreparationBuilder references(ReferencesDApercu value);
  }

  public interface PreparationBuilder {
    ClockBuilder preparation(PreparationDesActes value);
  }

  public interface ClockBuilder {
    ConfirmerLesActes clock(Clock value);
  }
}
