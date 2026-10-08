package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.categoriedeproduit.domain.CategorieDejaExistanteException;
import com.glm.glmback.categoriedeproduit.domain.CategorieIntrouvableException;
import com.glm.glmback.categoriedeproduit.domain.CategorieUtiliseeException;
import com.glm.glmback.categoriedeproduit.domain.OrdreIncompletException;
import com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdviceContract;
import com.glm.glmback.shared.error.infrastructure.primary.PublishedProblem;
import java.util.stream.Stream;

@UnitTest
class CategorieDeProduitExceptionAdviceTest extends ExceptionAdviceContract {

  @Override
  protected Object advice() {
    return new CategorieDeProduitExceptionAdvice();
  }

  @Override
  protected Stream<PublishedProblem> erreursPubliees() {
    return Stream.of(
      new PublishedProblem(
        new CategorieIntrouvableException(CODE_MOULE),
        "urn:glm:erreur:categorie-de-produit:categorie-introuvable",
        NOT_FOUND
      ),
      new PublishedProblem(
        new CategorieDejaExistanteException(CODE_MOULE),
        "urn:glm:erreur:categorie-de-produit:categorie-deja-existante",
        CONFLICT
      ),
      new PublishedProblem(new CategorieUtiliseeException(CODE_MOULE), "urn:glm:erreur:categorie-de-produit:categorie-utilisee", CONFLICT),
      new PublishedProblem(new OrdreIncompletException(), "urn:glm:erreur:categorie-de-produit:ordre-incomplet", CONFLICT)
    );
  }
}
