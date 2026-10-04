package com.glm.glmback.atelier.gestionconflits.infrastructure.secondary;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

class CryptographieDesReferences {

  byte[] chiffre(SecretKey cle, byte[] nonce, byte[] entete, byte[] valeur) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(128, nonce));
    cipher.updateAAD(entete);
    return cipher.doFinal(valeur);
  }

  byte[] dechiffre(SecretKey cle, byte[] nonce, byte[] entete, byte[] valeur) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(128, nonce));
    cipher.updateAAD(entete);
    return cipher.doFinal(valeur);
  }

  byte[] empreinte(byte[] valeur) throws GeneralSecurityException {
    return MessageDigest.getInstance("SHA-256").digest(valeur);
  }
}
