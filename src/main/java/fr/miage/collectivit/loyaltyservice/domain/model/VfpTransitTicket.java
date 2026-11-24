package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/** Domain entity representing an issued VFP public transit ticket (SLE-47). */
@Getter
public class VfpTransitTicket {

  public static final int DURATION_MINUTES = 60;
  public static final String STATUS_CONFIRMED = "CONFIRMED";
  public static final String DEFAULT_NETWORK = "Réseau Urbain Collectiv'IT";

  private final Long id;
  private final UUID uid;
  private final String userId;
  private final String ticketReference;
  private final String ticketCode;
  private final String networkName;
  private final String status;
  private final Instant startedAt;
  private final Instant expiresAt;
  private final LocalDate ticketDate;
  private final Instant createdAt;

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public VfpTransitTicket(
      Long id,
      UUID uid,
      String userId,
      String ticketReference,
      String ticketCode,
      String networkName,
      String status,
      Instant startedAt,
      Instant expiresAt,
      LocalDate ticketDate,
      Instant createdAt) {
    this.id = id;
    this.uid = uid != null ? uid : UUID.randomUUID();
    this.userId = Objects.requireNonNull(userId, "userId must not be null").trim();
    this.ticketReference =
        Objects.requireNonNull(ticketReference, "ticketReference must not be null").trim();
    this.ticketCode = Objects.requireNonNull(ticketCode, "ticketCode must not be null").trim();
    this.networkName =
        networkName != null && !networkName.isBlank() ? networkName.trim() : DEFAULT_NETWORK;
    this.status = Objects.requireNonNull(status, "status must not be null").trim();
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    this.ticketDate = Objects.requireNonNull(ticketDate, "ticketDate must not be null");
    this.createdAt = createdAt != null ? createdAt : this.startedAt;
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public static VfpTransitTicket confirmed(
      String userId,
      String ticketReference,
      String ticketCode,
      String networkName,
      Instant startedAt,
      Instant expiresAt,
      LocalDate ticketDate) {
    return new VfpTransitTicket(
        null,
        UUID.randomUUID(),
        userId,
        ticketReference,
        ticketCode,
        networkName,
        STATUS_CONFIRMED,
        startedAt,
        expiresAt,
        ticketDate,
        Instant.now());
  }
}
