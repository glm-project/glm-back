package com.glm.glmback.atelier.gestionconflits.application;

public interface ReferencesDApercu {
  String issue(PreuveDApercu preuve);

  PreuveDApercu read(String reference);
}
