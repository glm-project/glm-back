package com.glm.glmback.pupitre.domain;

import java.util.Optional;

/**
 * La version du logo de l'entreprise courante, vide tant qu'aucun n'est depose. Le pupitre n'en recoit que la version :
 * l'image se lit a part, a l'adresse de sa version, et seulement quand elle change.
 */
@FunctionalInterface
public interface LogoDuPupitre {
  Optional<VersionDuLogo> version();
}
