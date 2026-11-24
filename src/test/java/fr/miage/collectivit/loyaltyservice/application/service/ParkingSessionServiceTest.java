package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLicensePlateException;
import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingSessionAlreadyUsedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.exception.VehicleNotRegisteredException;
import fr.miage.collectivit.loyaltyservice.domain.model.ParkingGatewayTicket;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpDailyEligibility;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingGatewayPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingSessionPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitTicketPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVehiclesPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application Service - ParkingSessionService Test (SLE-78)")
class ParkingSessionServiceTest {

  private static final String USER_ID = "user-123";
  private static final Instant FIXED_NOW = Instant.parse("2026-11-17T14:00:00Z");
  private static final LocalDate FIXED_DATE = LocalDate.of(2026, 11, 17);

  @Mock private UserVfpPersistencePort userVfpPersistencePort;
  @Mock private UserVehiclesPort userVehiclesPort;
  @Mock private ParkingGatewayPort parkingGatewayPort;
  @Mock private ParkingSessionPersistencePort parkingSessionPersistencePort;
  @Mock private TransitTicketPersistencePort transitTicketPersistencePort;

  private ParkingSessionService service;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    service =
        new ParkingSessionService(
            userVfpPersistencePort,
            userVehiclesPort,
            parkingGatewayPort,
            parkingSessionPersistencePort,
            transitTicketPersistencePort,
            fixedClock);
  }

  @Test
  @DisplayName(
      "Should confirm, normalize plate, set 20m expiration and persist when VFP owns plate")
  void shouldActivateAndAcknowledgeSessionSuccessfully() {
    UserVfpStatus activeVfp =
        new UserVfpStatus(USER_ID, true, FIXED_NOW.minusSeconds(3600), null, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, FIXED_DATE)).thenReturn(false);
    when(userVehiclesPort.getUserLicensePlates(USER_ID)).thenReturn(List.of("AA-123-BB"));
    when(parkingGatewayPort.requestParkingTicket(USER_ID, "AA-123-BB", 20))
        .thenReturn(
            new ParkingGatewayTicket(
                "CONFIRMED",
                "PARK-TICKET-001",
                "AA-123-BB",
                20,
                FIXED_NOW,
                FIXED_NOW.plusSeconds(1200)));
    when(parkingSessionPersistencePort.save(any(VfpParkingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VfpParkingSession result = service.activateSession(USER_ID, "aa123bb");

    assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    assertThat(result.getTicketReference()).isEqualTo("PARK-TICKET-001");
    assertThat(result.getLicensePlate()).isEqualTo("AA-123-BB");
    assertThat(result.getStartedAt()).isEqualTo(FIXED_NOW);
    assertThat(result.getExpiresAt()).isEqualTo(Instant.parse("2026-11-17T14:20:00Z"));
    verify(parkingSessionPersistencePort).save(any(VfpParkingSession.class));
  }

  @Test
  @DisplayName("Should reject when user is not VFP")
  void shouldThrowWhenUserNotVfp() {
    UserVfpStatus inactiveVfp = new UserVfpStatus(USER_ID, false, null, FIXED_NOW, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(inactiveVfp));

    assertThatThrownBy(() -> service.activateSession(USER_ID, "AA-123-BB"))
        .isInstanceOf(UserNotVfpException.class);
    verify(parkingGatewayPort, never()).requestParkingTicket(any(), any(), any(Integer.class));
  }

  @Test
  @DisplayName("Should reject when license plate format is invalid")
  void shouldThrowWhenPlateInvalid() {
    assertThatThrownBy(() -> service.activateSession(USER_ID, "INVALID"))
        .isInstanceOf(InvalidLicensePlateException.class);
  }

  @Test
  @DisplayName("Should reject when plate is not registered on user profile")
  void shouldThrowWhenPlateNotOwned() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, FIXED_DATE)).thenReturn(false);
    when(userVehiclesPort.getUserLicensePlates(USER_ID)).thenReturn(List.of("CC-456-DD"));

    assertThatThrownBy(() -> service.activateSession(USER_ID, "AA-123-BB"))
        .isInstanceOf(VehicleNotRegisteredException.class);
    verify(parkingGatewayPort, never()).requestParkingTicket(any(), any(), any(Integer.class));
  }

  @Test
  @DisplayName("Should reject when daily parking session was already used today")
  void shouldThrowWhenDailySessionAlreadyUsed() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, FIXED_DATE)).thenReturn(true);

    assertThatThrownBy(() -> service.activateSession(USER_ID, "AA-123-BB"))
        .isInstanceOf(ParkingSessionAlreadyUsedException.class);
    verify(parkingGatewayPort, never()).requestParkingTicket(any(), any(), any(Integer.class));
  }

  @Test
  @DisplayName("Should throw ParkingGatewayUnavailableException when gateway rejects status")
  void shouldThrowWhenGatewayReturnsNonConfirmed() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, FIXED_DATE)).thenReturn(false);
    when(userVehiclesPort.getUserLicensePlates(USER_ID)).thenReturn(List.of("AA-123-BB"));
    when(parkingGatewayPort.requestParkingTicket(USER_ID, "AA-123-BB", 20))
        .thenReturn(new ParkingGatewayTicket("REJECTED", null, "AA-123-BB", 20, null, null));

    assertThatThrownBy(() -> service.activateSession(USER_ID, "AA-123-BB"))
        .isInstanceOf(ParkingGatewayUnavailableException.class);
    verify(parkingSessionPersistencePort, never()).save(any());
  }

  @Test
  @DisplayName(
      "Should translate concurrent DataIntegrityViolationException into ParkingSessionAlreadyUsedException")
  void shouldTranslateConcurrentDuplicateSaveIntoConflict() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, FIXED_DATE)).thenReturn(false);
    when(userVehiclesPort.getUserLicensePlates(USER_ID)).thenReturn(List.of("AA-123-BB"));
    when(parkingGatewayPort.requestParkingTicket(USER_ID, "AA-123-BB", 20))
        .thenReturn(
            new ParkingGatewayTicket(
                "CONFIRMED",
                "PARK-CONC-001",
                "AA-123-BB",
                20,
                FIXED_NOW,
                FIXED_NOW.plusSeconds(1200)));
    when(parkingSessionPersistencePort.save(any(VfpParkingSession.class)))
        .thenThrow(new DataIntegrityViolationException("uq_vfp_parking_user_daily"));

    assertThatThrownBy(() -> service.activateSession(USER_ID, "AA-123-BB"))
        .isInstanceOf(ParkingSessionAlreadyUsedException.class);
  }

  @Test
  @DisplayName(
      "Should use Europe/Paris calendar date and exact 20-minute duration across DST transition")
  void shouldBeDstProofAcrossAutumnSwitchNight() {
    Instant dstFallbackStart = Instant.parse("2026-10-25T00:50:00Z"); // 02:50 CEST -> 02:10 CET
    ParkingSessionService dstService =
        new ParkingSessionService(
            userVfpPersistencePort,
            userVehiclesPort,
            parkingGatewayPort,
            parkingSessionPersistencePort,
            transitTicketPersistencePort,
            Clock.fixed(dstFallbackStart, ZoneOffset.UTC));

    LocalDate parisDate = LocalDate.of(2026, 10, 25);
    UserVfpStatus activeVfp =
        new UserVfpStatus(
            USER_ID, true, dstFallbackStart.minusSeconds(3600), null, dstFallbackStart);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, parisDate)).thenReturn(false);
    when(userVehiclesPort.getUserLicensePlates(USER_ID)).thenReturn(List.of("AA-123-BB"));
    when(parkingGatewayPort.requestParkingTicket(USER_ID, "AA-123-BB", 20))
        .thenReturn(
            new ParkingGatewayTicket(
                "CONFIRMED", "PARK-DST-001", "AA-123-BB", 20, dstFallbackStart, null));
    when(parkingSessionPersistencePort.save(any(VfpParkingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VfpParkingSession result = dstService.activateSession(USER_ID, "AA-123-BB");

    assertThat(result.getSessionDate()).isEqualTo(parisDate);
    assertThat(result.getExpiresAt()).isEqualTo(Instant.parse("2026-10-25T01:10:00Z"));
  }

  @Test
  @DisplayName("Should return eligible=true when active VFP user has no session today")
  void shouldReturnEligibleWhenVfpHasNoSessionToday() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.findLatestByUserAndDate(USER_ID, FIXED_DATE))
        .thenReturn(Optional.empty());

    VfpDailyEligibility eligibility = service.getDailyEligibility(USER_ID);

    assertThat(eligibility.isVfp()).isTrue();
    assertThat(eligibility.eligible()).isTrue();
    assertThat(eligibility.parkingUsedToday()).isFalse();
    assertThat(eligibility.consumedBenefitType()).isNull();
    assertThat(eligibility.activeParkingSession()).isNull();
  }

  @Test
  @DisplayName("Should return eligible=false and active session when session is still running")
  void shouldReturnIneligibleWithActiveSessionWhenRunning() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    VfpParkingSession activeSession =
        VfpParkingSession.confirmed(
            USER_ID,
            "AA-123-BB",
            "PARK-ACTIVE-01",
            FIXED_NOW.minusSeconds(300),
            FIXED_NOW.plusSeconds(900),
            FIXED_DATE);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.findLatestByUserAndDate(USER_ID, FIXED_DATE))
        .thenReturn(Optional.of(activeSession));

    VfpDailyEligibility eligibility = service.getDailyEligibility(USER_ID);

    assertThat(eligibility.isVfp()).isTrue();
    assertThat(eligibility.eligible()).isFalse();
    assertThat(eligibility.parkingUsedToday()).isTrue();
    assertThat(eligibility.consumedBenefitType()).isEqualTo("PARKING");
    assertThat(eligibility.activeParkingSession()).isNotNull();
    assertThat(eligibility.activeParkingSession().getTicketReference()).isEqualTo("PARK-ACTIVE-01");
  }

  @Test
  @DisplayName(
      "Should return eligible=false and null activeParkingSession when today session expired")
  void shouldReturnIneligibleWithoutActiveSessionWhenExpired() {
    UserVfpStatus activeVfp = new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW);
    VfpParkingSession expiredSession =
        VfpParkingSession.confirmed(
            USER_ID,
            "AA-123-BB",
            "PARK-OLD-01",
            FIXED_NOW.minusSeconds(2400),
            FIXED_NOW.minusSeconds(1200),
            FIXED_DATE);
    when(userVfpPersistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(activeVfp));
    when(parkingSessionPersistencePort.findLatestByUserAndDate(USER_ID, FIXED_DATE))
        .thenReturn(Optional.of(expiredSession));

    VfpDailyEligibility eligibility = service.getDailyEligibility(USER_ID);

    assertThat(eligibility.eligible()).isFalse();
    assertThat(eligibility.parkingUsedToday()).isTrue();
    assertThat(eligibility.activeParkingSession()).isNull();
  }
}
