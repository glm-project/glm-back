package com.glm.glmback.shared.authentication.infrastructure.primary;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class SignedJwtFixture {

  public static final RSAKey SIGNING_KEY = key();
  public static final String API_AUDIENCE = "account";

  private SignedJwtFixture() {}

  private static RSAKey key() {
    try {
      return new RSAKeyGenerator(2048).keyID("signed-claims").generate();
    } catch (JOSEException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  public static JWTClaimsSet.Builder claims(String issuer) {
    Instant now = Instant.now();
    return new JWTClaimsSet.Builder()
      .subject("same-subject")
      .issuer(issuer)
      .audience(API_AUDIENCE)
      .issueTime(Date.from(now))
      .expirationTime(Date.from(now.plusSeconds(300)))
      .claim("preferred_username", "gestionnaire.impeccmold")
      .claim("roles", List.of("ROLE_GESTIONNAIRE", "ROLE_USER"))
      .claim("tenant", "impeccmold");
  }

  public static String token(JWTClaimsSet claims) throws JOSEException {
    SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(SIGNING_KEY.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(SIGNING_KEY));
    return jwt.serialize();
  }
}
