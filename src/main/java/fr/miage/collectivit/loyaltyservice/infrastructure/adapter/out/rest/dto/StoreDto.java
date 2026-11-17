package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

/** DTO representing store data returned by Store Service. */
public record StoreDto(Long id, String name, String category, Long userId) {

  public StoreDto(Long id, String name, Long userId) {
    this(id, name, null, userId);
  }
}
