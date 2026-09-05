package com.glm.glmback.shared.pagination.infrastructure.primary;

import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import org.junit.jupiter.api.Test;

@UnitTest
class RestPageTest {

  @Test
  void shouldMapADomainPageToATransportPage() {
    Page<String> page = Page.<String>builder().content(firstAndSecond()).currentPage(1).pageSize(2).totalElementsCount(5);

    RestPage<String> restPage = RestPage.from(page, String::toUpperCase);

    assertThat(restPage.content()).containsExactly("FIRST", "SECOND");
    assertThat(restPage.currentPage()).isEqualTo(1);
    assertThat(restPage.pageSize()).isEqualTo(2);
    assertThat(restPage.totalElementsCount()).isEqualTo(5);
  }
}
