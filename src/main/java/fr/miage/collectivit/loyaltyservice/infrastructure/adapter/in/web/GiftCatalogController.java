package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetGiftCatalogQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ManageGiftCatalogUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftCreateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftStockAdjustRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftUpdateRequest;
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
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound web adapter exposing REST endpoints for the partner store gift catalog (SLE-60).
 */
@Validated
@RestController
@RequestMapping("/v1/loyalty/stores/{storeId}/gifts")
@Tag(
    name = "Loyalty Gift Catalog",
    description = "Gestion du catalogue de cadeaux fidélité des commerces")
@RequiredArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
public class GiftCatalogController {

  private static final String ROLE_MERCHANT = "MERCHANT";
  private static final String ROLE_ADMIN = "ADMIN";

  private final ManageGiftCatalogUseCase manageGiftCatalogUseCase;
  private final GetGiftCatalogQuery getGiftCatalogQuery;

  @GetMapping
  @Operation(
      summary = "Lister les cadeaux fidélité d'un commerce",
      description = "Retourne la liste des articles cadeaux d'un commerce. Accès public.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Liste des cadeaux récupérée")})
  public ResponseEntity<List<GiftResponse>> getGifts(
      @Parameter(description = "Identifiant du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Parameter(description = "Inclure les articles inactifs")
          @RequestParam(required = false, defaultValue = "false")
          boolean includeInactive) {
    List<GiftResponse> responses =
        getGiftCatalogQuery.getGiftsByStore(storeId, includeInactive).stream()
            .map(GiftResponse::fromDomain)
            .toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{giftId}")
  @Operation(
      summary = "Consulter le détail d'un article cadeau",
      description =
          "Retourne le détail d'un article cadeau du catalogue d'un commerce. Accès public.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Article cadeau trouvé"),
        @ApiResponse(responseCode = "404", description = "Article introuvable")
      })
  public ResponseEntity<GiftResponse> getGiftById(
      @Parameter(description = "Identifiant du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Parameter(description = "Identifiant de l'article cadeau", example = "1")
          @PathVariable
          @Positive(message = "Gift identifier must be positive")
          Long giftId) {
    LoyaltyGift gift = getGiftCatalogQuery.getGiftById(storeId, giftId);
    return ResponseEntity.ok(GiftResponse.fromDomain(gift));
  }

  @PostMapping
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Ajouter un article au catalogue de cadeaux",
      description =
          "Crée un nouvel article cadeau pour le commerce. Réservé au commerçant propriétaire ou administrateur.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Article cadeau créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Commerce introuvable")
      })
  public ResponseEntity<GiftResponse> createGift(
      @Parameter(description = "Identifiant du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Valid @RequestBody GiftCreateRequest requestDto,
      HttpServletRequest request) {
    Long callerUserId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    LoyaltyGift created =
        manageGiftCatalogUseCase.createGift(
            storeId, requestDto.toCommand(), callerUserId, callerRole);
    URI location =
        URI.create(String.format("/v1/loyalty/stores/%d/gifts/%d", storeId, created.getId()));
    return ResponseEntity.created(location).body(GiftResponse.fromDomain(created));
  }

  @PutMapping("/{giftId}")
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Modifier un article cadeau",
      description = "Met à jour les informations d'un article cadeau existant.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Article cadeau mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Article ou commerce introuvable")
      })
  public ResponseEntity<GiftResponse> updateGift(
      @Parameter(description = "Identifiant du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Parameter(description = "Identifiant de l'article cadeau", example = "1")
          @PathVariable
          @Positive(message = "Gift identifier must be positive")
          Long giftId,
      @Valid @RequestBody GiftUpdateRequest requestDto,
      HttpServletRequest request) {
    Long callerUserId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    LoyaltyGift updated =
        manageGiftCatalogUseCase.updateGift(
            storeId, giftId, requestDto.toCommand(), callerUserId, callerRole);
    return ResponseEntity.ok(GiftResponse.fromDomain(updated));
  }

  @PatchMapping("/{giftId}/stock")
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Ajuster le stock d'un article cadeau (SLE-193)",
      description = "Ajuste le stock dédié (+/- delta) d'un article cadeau.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Stock ajusté avec succès"),
        @ApiResponse(
            responseCode = "400",
            description = "Ajustement invalide ou stock insuffisant"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Article introuvable")
      })
  public ResponseEntity<GiftResponse> adjustStock(
      @Parameter(description = "Identifiant du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Parameter(description = "Identifiant de l'article cadeau", example = "1")
          @PathVariable
          @Positive(message = "Gift identifier must be positive")
          Long giftId,
      @Valid @RequestBody GiftStockAdjustRequest requestDto,
      HttpServletRequest request) {
    Long callerUserId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    LoyaltyGift adjusted =
        manageGiftCatalogUseCase.adjustStock(
            storeId, giftId, requestDto.delta(), callerUserId, callerRole);
    return ResponseEntity.ok(GiftResponse.fromDomain(adjusted));
  }

  @DeleteMapping("/{giftId}")
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Supprimer un article cadeau",
      description = "Supprime un article cadeau du catalogue.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Article supprimé avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Article introuvable")
      })
  public ResponseEntity<Void> deleteGift(
      @Parameter(description = "Identifiant du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Parameter(description = "Identifiant de l'article cadeau", example = "1")
          @PathVariable
          @Positive(message = "Gift identifier must be positive")
          Long giftId,
      HttpServletRequest request) {
    Long callerUserId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    manageGiftCatalogUseCase.deleteGift(storeId, giftId, callerUserId, callerRole);
    return ResponseEntity.noContent().build();
  }
}
