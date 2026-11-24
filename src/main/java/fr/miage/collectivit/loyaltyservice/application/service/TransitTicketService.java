package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.TransitTicketAlreadyUsedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.model.TransitGatewayTicket;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ActivateTransitTicketUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingSessionPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitGatewayPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitTicketPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Application service managing VFP public transit ticket activation, non-cumulation validation
 * (SLE-48), gateway communication, and persistence (SLE-47).
 */
@Slf4j
@Service
public class TransitTicketService implements ActivateTransitTicketUseCase {

  public static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Paris");
  private static final String TICKET_PREFIX = "TRN-";

  private final UserVfpPersistencePort userVfpPersistencePort;
  private final TransitGatewayPort transitGatewayPort;
  private final TransitTicketPersistencePort transitTicketPersistencePort;
  private final ParkingSessionPersistencePort parkingSessionPersistencePort;
  private final Clock clock;

  @Autowired
  public TransitTicketService(
      UserVfpPersistencePort userVfpPersistencePort,
      TransitGatewayPort transitGatewayPort,
      TransitTicketPersistencePort transitTicketPersistencePort,
      ParkingSessionPersistencePort parkingSessionPersistencePort) {
    this(
        userVfpPersistencePort,
        transitGatewayPort,
        transitTicketPersistencePort,
        parkingSessionPersistencePort,
        Clock.systemUTC());
  }

  public TransitTicketService(
      UserVfpPersistencePort userVfpPersistencePort,
      TransitGatewayPort transitGatewayPort,
      TransitTicketPersistencePort transitTicketPersistencePort,
      ParkingSessionPersistencePort parkingSessionPersistencePort,
      Clock clock) {
    this.userVfpPersistencePort = userVfpPersistencePort;
    this.transitGatewayPort = transitGatewayPort;
    this.transitTicketPersistencePort = transitTicketPersistencePort;
    this.parkingSessionPersistencePort = parkingSessionPersistencePort;
    this.clock = clock;
  }

  @Override
  @SuppressWarnings("PMD.LawOfDemeter")
  public VfpTransitTicket activateTicket(String userId) {
    String cleanUserId = validateUserId(userId);

    boolean isVfp =
        userVfpPersistencePort.findByUserId(cleanUserId).map(UserVfpStatus::isVfp).orElse(false);
    if (!isVfp) {
      throw new UserNotVfpException("User " + cleanUserId + " does not hold an active VFP status");
    }

    LocalDate sessionDate = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
    ensureNoBenefitUsedToday(cleanUserId, sessionDate);

    TransitGatewayTicket gatewayTicket =
        transitGatewayPort.requestTransitTicket(cleanUserId, VfpTransitTicket.DURATION_MINUTES);

    Instant fallbackNow = clock.instant();
    VfpTransitTicket domainTicket =
        buildConfirmedTicket(cleanUserId, gatewayTicket, fallbackNow, sessionDate);

    VfpTransitTicket saved;
    try {
      saved = transitTicketPersistencePort.save(domainTicket);
    } catch (DataIntegrityViolationException ex) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Concurrent attempt to activate daily VFP transit ticket for user {} on date {}: {}",
            cleanUserId,
            sessionDate,
            ex.getMessage());
      }
      throw new TransitTicketAlreadyUsedException(
          "Daily free VFP transit ticket or parking session has already been activated today", ex);
    }

    if (log.isInfoEnabled()) {
      log.info(
          "VFP transit ticket confirmed: userId={}, ticketReference={}, ticketCode={}, expiresAt={}",
          saved.getUserId(),
          saved.getTicketReference(),
          saved.getTicketCode(),
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

  private void ensureNoBenefitUsedToday(String userId, LocalDate sessionDate) {
    if (transitTicketPersistencePort.existsForUserOnDate(userId, sessionDate)) {
      throw new TransitTicketAlreadyUsedException(
          "Daily free VFP transit ticket has already been activated today");
    }
    if (parkingSessionPersistencePort.existsForUserOnDate(userId, sessionDate)) {
      throw new TransitTicketAlreadyUsedException(
          "Daily institutional benefit already consumed today (parking session active or used)");
    }
  }

  private VfpTransitTicket buildConfirmedTicket(
      String userId, TransitGatewayTicket ticket, Instant fallbackNow, LocalDate sessionDate) {
    Instant startedAt = ticket.startedAt() != null ? ticket.startedAt() : fallbackNow;
    Instant expiresAt =
        ticket.expiresAt() != null
            ? ticket.expiresAt()
            : startedAt.plus(Duration.ofMinutes(VfpTransitTicket.DURATION_MINUTES));
    String ticketReference =
        ticket.ticketReference() != null && !ticket.ticketReference().isBlank()
            ? ticket.ticketReference().trim()
            : TICKET_PREFIX + UUID.randomUUID();
    String ticketCode =
        ticket.ticketCode() != null && !ticket.ticketCode().isBlank()
            ? ticket.ticketCode().trim()
            : "BUS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    String networkName =
        ticket.networkName() != null && !ticket.networkName().isBlank()
            ? ticket.networkName().trim()
            : VfpTransitTicket.DEFAULT_NETWORK;

    return VfpTransitTicket.confirmed(
        userId, ticketReference, ticketCode, networkName, startedAt, expiresAt, sessionDate);
  }
}
