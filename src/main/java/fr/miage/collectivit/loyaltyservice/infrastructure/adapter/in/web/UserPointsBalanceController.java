package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserPointsAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsBalance;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserPointsBalanceQuery;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.UserPointsBalanceResponse;
import fr.miage.collectivit.loyaltyservice.security.CallerAccessGuard;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({"/v1/loyalty", "/api/v1/loyalty"})
@Tag(
    name = "Loyalty Points Balance",
    description =
        "Consultation du solde de points de fidélité collectés et de l'historique des transactions")
@RequiredArgsConstructor
@SuppressWarnings("PMD.LawOfDemeter")
public class UserPointsBalanceController {

  private static final String ROLE_CLIENT = "CLIENT";
  private static final String ROLE_MERCHANT = "MERCHANT";
  private static final String ROLE_ADMIN = "ADMIN";

  private final GetUserPointsBalanceQuery getUserPointsBalanceQuery;
  private final CallerAccessGuard callerAccessGuard;

  @GetMapping("/balance")
  @RequiresRole({ROLE_CLIENT, ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Consulter mon solde de points de fidélité",
      description =
          "Retourne le solde de points de fidélité de l'utilisateur authentifié ainsi que son historique des gains et dépenses de points.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Solde et historique de points retournés avec succès"),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)"),
        @ApiResponse(
            responseCode = "403",
            description = "Identifiant utilisateur absent ou rôle non autorisé")
      })
  public ResponseEntity<UserPointsBalanceResponse> getCurrentUserBalance(
      HttpServletRequest request) {
    String resolvedUserId =
        callerAccessGuard.resolveAuthenticatedUserId(
            request,
            () ->
                new UserPointsAccessDeniedException(
                    "Access denied: Authenticated user identifier is missing from token."));
    UserPointsBalance balance = getUserPointsBalanceQuery.getUserPointsBalance(resolvedUserId);
    return ResponseEntity.ok(UserPointsBalanceResponse.fromDomain(balance));
  }

  @GetMapping("/users/{userId}/balance")
  @RequiresRole({ROLE_CLIENT, ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Consulter le solde de points de fidélité d'un utilisateur",
      description =
          "Retourne le solde de points et l'historique d'un usager donné (retourne 0.0 pt si aucun mouvement n'a encore été enregistré). Protégé contre les accès transverses (BOLA).",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Solde et historique de points retournés avec succès"),
        @ApiResponse(responseCode = "400", description = "Identifiant userId invalide"),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)"),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (l'usager ne peut consulter que son propre solde)")
      })
  public ResponseEntity<UserPointsBalanceResponse> getUserBalance(
      @Parameter(
              description = "UUID public de l'utilisateur",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          @NotBlank(message = "userId must not be blank")
          String userId,
      HttpServletRequest request) {
    verifyPointsAccess(userId, request);
    UserPointsBalance balance = getUserPointsBalanceQuery.getUserPointsBalance(userId);
    return ResponseEntity.ok(UserPointsBalanceResponse.fromDomain(balance));
  }

  @GetMapping("/customers/{customerId}/points")
  @RequiresRole({ROLE_CLIENT, ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Consulter les points fidélité d'un client (endpoint checkout compatible)",
      description =
          "Retourne le solde de points (champs balance et points) et l'historique d'un client. Protégé contre les accès transverses (BOLA).",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Solde de points retourné avec succès"),
        @ApiResponse(responseCode = "400", description = "Identifiant customerId invalide"),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)"),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (l'usager ne peut consulter que ses propres points)")
      })
  public ResponseEntity<UserPointsBalanceResponse> getCustomerPoints(
      @Parameter(
              description = "UUID public ou identifiant client",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          @NotBlank(message = "customerId must not be blank")
          String customerId,
      HttpServletRequest request) {
    verifyPointsAccess(customerId, request);
    UserPointsBalance balance = getUserPointsBalanceQuery.getUserPointsBalance(customerId);
    return ResponseEntity.ok(UserPointsBalanceResponse.fromDomain(balance));
  }

  private void verifyPointsAccess(String targetUserId, HttpServletRequest request) {
    callerAccessGuard.verifyUserAccess(
        targetUserId,
        request,
        () ->
            new UserPointsAccessDeniedException(
                "Access denied: You can only view your own loyalty points balance."));
  }
}
