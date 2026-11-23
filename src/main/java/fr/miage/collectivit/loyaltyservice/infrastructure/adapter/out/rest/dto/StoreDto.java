package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

import java.util.UUID;

/** DTO representing store data returned by Store Service. */
public record StoreDto(UUID uid, String name, String category, UUID ownerUid) {

  public StoreDto(UUID uid, String name, UUID ownerUid) {
    this(uid, name, null, ownerUid);
  }
}
