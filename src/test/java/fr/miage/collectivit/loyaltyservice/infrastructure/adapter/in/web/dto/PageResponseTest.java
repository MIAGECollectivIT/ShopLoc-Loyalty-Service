package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DTO - PageResponse Unit Tests")
class PageResponseTest {

  @Test
  @DisplayName("Should correctly paginate an in-memory list")
  void shouldCorrectlyPaginateList() {
    List<String> items = List.of("A", "B", "C", "D", "E");

    PageResponse<String> page0 = PageResponse.of(items, 0, 2);
    assertThat(page0.content()).containsExactly("A", "B");
    assertThat(page0.page()).isZero();
    assertThat(page0.size()).isEqualTo(2);
    assertThat(page0.totalElements()).isEqualTo(5);
    assertThat(page0.totalPages()).isEqualTo(3);
    assertThat(page0.first()).isTrue();
    assertThat(page0.last()).isFalse();

    PageResponse<String> page2 = PageResponse.of(items, 2, 2);
    assertThat(page2.content()).containsExactly("E");
    assertThat(page2.page()).isEqualTo(2);
    assertThat(page2.first()).isFalse();
    assertThat(page2.last()).isTrue();
  }

  @Test
  @DisplayName("Should handle empty list cleanly")
  void shouldHandleEmptyList() {
    PageResponse<String> empty = PageResponse.of(List.of(), 0, 10);
    assertThat(empty.content()).isEmpty();
    assertThat(empty.totalElements()).isZero();
    assertThat(empty.totalPages()).isZero();
    assertThat(empty.first()).isTrue();
    assertThat(empty.last()).isTrue();
  }
}
