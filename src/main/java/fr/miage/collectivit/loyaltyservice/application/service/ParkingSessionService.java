package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingSessionAlreadyUsedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.exception.VehicleNotRegisteredException;
import fr.miage.collectivit.loyaltyservice.domain.model.FrenchLicensePlateNormalizer;
import fr.miage.collectivit.loyaltyservice.domain.model.ParkingGatewayTicket;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ActivateParkingSessionUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingGatewayPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingSessionPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVehiclesPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Application service orchestrating VFP parking session validation, third-party acknowledgment
 * retrieval, and persistence (SLE-78 / SLE-46).
 */
@Slf4j
@Service
public class ParkingSessionService implements ActivateParkingSessionUseCase {

  public static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Paris");
  private static final String TICKET_PREFIX = "PARK-";

  private final UserVfpPersistencePort userVfpPersistencePort;
  private final UserVehiclesPort userVehiclesPort;
  private final ParkingGatewayPort parkingGatewayPort;
  private final ParkingSessionPersistencePort parkingSessionPersistencePort;
  private final Clock clock;

  @Autowired
  public ParkingSessionService(
      UserVfpPersistencePort userVfpPersistencePort,
      UserVehiclesPort userVehiclesPort,
      ParkingGatewayPort parkingGatewayPort,
      ParkingSessionPersistencePort parkingSessionPersistencePort) {
    this(
        userVfpPersistencePort,
        userVehiclesPort,
        parkingGatewayPort,
        parkingSessionPersistencePort,
        Clock.systemUTC());
  }

  public ParkingSessionService(
      UserVfpPersistencePort userVfpPersistencePort,
      UserVehiclesPort userVehiclesPort,
      ParkingGatewayPort parkingGatewayPort,
      ParkingSessionPersistencePort parkingSessionPersistencePort,
      Clock clock) {
    this.userVfpPersistencePort = userVfpPersistencePort;
    this.userVehiclesPort = userVehiclesPort;
    this.parkingGatewayPort = parkingGatewayPort;
    this.parkingSessionPersistencePort = parkingSessionPersistencePort;
    this.clock = clock;
  }

  @Override
  public VfpParkingSession activateSession(String userId, String licensePlate) {
    String cleanUserId = validateUserId(userId);
    String normalizedPlate = FrenchLicensePlateNormalizer.validateAndNormalize(licensePlate);

    verifyActiveVfpStatus(cleanUserId);

    Instant now = clock.instant();
    LocalDate sessionDate = LocalDate.ofInstant(now, BUSINESS_ZONE);
    ensureNoSessionUsedToday(cleanUserId, sessionDate);
    verifyVehicleOwnership(cleanUserId, normalizedPlate);

    ParkingGatewayTicket ticket =
        parkingGatewayPort.requestParkingTicket(
            cleanUserId, normalizedPlate, VfpParkingSession.DURATION_MINUTES);
    validateTicketResponse(ticket);

    VfpParkingSession session =
        buildConfirmedSession(cleanUserId, normalizedPlate, ticket, now, sessionDate);
    VfpParkingSession saved = saveConfirmedSession(session);

    if (log.isInfoEnabled()) {
      log.info(
          "VFP parking session confirmed: userId={}, plate={}, ticketReference={}, expiresAt={}",
          saved.getUserId(),
          saved.getLicensePlate(),
          saved.getTicketReference(),
          saved.getExpiresAt());
    }
    return saved;
  }

  private String validateUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID must not be blank");
    }
    return userId.trim();
  }

  private void ensureNoSessionUsedToday(String userId, LocalDate sessionDate) {
    if (parkingSessionPersistencePort.existsForUserOnDate(userId, sessionDate)) {
      throw new ParkingSessionAlreadyUsedException(
          "Daily free VFP parking session has already been activated today");
    }
  }

  private VfpParkingSession buildConfirmedSession(
      String userId,
      String normalizedPlate,
      ParkingGatewayTicket ticket,
      Instant fallbackNow,
      LocalDate sessionDate) {
    Instant startedAt = ticket.startedAt() != null ? ticket.startedAt() : fallbackNow;
    Instant expiresAt = startedAt.plus(Duration.ofMinutes(VfpParkingSession.DURATION_MINUTES));
    String ticketReference =
        hasText(ticket.ticketReference())
            ? ticket.ticketReference().trim()
            : TICKET_PREFIX + UUID.randomUUID();
    return VfpParkingSession.confirmed(
        userId, normalizedPlate, ticketReference, startedAt, expiresAt, sessionDate);
  }

  private VfpParkingSession saveConfirmedSession(VfpParkingSession session) {
    try {
      return parkingSessionPersistencePort.save(session);
    } catch (DataIntegrityViolationException ex) {
      throw new ParkingSessionAlreadyUsedException(
          "Daily free VFP parking session has already been activated today", ex);
    }
  }

  private void verifyActiveVfpStatus(String userId) {
    UserVfpStatus status =
        userVfpPersistencePort
            .findByUserId(userId)
            .orElseThrow(
                () ->
                    new UserNotVfpException(
                        "Free parking session is reserved for users with an active VFP status"));
    if (!status.isVfp()) {
      throw new UserNotVfpException(
          "Free parking session is reserved for users with an active VFP status");
    }
  }

  private void verifyVehicleOwnership(String userId, String normalizedPlate) {
    List<String> registeredPlates = userVehiclesPort.getUserLicensePlates(userId);
    boolean owned =
        registeredPlates != null
            && registeredPlates.stream()
                .map(FrenchLicensePlateNormalizer::normalize)
                .anyMatch(normalizedPlate::equalsIgnoreCase);
    if (!owned) {
      throw new VehicleNotRegisteredException(
          "License plate " + normalizedPlate + " is not registered on the user's profile");
    }
  }

  private void validateTicketResponse(ParkingGatewayTicket ticket) {
    if (ticket == null || !VfpParkingSession.STATUS_CONFIRMED.equals(ticket.status())) {
      String receivedStatus = ticket != null ? ticket.status() : "null";
      throw new ParkingGatewayUnavailableException(
          "Parking gateway rejected or returned invalid session status: " + receivedStatus);
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
