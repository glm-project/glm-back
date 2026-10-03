package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ApercuInvalideException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class AesReferencesDApercuTest {

  private static final String CLE_LOCALE = Base64.getEncoder().encodeToString(new byte[32]);
  private final ConfigurationDesApercus configuration = new ConfigurationDesApercus(
    "locale",
    Map.of("locale", CLE_LOCALE),
    Duration.ofMinutes(15)
  );

  @ParameterizedTest(name = "reference invalide {index}")
  @MethodSource("referencesMalFormees")
  void shouldRefuserUneReferenceAltereeOuMalFormee(String reference) {
    var codec = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
    assertThatThrownBy(() -> codec.read(reference)).isExactlyInstanceOf(ApercuInvalideException.class);
  }

  private static Stream<String> referencesMalFormees() {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    var config = new ConfigurationDesApercus("locale", Map.of("locale", CLE_LOCALE), Duration.ofMinutes(15));
    String valide = new AesReferencesDApercu(config, new SecureRandom(), new CryptographieDesReferences()).issue(
      recuDAnnulation(suivi).preuve()
    );
    byte[] chiffre = Base64.getUrlDecoder().decode(valide.substring("v1.locale.".length()));
    chiffre[12] ^= 1;
    return Stream.of(
      null,
      "",
      "v1.locale.A",
      "v1.locale.AAAA",
      valide + ".surplus",
      valide.replace("v1.", "v2."),
      valide.replace(".locale.", ".inconnue."),
      "v1.locale." + "A".repeat(16385),
      "v1.locale." + Base64.getUrlEncoder().withoutPadding().encodeToString(chiffre)
    );
  }

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
