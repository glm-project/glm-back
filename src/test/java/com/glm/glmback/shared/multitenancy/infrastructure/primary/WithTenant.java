package com.glm.glmback.shared.multitenancy.infrastructure.primary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Authentifie par un JWT porteur d'un claim de tenant : {@code @WithMockUser} n'en produit pas, et
 * sans tenant aucun acces a la base n'est possible.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@WithSecurityContext(factory = WithTenant.Factory.class)
public @interface WithTenant {
  String value();

  class Factory implements WithSecurityContextFactory<WithTenant> {

    @Override
    public SecurityContext createSecurityContext(WithTenant annotation) {
      return TenantSecurityContexts.securityContext(annotation.value());
    }
  }
}
