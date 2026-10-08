package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Un pointage que la regle de reception a ignore : ce qui est arrive, quand le serveur l'a recu, et pourquoi il n'est pas
 * entre au journal.
 *
 * <p>
 * C'est ce que l'audit conserve, une ligne par pointage ignore : l'identifiant du geste, l'operateur, le suivi, le
 * poste, le type et l'heure du geste viennent du pointage ; l'heure de reception est la date d'enregistrement de
 * l'horodatage ; la raison et le dernier pointage accepte compare, du verdict. L'audit ne sert pas a rejouer le
 * pointage.
 * </p>
 */
public record PointageIgnore(PointageAEnregistrer pointage, Horodatage horodatage, VerdictDeReception.Ignore verdict) {
  public PointageIgnore {
    Assert.notNull("pointage", pointage);
    Assert.notNull("horodatage", horodatage);
    Assert.notNull("verdict", verdict);
  }
}
