package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetGiftCatalogQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ManageGiftCatalogUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftCreateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftStockAdjustRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftUpdateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.PageResponse;
import fr.miage.collectivit.loyaltyservice.security.CallerAccessGuard;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.loyaltyservice.security.PublicEndpoint;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Primary inbound web adapter exposing REST endpoints for the partner store gift catalog (SLE-60).
 */
@Validated
@RestController
@RequestMapping("/v1/loyalty/stores/{storeUid}/gifts")
@Tag(
    name = "Loyalty Gift Catalog",
    description = "Gestion du catalogue de cadeaux fidélité des commerces")
@RequiredArgsConstructor
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.AvoidDuplicateLiterals",
  "PMD.CloseResource",
  "PMD.LawOfDemeter",
  "PMD.TooManyMethods"
})
public class GiftCatalogController {

  private static final String ROLE_MERCHANT = "MERCHANT";
  private static final String ROLE_ADMIN = "ADMIN";

  private final ManageGiftCatalogUseCase manageGiftCatalogUseCase;
  private final GetGiftCatalogQuery getGiftCatalogQuery;
  private final CallerAccessGuard callerAccessGuard;

  @GetMapping
  @PublicEndpoint
  @Operation(
      summary = "Lister les cadeaux fidélité d'un commerce",
      description = "Retourne la liste des articles cadeaux d'un commerce. Accès public.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Liste des cadeaux récupérée")})
  public ResponseEntity<List<GiftResponse>> getGifts(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(description = "Inclure les articles inactifs")
          @RequestParam(required = false, defaultValue = "false")
          boolean includeInactive) {
    List<GiftResponse> responses =
        getGiftCatalogQuery.getGiftsByStore(storeUid, includeInactive).stream()
            .map(GiftResponse::fromDomain)
            .toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/page")
  @PublicEndpoint
  @Operation(
      summary = "Lister les cadeaux fidélité d'un commerce (paginé)",
      description = "Retourne la page des articles cadeaux d'un commerce. Accès public.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Page des cadeaux récupérée")})
  public ResponseEntity<PageResponse<GiftResponse>> getGiftsPage(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(description = "Inclure les articles inactifs")
          @RequestParam(required = false, defaultValue = "false")
          boolean includeInactive,
      @Parameter(description = "Numéro de page (0-indexé)", example = "0")
          @RequestParam(required = false, defaultValue = "0")
          int page,
      @Parameter(description = "Taille de la page", example = "20")
          @RequestParam(required = false, defaultValue = "20")
          int size) {
    List<GiftResponse> responses =
        getGiftCatalogQuery.getGiftsByStore(storeUid, includeInactive).stream()
            .map(GiftResponse::fromDomain)
            .toList();
    return ResponseEntity.ok(PageResponse.of(responses, page, size));
  }

  @GetMapping("/{giftUid}")
  @PublicEndpoint
  @Operation(
      summary = "Consulter le détail d'un article cadeau",
      description =
          "Retourne le détail d'un article cadeau du catalogue d'un commerce. Accès public.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Article cadeau trouvé"),
        @ApiResponse(responseCode = "404", description = "Article introuvable")
      })
  public ResponseEntity<GiftResponse> getGiftByUid(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(
              description = "Identifiant UUID de l'article cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          @PathVariable
          UUID giftUid) {
    LoyaltyGift gift = getGiftCatalogQuery.getGiftByUid(storeUid, giftUid);
    return ResponseEntity.ok(GiftResponse.fromDomain(gift));
  }

  @GetMapping("/{giftUid}/image")
  @PublicEndpoint
  @Operation(
      summary = "Récupérer l'image binaire d'un article cadeau",
      description =
          "Retourne l'image binaire stockée sur S3 avec en-tête Cache-Control. Accès public.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Image récupérée"),
        @ApiResponse(responseCode = "404", description = "Article ou image introuvable")
      })
  public ResponseEntity<byte[]> getGiftImage(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(
              description = "Identifiant UUID de l'article cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          @PathVariable
          UUID giftUid) {
    return getGiftCatalogQuery
        .getGiftImage(storeUid, giftUid)
        .map(
            resource ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resource.contentType()))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header("Content-Security-Policy", "default-src 'none'")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource.bytes()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Ajouter un article au catalogue de cadeaux avec photo S3 (multipart/form-data)",
      description =
          "Crée un nouvel article cadeau avec photo uploadée sur S3. Réservé au commerçant propriétaire ou administrateur.",
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
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Valid @RequestPart("gift") GiftCreateRequest requestDto,
      @RequestPart(value = "image", required = false) MultipartFile image,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    InputStream imageStream = null;
    String contentType = null;
    String originalFilename = null;
    Long size = null;
    if (image != null && !image.isEmpty()) {
      try {
        imageStream = image.getInputStream();
        contentType = image.getContentType();
        originalFilename = image.getOriginalFilename();
        size = image.getSize();
      } catch (IOException e) {
        throw new IllegalArgumentException("Impossible de lire l'image transmise", e);
      }
    }

    LoyaltyGift created =
        manageGiftCatalogUseCase.createGift(
            storeUid,
            requestDto.toCommand(),
            imageStream,
            size != null ? size : 0L,
            originalFilename,
            contentType,
            callerUserId,
            callerRole);
    URI location =
        URI.create(String.format("/v1/loyalty/stores/%s/gifts/%s", storeUid, created.getUid()));
    return ResponseEntity.created(location).body(GiftResponse.fromDomain(created));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Ajouter un article au catalogue de cadeaux (JSON)",
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
  public ResponseEntity<GiftResponse> createGiftJson(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Valid @RequestBody GiftCreateRequest requestDto,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    LoyaltyGift created =
        manageGiftCatalogUseCase.createGift(
            storeUid, requestDto.toCommand(), callerUserId, callerRole);
    URI location =
        URI.create(String.format("/v1/loyalty/stores/%s/gifts/%s", storeUid, created.getUid()));
    return ResponseEntity.created(location).body(GiftResponse.fromDomain(created));
  }

  @PutMapping(value = "/{giftUid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Modifier un article cadeau avec photo S3 (multipart/form-data)",
      description = "Met à jour les informations d'un article cadeau avec nouvelle photo S3.",
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
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(
              description = "Identifiant UUID de l'article cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          @PathVariable
          UUID giftUid,
      @Valid @RequestPart("gift") GiftUpdateRequest requestDto,
      @RequestPart(value = "image", required = false) MultipartFile image,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    InputStream imageStream = null;
    String contentType = null;
    String originalFilename = null;
    Long size = null;
    if (image != null && !image.isEmpty()) {
      try {
        imageStream = image.getInputStream();
        contentType = image.getContentType();
        originalFilename = image.getOriginalFilename();
        size = image.getSize();
      } catch (IOException e) {
        throw new IllegalArgumentException("Impossible de lire l'image transmise", e);
      }
    }

    LoyaltyGift updated =
        manageGiftCatalogUseCase.updateGift(
            storeUid,
            giftUid,
            requestDto.toCommand(),
            imageStream,
            size != null ? size : 0L,
            originalFilename,
            contentType,
            callerUserId,
            callerRole);
    return ResponseEntity.ok(GiftResponse.fromDomain(updated));
  }

  @PutMapping(value = "/{giftUid}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @RequiresRole({ROLE_MERCHANT, ROLE_ADMIN})
  @Operation(
      summary = "Modifier un article cadeau (JSON)",
      description = "Met à jour les informations d'un article cadeau existant sans nouvelle photo.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Article cadeau mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Article ou commerce introuvable")
      })
  public ResponseEntity<GiftResponse> updateGiftJson(
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(
              description = "Identifiant UUID de l'article cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          @PathVariable
          UUID giftUid,
      @Valid @RequestBody GiftUpdateRequest requestDto,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    LoyaltyGift updated =
        manageGiftCatalogUseCase.updateGift(
            storeUid, giftUid, requestDto.toCommand(), callerUserId, callerRole);
    return ResponseEntity.ok(GiftResponse.fromDomain(updated));
  }

  @PatchMapping("/{giftUid}/stock")
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
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(
              description = "Identifiant UUID de l'article cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          @PathVariable
          UUID giftUid,
      @Valid @RequestBody GiftStockAdjustRequest requestDto,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    LoyaltyGift adjusted =
        manageGiftCatalogUseCase.adjustStock(
            storeUid, giftUid, requestDto.delta(), callerUserId, callerRole);
    return ResponseEntity.ok(GiftResponse.fromDomain(adjusted));
  }

  @DeleteMapping("/{giftUid}")
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
      @Parameter(
              description = "Identifiant UUID du commerce",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID storeUid,
      @Parameter(
              description = "Identifiant UUID de l'article cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          @PathVariable
          UUID giftUid,
      HttpServletRequest request) {
    UUID callerUserId = callerAccessGuard.resolveAuthenticatedUserUuid(request);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    manageGiftCatalogUseCase.deleteGift(storeUid, giftUid, callerUserId, callerRole);
    return ResponseEntity.noContent().build();
  }
}
