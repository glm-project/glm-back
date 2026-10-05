package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

class EmpreinteSha256 {

  byte[] empreinte(byte[] valeur) throws GeneralSecurityException {
    return MessageDigest.getInstance("SHA-256").digest(valeur);
  }
}
