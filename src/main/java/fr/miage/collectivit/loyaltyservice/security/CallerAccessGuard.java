package fr.miage.collectivit.loyaltyservice.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Shared web-security guard centralizing extraction of the authenticated caller identity from
 * {@link JwtAuthInterceptor} request attributes and enforcing BOLA ownership checks (self or ADMIN)
 * across user-scoped endpoints.
 */
@Component
public class CallerAccessGuard {

  private static final String ROLE_ADMIN = "ADMIN";

  /**
   * Resolves the authenticated caller's canonical UUID string ({@code userId} claim) from request
   * attributes.
   */
  public String resolveAuthenticatedUserId(
      HttpServletRequest request, Supplier<? extends RuntimeException> missingIdentityException) {
    String callerUserId = extractUserIdString(request);
    if (callerUserId != null) {
      return callerUserId;
    }
    throw missingIdentityException.get();
  }

  /**
   * Resolves the authenticated caller's {@link UUID} from {@link JwtAuthInterceptor#ATTR_USER_ID},
   * or {@code null} if missing or not a valid UUID.
   */
  public UUID resolveAuthenticatedUserUuid(HttpServletRequest request) {
    Object attr = request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    if (attr instanceof UUID uuid) {
      return uuid;
    }
    if (attr instanceof String str && !str.isBlank()) {
      try {
        return UUID.fromString(str.trim());
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
    return null;
  }

  /**
   * Verifies that the authenticated caller is either an {@code ADMIN} or the owner of {@code
   * targetUserId} (matching {@code userId} case-insensitively).
   */
  public void verifyUserAccess(
      String targetUserId,
      HttpServletRequest request,
      Supplier<? extends RuntimeException> accessDeniedException) {
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    String callerUserId = extractUserIdString(request);
    String normalizedTarget = targetUserId != null ? targetUserId.trim() : "";

    boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(callerRole);
    boolean isSelf = callerUserId != null && callerUserId.equalsIgnoreCase(normalizedTarget);

    if (!isAdmin && !isSelf) {
      throw accessDeniedException.get();
    }
  }

  /**
   * Verifies access to customer points query: ADMIN and MERCHANT can query any customer, while
   * CLIENT can only query their own points.
   */
  public void verifyCustomerPointsAccess(
      String targetUserId,
      HttpServletRequest request,
      Supplier<? extends RuntimeException> accessDeniedException) {
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    if (ROLE_ADMIN.equalsIgnoreCase(callerRole) || "MERCHANT".equalsIgnoreCase(callerRole)) {
      return;
    }
    verifyUserAccess(targetUserId, request, accessDeniedException);
  }

  private String extractUserIdString(HttpServletRequest request) {
    Object attr = request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    if (attr instanceof UUID uuid) {
      return uuid.toString();
    }
    if (attr instanceof String str && !str.isBlank()) {
      return str.trim();
    }
    return null;
  }
}
