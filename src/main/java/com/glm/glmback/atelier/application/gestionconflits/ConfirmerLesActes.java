package com.glm.glmback.atelier.application.gestionconflits;

import com.glm.glmback.atelier.application.AgregatDEvenement;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.TypeDAgregatDEvenement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.gestionconflits.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.gestionconflits.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.gestionconflits.EtatDAdresseDossier;
import com.glm.glmback.atelier.domain.gestionconflits.LectureDossierConflit;
import com.glm.glmback.atelier.domain.gestionconflits.PropositionInvalideException;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmerLesActes {

  private final SuiviDAtelierRepository suivis;
  private final RecusDActes recus;
  private final PreparationDesActes preparation;
  private final IdentitesDEvenements identites;
  private final Clock clock;

  ConfirmerLesActes(
    SuiviDAtelierRepository suivis,
    RecusDActes recus,
    PreparationDesActes preparation,
    IdentitesDEvenements identites,
    Clock clock
  ) {
    this.suivis = suivis;
    this.recus = recus;
    this.preparation = preparation;
    this.identites = identites;
    this.clock = clock;
  }

  public static SuivisBuilder builder() {
    return suivis -> recus -> preparation -> identites -> clock -> new ConfirmerLesActes(suivis, recus, preparation, identites, clock);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ResultatDActe confirmer(SuiviDAtelierId suivi, PropositionAConfirmer proposition, ContexteDeResolution contexte) {
    if (!proposition.adresse().suivi().equals(suivi)) {
      throw new PropositionInvalideException();
    }
    var commande = proposition.commande();
    var existant = recus.get(commande);
    if (existant.isPresent()) {
      return rejoue(existant.orElseThrow(), suivi, proposition, contexte);
    }
    var avant = verrouille(suivi);
    var terminePendantLAttente = recus.get(commande);
    if (terminePendantLAttente.isPresent()) {
      return rejoue(terminePendantLAttente.orElseThrow(), suivi, proposition, contexte);
    }
    if (!avant.revision().equals(proposition.revision())) {
      throw new ApercuObsoleteException();
    }
    var maintenant = clock.now();
    var dossierAvant = new LectureDossierConflit(proposition.adresse(), new LectureDuSuivi(avant, maintenant));
    if (dossierAvant.kind() != EtatDAdresseDossier.EN_CONFLIT) {
      throw new ApercuObsoleteException();
    }
    ActePrepare prepare;
    try {
      prepare = preparation.prepare(avant, proposition.acte(), proposition.evenement(), contexte.gestionnaire().auteur(), maintenant);
    } catch (OperateurNonHabiliteException | OperateurDAtelierIntrouvableException | PosteDAtelierIntrouvableException refus) {
      throw new ApercuObsoleteException();
    }
    if (!prepare.empreinteConsequences().equals(proposition.empreinteConsequences())) {
      throw new ApercuObsoleteException();
    }
    proposition
      .evenement()
      .ifPresent(evenement -> {
        if (!identites.reserveHorsPupitre(evenement.uuid())) {
          throw new ApercuObsoleteException();
        }
      });
    var enregistre = suivis.update(prepare.apres());
    proposition
      .evenement()
      .ifPresent(evenement ->
        identites.associe(evenement.uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, enregistre.id().uuid()))
      );
    var dossier = dossierAvant.apresActe(new LectureDuSuivi(enregistre, maintenant));
    var touches = enregistre
      .journal()
      .evenements()
      .stream()
      .filter(evenement -> !avant.journal().evenement(evenement.id()).filter(evenement::equals).isPresent())
      .map(EvenementDAtelier::id)
      .toList();
    var recu = RecuDActe.builder()
      .proposition(proposition)
      .contexte(contexte)
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
    return recus.get(commande).map(recu -> rejoue(recu, suivi, recu.proposition(), contexte));
  }

  private ResultatDActe rejoue(RecuDActe recu, SuiviDAtelierId suivi, PropositionAConfirmer proposition, ContexteDeResolution contexte) {
    if (
      !recu.proposition().adresse().suivi().equals(suivi)
      || !recu.proposition().memeDemandeQue(proposition)
      || !recu.contexte().correspondA(contexte)
    ) {
      throw new ConfirmationReutiliseeException(recu.proposition().commande());
    }
    return canonique(recu);
  }

  private SuiviDAtelier verrouille(SuiviDAtelierId suivi) {
    return suivis.getForUpdate(suivi).orElseThrow(() -> new SuiviDAtelierIntrouvableException(suivi));
  }

  private ResultatDActe canonique(RecuDActe recu) {
    var adresse = recu.proposition().adresse();
    var suivi = verrouille(adresse.suivi());
    return new ResultatDActe(recu, new LectureDossierConflit(adresse, new LectureDuSuivi(suivi, clock.now()), recu.activitesConcernees()));
  }

  public interface SuivisBuilder {
    RecusBuilder suivis(SuiviDAtelierRepository value);
  }

  public interface RecusBuilder {
    PreparationBuilder recus(RecusDActes value);
  }

  public interface PreparationBuilder {
    IdentitesBuilder preparation(PreparationDesActes value);
  }

  public interface IdentitesBuilder {
    ClockBuilder identites(IdentitesDEvenements value);
  }

  public interface ClockBuilder {
    ConfirmerLesActes clock(Clock value);
  }
}
