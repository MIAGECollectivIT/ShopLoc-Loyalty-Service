package fr.miage.collectivit.loyaltyservice.domain.port.in;

/** Command record for updating an existing loyalty gift item. */
public record GiftUpdateCommand(
    String name, String description, Integer pointsCost, String imageUrl) {}
