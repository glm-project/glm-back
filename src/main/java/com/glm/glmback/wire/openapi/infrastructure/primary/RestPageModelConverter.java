package com.glm.glmback.wire.openapi.infrastructure.primary;

import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * Keeps the historic {@code PageRest*} schema names while the HTTP layer uses {@link RestPage}.
 *
 * <p>
 * A fixed {@code @Schema(name = "Page")} alone makes swagger-core collapse every instantiation into one schema and
 * retain the content type of whichever endpoint it visits last. Supplying the resolved generic name before the
 * standard converter runs preserves one schema per content type.
 * </p>
 */
final class RestPageModelConverter implements ModelConverter {

  @Override
  public io.swagger.v3.oas.models.media.Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    if (type.getType() instanceof ParameterizedType parameterizedType && parameterizedType.getRawType().equals(RestPage.class)) {
      type.name("Page" + schemaName(parameterizedType.getActualTypeArguments()[0]));
    }

    return chain.next().resolve(type, context, chain);
  }

  private static String schemaName(Type type) {
    Class<?> schemaClass = (Class<?>) type;
    io.swagger.v3.oas.annotations.media.Schema annotation = schemaClass.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);

    return annotation == null || annotation.name().isBlank() ? schemaClass.getSimpleName() : annotation.name();
  }
}
