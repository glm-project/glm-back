package com.glm.glmback.syntheseheures.application;

import com.glm.glmback.shared.time.domain.Clock;
import com.glm.glmback.syntheseheures.domain.ActivitesDeLOperateur;
import com.glm.glmback.syntheseheures.domain.ElementsDeFabrication;
import com.glm.glmback.syntheseheures.domain.FuseauHoraireDeLEntreprise;
import com.glm.glmback.syntheseheures.domain.JournalDeLOperateur;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.OperateursConnus;
import com.glm.glmback.syntheseheures.domain.PostesDeTravail;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import com.glm.glmback.syntheseheures.domain.SyntheseDesHeures;
import com.glm.glmback.syntheseheures.domain.SynthesesDesHeuresService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture des activites, du journal et du referentiel dans une transaction en lecture seule.
 * Cette transaction ne garantit pas un instantane face aux ecritures concurrentes.
 */
@Service
public class SynthesesDesHeuresApplicationService {

  private final SynthesesDesHeuresService synthesesDesHeures;

  public SynthesesDesHeuresApplicationService(
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    ActivitesDeLOperateur activites,
    JournalDeLOperateur journal,
    ElementsDeFabrication elements,
    PostesDeTravail postes,
    Clock clock
  ) {
    this.synthesesDesHeures = SynthesesDesHeuresService.builder()
      .operateurs(operateurs)
      .fuseau(fuseau)
      .activites(activites)
      .journal(journal)
      .elements(elements)
      .postes(postes)
      .clock(clock);
  }

  /**
   * La lecture est ouverte aux deux roles metier, comme la feuille de temps : ce releve affiche du temps, pas un
   * montant — rien ne justifie de le reserver au gestionnaire comme {@code coutderevient}, qui derive des taux
   * horaires. {@code ADMIN} n'a aucun acces metier.
   */
  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public SyntheseDesHeures synthese(OperateurId operateur, SemaineCalendaire semaine) {
    return synthesesDesHeures.synthese(operateur, semaine);
  }
}
