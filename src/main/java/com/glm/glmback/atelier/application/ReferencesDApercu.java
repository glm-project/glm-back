package com.glm.glmback.atelier.application;

public interface ReferencesDApercu {
  String issue(PreuveDApercu preuve);

  PreuveDApercu read(String reference);
}
