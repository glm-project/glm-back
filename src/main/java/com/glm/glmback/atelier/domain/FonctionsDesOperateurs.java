package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * La fonction que porte le profil d'un operateur : fraisage, tournage, erosion, dessin.
 *
 * <p>
 * Elle sert a agreger la synthese d'un element par categorie de travail, jamais a interdire un pointage. Elle est
 * recopiee sur l'evenement au moment de la saisie, comme le nom de l'element l'est a l'engagement : un profil modifie
 * plus tard ne doit pas reecrire l'histoire de l'atelier.
 * </p>
 *
 * <p>
 * Elle est facultative, une entreprise cliente pouvant n'avoir aucune specialite a distinguer.
 * </p>
 */
public interface FonctionsDesOperateurs {
  Optional<NatureDOperation> fonction(Operateur operateur);
}
