package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Web Adapter - GlobalExceptionHandler Unit Test")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("Should return 409 Conflict when DataIntegrityViolationException is handled")
  void shouldReturn409ConflictOnDataIntegrityViolation() {
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("duplicate key value violates unique constraint");

    ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(response.getBody().get("message"))
        .isEqualTo("A database conflict occurred while updating loyalty rate for store.");
  }
}
