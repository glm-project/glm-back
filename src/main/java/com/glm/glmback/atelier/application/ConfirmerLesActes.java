package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.UUID;
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
      var recu = existant.orElseThrow();
      if (!recu.reference().equals(reference) || !recu.preuve().contexte().correspondA(contexte)) {
        throw new ConfirmationReutiliseeException(commande);
      }
      return canonique(recu);
    }
    var preuve = references.read(reference);
    var avant = suivis.getForUpdate(suivi).orElseThrow(() -> new SuiviDAtelierIntrouvableException(suivi));
    var maintenant = clock.now();
    var dossierAvant = new LectureDossierConflit(preuve.adresse(), new LectureDuSuivi(avant, maintenant));
    var prepare = preparation.prepare(avant, preuve.acte(), preuve.evenement(), contexte.gestionnaire().auteur(), maintenant);
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

  private ResultatDActe canonique(RecuDActe recu) {
    var adresse = recu.preuve().adresse();
    var suivi = suivis.getForUpdate(adresse.suivi()).orElseThrow(() -> new SuiviDAtelierIntrouvableException(adresse.suivi()));
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
