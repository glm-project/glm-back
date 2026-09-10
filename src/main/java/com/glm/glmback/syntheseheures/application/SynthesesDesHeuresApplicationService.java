package com.glm.glmback.syntheseheures.application;

import com.glm.glmback.syntheseheures.domain.FuseauHoraireDeLEntreprise;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.OperateursConnus;
import com.glm.glmback.syntheseheures.domain.PresenceDeLOperateur;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import com.glm.glmback.syntheseheures.domain.SyntheseDesHeures;
import com.glm.glmback.syntheseheures.domain.SynthesesDesHeuresService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture des syntheses des heures.
 *
 * <p>
 * Rien a ecrire : le contexte ne fait que relire les journaux de l'atelier. La transaction est donc en lecture
 * seule, et elle couvre les deux requetes du meme coup — journees puis journaux — pour que le releve ne melange pas
 * deux etats de la base.
 * </p>
 */
@Service
public class SynthesesDesHeuresApplicationService {

  private final SynthesesDesHeuresService synthesesDesHeures;

  public SynthesesDesHeuresApplicationService(
    PresenceDeLOperateur presences,
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau
  ) {
    this.synthesesDesHeures = new SynthesesDesHeuresService(presences, operateurs, fuseau);
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
