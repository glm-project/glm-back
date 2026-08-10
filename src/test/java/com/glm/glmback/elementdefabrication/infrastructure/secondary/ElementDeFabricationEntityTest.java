package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationEntityTest {

  @Test
  void shouldConvertOrdreDeFabricationToDomainAndBack() {
    ElementDeFabrication element = elementDeFabricationAssemblageCarter();

    assertThat(ElementDeFabricationEntity.from(element).toDomain()).isEqualTo(element);
  }

  @Test
  void shouldConvertProduitToDomainAndBack() {
    ElementDeFabrication element = elementDeFabricationCarterMoteur();

    assertThat(ElementDeFabricationEntity.from(element).toDomain()).isEqualTo(element);
  }
}
