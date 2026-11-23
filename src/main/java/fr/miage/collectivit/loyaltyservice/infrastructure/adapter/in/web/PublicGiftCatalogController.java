package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.domain.port.in.GetGiftCatalogQuery;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftResponse;
import fr.miage.collectivit.loyaltyservice.security.PublicEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public REST adapter exposing the global active gift catalog for customers (SLE-39 / SLE-223). */
@PublicEndpoint
@Validated
@RestController
@RequestMapping("/v1/loyalty/gifts")
@Tag(
    name = "Public Loyalty Gift Catalog",
    description = "Consultation publique du catalogue global des cadeaux fidélité")
@RequiredArgsConstructor
public class PublicGiftCatalogController {

  private final GetGiftCatalogQuery getGiftCatalogQuery;

  @GetMapping
  @Operation(
      summary = "Consulter le catalogue global des cadeaux actifs",
      description =
          "Retourne la liste publique des cadeaux fidélité actifs avec informations commerçant, filtrable par commerce et catégorie.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Catalogue des cadeaux récupéré")})
  public ResponseEntity<List<GiftResponse>> getPublicGifts(
      @Parameter(
              description = "Identifiant UUID optionnel d'un commerce partenaire",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @RequestParam(required = false)
          UUID storeUid,
      @Parameter(
              description = "Catégorie optionnelle de commerce ou cadeau",
              example = "Restauration")
          @RequestParam(required = false)
          String category) {
    List<GiftResponse> responses =
        getGiftCatalogQuery.getPublicCatalog(storeUid, category).stream()
            .map(GiftResponse::fromCatalogItem)
            .toList();
    return ResponseEntity.ok(responses);
  }
}
