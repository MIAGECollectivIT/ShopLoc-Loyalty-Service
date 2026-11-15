package fr.miage.collectivit.loyaltyservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.exception.UserPointsAccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("Security - CallerAccessGuard Unit Tests")
class CallerAccessGuardTest {

  private final CallerAccessGuard guard = new CallerAccessGuard();

  @Test
  @DisplayName("Should resolve trimmed userId when present on request")
  void shouldResolveUserId() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "  user-uuid-123  ");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID, 42L);

    String resolved =
        guard.resolveAuthenticatedUserId(
            request, () -> new UserPointsAccessDeniedException("missing"));

    assertThat(resolved).isEqualTo("user-uuid-123");
  }

  @Test
  @DisplayName("Should fallback to dbId when userId claim is absent or blank")
  void shouldFallbackToDbId() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "   ");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID, 77L);

    String resolved =
        guard.resolveAuthenticatedUserId(
            request, () -> new UserPointsAccessDeniedException("missing"));

    assertThat(resolved).isEqualTo("77");
  }

  @Test
  @DisplayName("Should throw configured exception when no caller identifier is present")
  void shouldThrowWhenNoIdentifierPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThatThrownBy(
            () ->
                guard.resolveAuthenticatedUserId(
                    request, () -> new UserPointsAccessDeniedException("missing identity")))
        .isInstanceOf(UserPointsAccessDeniedException.class)
        .hasMessage("missing identity");
  }

  @Test
  @DisplayName("Should allow ADMIN caller to access any userId")
  void shouldAllowAdminForAnyTarget() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "admin");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "admin-uuid");

    assertThatCode(
            () ->
                guard.verifyUserAccess(
                    "other-user-uuid",
                    request,
                    () -> new UserPointsAccessDeniedException("denied")))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should allow owner matching userId (case-insensitive) or dbId")
  void shouldAllowSelfByUserIdOrDbId() {
    MockHttpServletRequest byUuidRequest = new MockHttpServletRequest();
    byUuidRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT");
    byUuidRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "User-Uuid-1");

    assertThatCode(
            () ->
                guard.verifyUserAccess(
                    "  user-uuid-1 ",
                    byUuidRequest,
                    () -> new UserPointsAccessDeniedException("denied")))
        .doesNotThrowAnyException();

    MockHttpServletRequest byDbIdRequest = new MockHttpServletRequest();
    byDbIdRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT");
    byDbIdRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID, 105L);

    assertThatCode(
            () ->
                guard.verifyUserAccess(
                    "105", byDbIdRequest, () -> new UserPointsAccessDeniedException("denied")))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should reject non-admin accessing another user's resource (BOLA)")
  void shouldDenyCrossUserAccess() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "user-100");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID, 100L);

    assertThatThrownBy(
            () ->
                guard.verifyUserAccess(
                    "user-999", request, () -> new UserPointsAccessDeniedException("bola denied")))
        .isInstanceOf(UserPointsAccessDeniedException.class)
        .hasMessage("bola denied");
  }
}
