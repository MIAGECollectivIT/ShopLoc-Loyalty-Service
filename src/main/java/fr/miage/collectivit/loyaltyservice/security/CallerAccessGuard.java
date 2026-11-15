package fr.miage.collectivit.loyaltyservice.security;

import jakarta.servlet.http.HttpServletRequest;
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
   * Resolves the authenticated caller's canonical identifier ({@code userId} claim, falling back to
   * {@code dbId}) from request attributes.
   */
  public String resolveAuthenticatedUserId(
      HttpServletRequest request, Supplier<? extends RuntimeException> missingIdentityException) {
    String callerUserId = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    if (callerUserId != null && !callerUserId.isBlank()) {
      return callerUserId.trim();
    }
    Long callerUserDbId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    if (callerUserDbId != null) {
      return String.valueOf(callerUserDbId);
    }
    throw missingIdentityException.get();
  }

  /**
   * Verifies that the authenticated caller is either an {@code ADMIN} or the owner of {@code
   * targetUserId} (matching {@code userId} case-insensitively or {@code dbId}).
   */
  public void verifyUserAccess(
      String targetUserId,
      HttpServletRequest request,
      Supplier<? extends RuntimeException> accessDeniedException) {
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    String callerUserId = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    Long callerUserDbId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    String normalizedTarget = targetUserId != null ? targetUserId.trim() : "";

    boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(callerRole);
    boolean isSelf =
        (callerUserId != null && callerUserId.equalsIgnoreCase(normalizedTarget))
            || (callerUserDbId != null && String.valueOf(callerUserDbId).equals(normalizedTarget));

    if (!isAdmin && !isSelf) {
      throw accessDeniedException.get();
    }
  }
}
