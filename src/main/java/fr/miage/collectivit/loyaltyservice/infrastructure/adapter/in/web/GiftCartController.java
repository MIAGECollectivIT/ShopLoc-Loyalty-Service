package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.application.service.GiftCartEligibilityService;
import fr.miage.collectivit.loyaltyservice.application.service.LoyaltyPointsHoldService;
import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.CreateLoyaltyHoldRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftCartEligibilityResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.LoyaltyHoldResponse;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Web adapter for Click & Collect gift cart eligibility and points holds (SLE-41 / SLE-292). */
@Validated
@RestController
@RequestMapping("/v1/loyalty")
@Tag(
    name = "Gift Cart Eligibility & Holds",
    description = "Vérification d'éligibilité panier et mise en attente des points Click & Collect")
@RequiredArgsConstructor
public class GiftCartController {

  private static final String ROLE_CLIENT = "CLIENT";
  private static final String ROLE_MERCHANT = "MERCHANT";
  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_SERVICE = "SERVICE";

  private final GiftCartEligibilityService eligibilityService;
  private final LoyaltyPointsHoldService holdService;

  @GetMapping("/gifts/eligibility")
  @RequiresRole({ROLE_CLIENT, ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Vérifier l'éligibilité du client pour l'ajout de cadeaux au panier",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<GiftCartEligibilityResponse> getEligibility(
      @RequestParam(required = false) UUID storeUid, HttpServletRequest request) {
    String userId = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    return ResponseEntity.ok(eligibilityService.getEligibility(userId, storeUid));
  }

  @PostMapping("/holds")
  @RequiresRole({ROLE_ADMIN, ROLE_SERVICE})
  @Operation(
      summary = "Créer une mise en attente de points et réserver le stock cadeau (inter-service)",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<LoyaltyHoldResponse> createHold(
      @Valid @RequestBody CreateLoyaltyHoldRequest requestDto) {
    LoyaltyHoldResponse response = holdService.createHold(requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/holds/{orderReference}")
  @RequiresRole({ROLE_ADMIN, ROLE_SERVICE})
  @Operation(
      summary =
          "Libérer une mise en attente de points et restaurer le stock cadeau (inter-service)",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<Void> releaseHold(@PathVariable String orderReference) {
    holdService.releaseHold(orderReference);
    return ResponseEntity.noContent().build();
  }
}
