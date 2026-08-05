package com.glm.glmback.shared.error.domain;

import java.util.Map;

public final class StringNotMatchingPatternException extends AssertionException {

  private final String pattern;

  private StringNotMatchingPatternException(StringNotMatchingPatternExceptionBuilder builder) {
    super(builder.field, builder.message());
    pattern = builder.pattern;
  }

  public static StringNotMatchingPatternExceptionBuilder builder() {
    return new StringNotMatchingPatternExceptionBuilder();
  }

  static final class StringNotMatchingPatternExceptionBuilder {

    private String value;
    private String pattern;
    private String field;

    private StringNotMatchingPatternExceptionBuilder() {}

    StringNotMatchingPatternExceptionBuilder field(String field) {
      this.field = field;

      return this;
    }

    StringNotMatchingPatternExceptionBuilder value(String value) {
      this.value = value;

      return this;
    }

    StringNotMatchingPatternExceptionBuilder pattern(String pattern) {
      this.pattern = pattern;

      return this;
    }

    private String message() {
      return "The value \"%s\" in field \"%s\" must match \"%s\"".formatted(value, field, pattern);
    }

    public StringNotMatchingPatternException build() {
      return new StringNotMatchingPatternException(this);
    }
  }

  @Override
  public AssertionErrorType type() {
    return AssertionErrorType.STRING_NOT_MATCHING_PATTERN;
  }

  @Override
  public Map<String, String> parameters() {
    return Map.of("pattern", pattern);
  }
}
