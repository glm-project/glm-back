package com.glm.glmback.atelier.infrastructure.secondary;

import java.time.Duration;
import java.util.Map;

public record ConfigurationDesApercus(String cleActive, Map<String, String> cles, Duration validite) {}
