package com.glm.glmback.pupitre.application;

import com.glm.glmback.pupitre.domain.OperateursDuPupitre;
import com.glm.glmback.pupitre.domain.ReferentielDuPupitre;
import com.glm.glmback.pupitre.domain.ReferentielsDuPupitreService;
import com.glm.glmback.pupitre.domain.SuivisOuvertsDuPupitre;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture du referentiel du pupitre.
 *
 * <p>
 * Rien a ecrire : le contexte ne fait que relire les tables des referentiels et le journal de l'atelier.
 * </p>
 */
@Service
public class ReferentielsDuPupitreApplicationService {

  private final ReferentielsDuPupitreService referentiels;

  public ReferentielsDuPupitreApplicationService(OperateursDuPupitre operateurs, SuivisOuvertsDuPupitre suivis, Clock clock) {
    this.referentiels = new ReferentielsDuPupitreService(operateurs, suivis, clock);
  }

  /**
   * La lecture se fait a l'isolation par defaut, {@code READ COMMITTED}.
   *
   * <p>
   * Cette methode a demande {@code REPEATABLE_READ} de sa livraison a la correction de #36 : chaque requete prenant
   * son propre instantane sous {@code READ COMMITTED}, la lecture des operateurs peut ignorer un operateur que la
   * lecture suivante des suivis designe dans une activite en cours. L'ecart est reel, il est desormais assume : la
   * demande d'isolation ne pouvait pas etre honoree, et elle rendait la route inutilisable.
   * </p>
   *
   * <p>
   * Hibernate pose le schema du tenant par {@code Connection.setSchema} des l'acquisition de la connexion. Ce
   * dialogue passe par le protocole etendu, donc prend un instantane : PostgreSQL refuse ensuite tout {@code SET
   * TRANSACTION ISOLATION LEVEL} par un {@code must be called before any query}, et la route repondait {@code 500} a
   * chaque appel. Seul l'ordre inverse fonctionne -- isolation d'abord, schema ensuite --, ce qui exige de
   * s'approprier l'acquisition de connexion par un {@code MultiTenantConnectionProvider} maison, dans le chemin meme
   * qui garantit l'etancheite entre entreprises clientes. Un defaut de {@code search_path} y couterait bien plus
   * cher que l'ecart qu'on evite.
   * </p>
   *
   * <p>
   * La lecture est ouverte aux deux roles metier : le pupitre porte un compte d'appareil {@code USER}, et le
   * back-office doit pouvoir verifier ce que l'atelier voit. {@code ADMIN} n'a aucun acces metier.
   * </p>
   */
  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public ReferentielDuPupitre referentiel() {
    return referentiels.referentiel();
  }
}
