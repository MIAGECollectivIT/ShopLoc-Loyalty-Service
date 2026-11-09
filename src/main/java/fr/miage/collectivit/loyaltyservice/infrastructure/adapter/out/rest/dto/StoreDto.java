package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

/** DTO representing minimal store data returned by Store Service. */
public record StoreDto(Long id, String name, Long userId) {}
