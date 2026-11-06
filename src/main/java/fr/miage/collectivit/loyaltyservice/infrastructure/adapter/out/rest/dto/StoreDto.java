package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

/** DTO représentant les données minimales nécessaires d'un commerce renvoyées par Store Service. */
public record StoreDto(Long id, String name, Long userId) {}
