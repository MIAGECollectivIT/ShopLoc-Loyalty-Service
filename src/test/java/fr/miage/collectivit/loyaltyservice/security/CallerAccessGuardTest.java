package fr.miage.collectivit.loyaltyservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.exception.UserPointsAccessDeniedException;
import java.util.UUID;
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
    String uuid = "550e8400-e29b-41d4-a716-446655440123";
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "  " + uuid + "  ");

    String resolved =
        guard.resolveAuthenticatedUserId(
            request, () -> new UserPointsAccessDeniedException("missing"));

    assertThat(resolved).isEqualTo(uuid);
    assertThat(guard.resolveAuthenticatedUserUuid(request)).isEqualTo(UUID.fromString(uuid));
  }

  @Test
  @DisplayName("Should resolve UUID attribute directly when stored as UUID")
  void shouldResolveUuidAttributeDirectly() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440777");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, uuid);

    String resolved =
        guard.resolveAuthenticatedUserId(
            request, () -> new UserPointsAccessDeniedException("missing"));

    assertThat(resolved).isEqualTo(uuid.toString());
    assertThat(guard.resolveAuthenticatedUserUuid(request)).isEqualTo(uuid);
  }

  @Test
  @DisplayName("Should throw configured exception when userId claim is absent or blank")
  void shouldThrowWhenNoIdentifierPresent() {
    MockHttpServletRequest emptyRequest = new MockHttpServletRequest();
    assertThatThrownBy(
            () ->
                guard.resolveAuthenticatedUserId(
                    emptyRequest, () -> new UserPointsAccessDeniedException("missing identity")))
        .isInstanceOf(UserPointsAccessDeniedException.class)
        .hasMessage("missing identity");
    assertThat(guard.resolveAuthenticatedUserUuid(emptyRequest)).isNull();

    MockHttpServletRequest blankRequest = new MockHttpServletRequest();
    blankRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "   ");
    assertThatThrownBy(
            () ->
                guard.resolveAuthenticatedUserId(
                    blankRequest, () -> new UserPointsAccessDeniedException("missing identity")))
        .isInstanceOf(UserPointsAccessDeniedException.class);
    assertThat(guard.resolveAuthenticatedUserUuid(blankRequest)).isNull();
  }

  @Test
  @DisplayName("Should return null UUID when userId attribute is not a valid UUID")
  void shouldReturnNullUuidForMalformedUserId() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "not-a-uuid");

    assertThat(guard.resolveAuthenticatedUserUuid(request)).isNull();
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
  @DisplayName("Should allow owner matching userId (case-insensitive)")
  void shouldAllowSelfByUserId() {
    MockHttpServletRequest byUuidRequest = new MockHttpServletRequest();
    byUuidRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT");
    byUuidRequest.setAttribute(
        JwtAuthInterceptor.ATTR_USER_ID, "550E8400-E29B-41D4-A716-446655440001");

    assertThatCode(
            () ->
                guard.verifyUserAccess(
                    "  550e8400-e29b-41d4-a716-446655440001 ",
                    byUuidRequest,
                    () -> new UserPointsAccessDeniedException("denied")))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should reject non-admin accessing another user's resource (BOLA)")
  void shouldDenyCrossUserAccess() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT");
    request.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "user-100");

    assertThatThrownBy(
            () ->
                guard.verifyUserAccess(
                    "user-999", request, () -> new UserPointsAccessDeniedException("bola denied")))
        .isInstanceOf(UserPointsAccessDeniedException.class)
        .hasMessage("bola denied");
  }

  @Test
  @DisplayName("Should allow MERCHANT and ADMIN to access customer points, but restrict CLIENT")
  void shouldAllowMerchantAndAdminOnCustomerPoints() {
    MockHttpServletRequest merchantRequest = new MockHttpServletRequest();
    merchantRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT");
    merchantRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "merchant-1");

    assertThatCode(
            () ->
                guard.verifyCustomerPointsAccess(
                    "customer-999",
                    merchantRequest,
                    () -> new UserPointsAccessDeniedException("denied")))
        .doesNotThrowAnyException();

    MockHttpServletRequest clientRequest = new MockHttpServletRequest();
    clientRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT");
    clientRequest.setAttribute(JwtAuthInterceptor.ATTR_USER_ID, "client-1");

    assertThatThrownBy(
            () ->
                guard.verifyCustomerPointsAccess(
                    "customer-999",
                    clientRequest,
                    () -> new UserPointsAccessDeniedException("denied")))
        .isInstanceOf(UserPointsAccessDeniedException.class);
  }
}
