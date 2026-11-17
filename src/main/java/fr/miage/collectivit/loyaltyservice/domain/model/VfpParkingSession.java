package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

/**
 * Domain model representing an activated 20-minute VFP free parking session and its external
 * acknowledgment ticket (SLE-78 / SLE-46).
 */
@Getter
public class VfpParkingSession {

  public static final int DURATION_MINUTES = 20;
  public static final String STATUS_CONFIRMED = "CONFIRMED";

  private final Long id;
  private final String userId;
  private final String licensePlate;
  private final String ticketReference;
  private final String status;
  private final Instant startedAt;
  private final Instant expiresAt;
  private final LocalDate sessionDate;
  private final Instant createdAt;

  public VfpParkingSession(
      Long id,
      String userId,
      String licensePlate,
      String ticketReference,
      String status,
      Instant startedAt,
      Instant expiresAt,
      LocalDate sessionDate,
      Instant createdAt) {
    this.id = id;
    this.userId = Objects.requireNonNull(userId, "userId must not be null").trim();
    this.licensePlate =
        Objects.requireNonNull(licensePlate, "licensePlate must not be null").trim();
    this.ticketReference =
        Objects.requireNonNull(ticketReference, "ticketReference must not be null").trim();
    this.status = Objects.requireNonNull(status, "status must not be null").trim();
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    this.sessionDate = Objects.requireNonNull(sessionDate, "sessionDate must not be null");
    this.createdAt = createdAt != null ? createdAt : this.startedAt;
  }

  public static VfpParkingSession confirmed(
      String userId,
      String licensePlate,
      String ticketReference,
      Instant startedAt,
      Instant expiresAt,
      LocalDate sessionDate) {
    return new VfpParkingSession(
        null,
        userId,
        licensePlate,
        ticketReference,
        STATUS_CONFIRMED,
        startedAt,
        expiresAt,
        sessionDate,
        startedAt);
  }
}
