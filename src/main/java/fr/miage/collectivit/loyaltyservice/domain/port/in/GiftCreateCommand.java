package fr.miage.collectivit.loyaltyservice.domain.port.in;

/** Command record for creating a new loyalty gift item. */
public record GiftCreateCommand(
    String name, String description, Integer pointsCost, Integer stock, String imageUrl) {}
