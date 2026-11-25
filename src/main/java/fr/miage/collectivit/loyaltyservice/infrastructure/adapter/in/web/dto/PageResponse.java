package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Standard generic page wrapper for API responses.
 *
 * @param <T> element type
 */
@Schema(description = "Enveloppe de pagination générique")
public record PageResponse<T>(
    @Schema(description = "Contenu de la page") List<T> content,
    @Schema(description = "Numéro de la page (0-indexé)", example = "0") int page,
    @Schema(description = "Taille de la page demandée", example = "20") int size,
    @Schema(description = "Nombre total d'éléments", example = "42") long totalElements,
    @Schema(description = "Nombre total de pages", example = "3") int totalPages,
    @Schema(description = "Indique si c'est la première page", example = "true") boolean first,
    @Schema(description = "Indique si c'est la dernière page", example = "false") boolean last) {

  public static <T> PageResponse<T> of(List<T> allItems, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, size);
    int totalElements = allItems.size();
    int totalPages = (int) Math.ceil((double) totalElements / safeSize);

    int fromIndex = Math.min(safePage * safeSize, totalElements);
    int toIndex = Math.min(fromIndex + safeSize, totalElements);
    List<T> subList = allItems.subList(fromIndex, toIndex);

    return new PageResponse<>(
        subList,
        safePage,
        safeSize,
        totalElements,
        totalPages,
        safePage == 0,
        safePage >= totalPages - 1);
  }
}
