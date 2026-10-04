package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.application.PreuveDApercu;
import com.glm.glmback.atelier.application.ReferencesDApercu;
import com.glm.glmback.atelier.domain.ApercuInvalideException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.core.JacksonException;

final class AesReferencesDApercu implements ReferencesDApercu {

  private final ConfigurationDesApercus configuration;
  private final SecureRandom aleatoire;
  private final CryptographieDesReferences cryptographie;

  AesReferencesDApercu(ConfigurationDesApercus configuration, SecureRandom aleatoire, CryptographieDesReferences cryptographie) {
    this.configuration = configuration;
    this.aleatoire = aleatoire;
    this.cryptographie = cryptographie;
  }

  @Override
  public String issue(PreuveDApercu preuve) {
    byte[] nonce = new byte[12];
    aleatoire.nextBytes(nonce);
    String entete = "v1." + configuration.cleActive();
    try {
      byte[] chiffre = cryptographie.chiffre(
        cle(configuration.cleActive()),
        nonce,
        entete.getBytes(StandardCharsets.US_ASCII),
        FormatDePreuveDApercu.serialise(preuve).getBytes(StandardCharsets.UTF_8)
      );
      byte[] contenu = ByteBuffer.allocate(nonce.length + chiffre.length).put(nonce).put(chiffre).array();
      return entete + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(contenu);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Impossible d'authentifier l'apercu", e);
    }
  }

  @Override
  public PreuveDApercu read(String reference) {
    if (reference == null || reference.length() > 16384 || !reference.matches("v1\\.[a-zA-Z0-9_-]{1,32}\\.[a-zA-Z0-9_-]+")) {
      throw new ApercuInvalideException();
    }
    String[] parties = reference.split("\\.");
    if (!configuration.cles().containsKey(parties[1])) {
      throw new ApercuInvalideException();
    }
    String entete = parties[0] + "." + parties[1];
    byte[] contenu;
    try {
      contenu = Base64.getUrlDecoder().decode(parties[2]);
    } catch (IllegalArgumentException e) {
      throw new ApercuInvalideException();
    }
    if (contenu.length < 28) {
      throw new ApercuInvalideException();
    }
    byte[] clair;
    try {
      clair = cryptographie.dechiffre(
        cle(parties[1]),
        Arrays.copyOfRange(contenu, 0, 12),
        entete.getBytes(StandardCharsets.US_ASCII),
        Arrays.copyOfRange(contenu, 12, contenu.length)
      );
    } catch (AEADBadTagException e) {
      throw new ApercuInvalideException();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Impossible de dechiffrer l'apercu", e);
    }
    try {
      return FormatDePreuveDApercu.relit(new String(clair, StandardCharsets.UTF_8));
    } catch (JacksonException e) {
      throw new ApercuInvalideException();
    }
  }

  private SecretKey cle(String identifiant) {
    return new SecretKeySpec(Base64.getDecoder().decode(configuration.cles().get(identifiant)), "AES");
  }
}
