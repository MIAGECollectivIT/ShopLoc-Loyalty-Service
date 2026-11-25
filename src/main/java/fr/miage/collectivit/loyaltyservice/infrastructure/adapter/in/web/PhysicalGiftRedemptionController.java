package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.PhysicalGiftRedemptionRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.PhysicalGiftRedemptionResponse;
import fr.miage.collectivit.loyaltyservice.security.CallerAccessGuard;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound web adapter exposing the merchant endpoint for in-store physical gift redemption
 * against customer loyalty points (Story SLE-43).
 */
@Validated
@RestController
@RequestMapping("/v1/loyalty/stores/{storeUid}/gifts")
@Tag(
    name = "Physical Gift Redemption",
    description = "Validation et débit immédiat de points lors de la remise d'un lot en boutique")
@RequiredArgsConstructor
@SuppressWarnings("PMD.LawOfDemeter")
public class PhysicalGiftRedemptionController {

  private static final String ROLE_MERCHANT = "MERCHANT";
  private static final String ROLE_ADMIN = "ADMIN";

  private final RedeemPhysicalGiftsUseCase redeemPhysicalGiftsUseCase;
  private final CallerAccessGuard callerAccessGuard;

  @PostMapping("/physical-redemption")
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Valider la remise physique d'un ou plusieurs cadeaux en boutique",
      description =
          "Vérifie le solde de points disponible du client, débite atomiquement le coût total en points et enregistre une transaction GIFT_REDEEM_PHYSICAL sans contrôle de stock.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Remise de cadeaux validée et points débités"),
        @ApiResponse(
            responseCode = "400",
            description = "Solde de points insuffisant, cadeau inactif ou requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (réservé au commerçant propriétaire ou administrateur)"),
        @ApiResponse(responseCode = "404", description = "Commerce ou cadeau introuvable")
      })
  public ResponseEntity<PhysicalGiftRedemptionResponse> redeemPhysicalGifts(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Valid @RequestBody PhysicalGiftRedemptionRequest requestDto,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    PhysicalGiftRedemptionResult result =
        redeemPhysicalGiftsUseCase.redeemPhysicalGifts(
            storeUid, requestDto.toCommand(), callerUserId, callerRole);

    return ResponseEntity.ok(PhysicalGiftRedemptionResponse.fromDomain(result));
  }
}
