package com.glm.glmback.atelier.application.gestionconflits;

public interface ReferencesDApercu {
  String issue(PreuveDApercu preuve);

  PreuveDApercu read(String reference);
}
