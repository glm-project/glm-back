package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.application.gestionconflits.ReferencesDApercu;
import com.glm.glmback.atelier.domain.gestionconflits.ApercuInvalideException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @MethodSource("pannesDeDechiffrement")
  void shouldSignalerUnePanneTechniqueDeDechiffrementDuneReferenceSaine(GeneralSecurityException indisponible)
    throws GeneralSecurityException {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    var preuve = recuDAnnulation(suivi).preuve();
    ReferencesDApercu emetteur = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
    String reference = emetteur.issue(preuve);
    var cryptographie = mock(CryptographieDesReferences.class);
    when(cryptographie.dechiffre(any(), any(), any(), any())).thenThrow(indisponible);
    ReferencesDApercu lecteur = new AesReferencesDApercu(configuration, new SecureRandom(), cryptographie);

    assertThatThrownBy(() -> lecteur.read(reference))
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasCause(indisponible);
  }

  private static Stream<GeneralSecurityException> pannesDeDechiffrement() {
    return Stream.of(
      new NoSuchAlgorithmException("algorithme indisponible"),
      new NoSuchPaddingException("padding indisponible"),
      new InvalidKeyException("cle refusee par le fournisseur"),
      new InvalidAlgorithmParameterException("parametres refuses par le fournisseur")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "{", "{}", "{\"commande\":\"pas-un-uuid\"}" })
  void shouldRefuserUnContenuAuthentifieQuiNestPasUnePreuve(String contenu) throws GeneralSecurityException {
    byte[] nonce = new byte[12];
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(CLE_LOCALE), "AES"), new GCMParameterSpec(128, nonce));
    cipher.updateAAD("v1.locale".getBytes(StandardCharsets.US_ASCII));
    byte[] chiffre = cipher.doFinal(contenu.getBytes(StandardCharsets.UTF_8));
    String reference =
      "v1.locale."
      + Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(ByteBuffer.allocate(nonce.length + chiffre.length).put(nonce).put(chiffre).array());
    ReferencesDApercu lecteur = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());

    assertThatThrownBy(() -> lecteur.read(reference)).isExactlyInstanceOf(ApercuInvalideException.class);
  }

  @Test
  void shouldPropagerLaPanneDuFournisseurPendantLaLectureDuneReferenceSaine() throws GeneralSecurityException {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    var preuve = recuDAnnulation(suivi).preuve();
    ReferencesDApercu emetteur = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
    String reference = emetteur.issue(preuve);
    var cryptographie = mock(CryptographieDesReferences.class);
    var indisponible = new ProviderException("fournisseur indisponible");
    when(cryptographie.dechiffre(any(), any(), any(), any())).thenThrow(indisponible);
    ReferencesDApercu lecteur = new AesReferencesDApercu(configuration, new SecureRandom(), cryptographie);

    assertThatThrownBy(() -> lecteur.read(reference)).isSameAs(indisponible);
  }

  @Test
  void shouldRefuserDEmettreUneReferenceSiLaCryptographieEchoue() throws GeneralSecurityException {
    var cryptographie = mock(CryptographieDesReferences.class);
    var indisponible = new GeneralSecurityException("fournisseur indisponible");
    when(cryptographie.chiffre(any(), any(), any(), any())).thenThrow(indisponible);
    var codec = new AesReferencesDApercu(configuration, new SecureRandom(), cryptographie);
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    assertThatThrownBy(() -> codec.issue(recuDAnnulation(suivi).preuve()))
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasCause(indisponible);
  }

  @Test
  void shouldConserverLesAnciennesReferencesPendantLaRotationEtAuthentifierLeNomDeCle() {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    var preuve = recuDAnnulation(suivi).preuve();
    var ancien = new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
    String reference = ancien.issue(preuve);
    var conservees = new ConfigurationDesApercus("nouvelle", Map.of("locale", CLE_LOCALE, "nouvelle", CLE_LOCALE), Duration.ofMinutes(15));
    var rotation = new AesReferencesDApercu(conservees, new SecureRandom(), new CryptographieDesReferences());
    assertThat(rotation.read(reference)).isEqualTo(preuve);
    assertThat(rotation.issue(preuve)).startsWith("v1.nouvelle.");
    assertThatThrownBy(() -> rotation.read(reference.replace(".locale.", ".nouvelle."))).isExactlyInstanceOf(ApercuInvalideException.class);
    var retirees = new ConfigurationDesApercus("nouvelle", Map.of("nouvelle", CLE_LOCALE), Duration.ofMinutes(15));
    var apresRetrait = new AesReferencesDApercu(retirees, new SecureRandom(), new CryptographieDesReferences());
    assertThatThrownBy(() -> apresRetrait.read(reference)).isExactlyInstanceOf(ApercuInvalideException.class);
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
