package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class AesReferencesDApercuTest {

  private static final String CLE_LOCALE = Base64.getEncoder().encodeToString(new byte[32]);
  private final ConfigurationDesApercus configuration = new ConfigurationDesApercus(
    "locale",
    Map.of("locale", CLE_LOCALE),
    Duration.ofMinutes(15)
  );

  @Test
  void shouldAuthentifierUnePreuveOpaqueLisibleParUneAutreInstance() {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    var preuve = recuDAnnulation(suivi).preuve();
    var premiere = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
    var seconde = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
    String reference = premiere.issue(preuve);
    assertThat(seconde.read(reference)).isEqualTo(preuve);
    assertThat(reference).startsWith("v1.locale.").doesNotContain(SUJET_LEROY).doesNotContain(MOTIF_ERREUR_DE_SAISIE.value());
    assertThat(premiere.issue(preuve)).isNotEqualTo(reference);
  }
}
