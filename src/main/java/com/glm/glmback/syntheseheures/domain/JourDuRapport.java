package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;
import java.util.List;

/** Un jour de la periode et ses pointages, dans l'ordre chronologique ; ses durees en sont la somme. */
public record JourDuRapport(LocalDate jour, List<LigneDuJournal> lignes) {
  public JourDuRapport {
    Assert.notNull("jour", jour);
    Assert.field("lignes", lignes).notNull().noNullElement();
  }

  public DureeTotale duree() {
    return DureeTotale.somme(lignes.stream().map(LigneDuJournal::duree).toList());
  }

  public DureeTotale dureeNonConformite() {
    return DureeTotale.somme(
      lignes
        .stream()
        .filter(ligne -> ligne.categorie() == CategorieDActivite.NON_CONFORMITE)
        .map(LigneDuJournal::duree)
        .toList()
    );
  }

  public boolean finAutomatique() {
    return lignes.stream().anyMatch(ligne -> ligne.etat() == EtatDActivite.TERMINEE_AUTOMATIQUEMENT);
  }

  public boolean simultanee() {
    return lignes.stream().anyMatch(LigneDuJournal::simultanee);
  }
}
