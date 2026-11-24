package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.TransitTicketAlreadyUsedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.model.TransitGatewayTicket;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingSessionPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitGatewayPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitTicketPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransitTicketService Unit Tests")
class TransitTicketServiceTest {

  private static final String USER_ID = "user-vfp-001";
  private static final Instant FIXED_NOW = Instant.parse("2026-11-24T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneId.of("Europe/Paris"));

  @Mock private UserVfpPersistencePort userVfpPersistencePort;
  @Mock private TransitGatewayPort transitGatewayPort;
  @Mock private TransitTicketPersistencePort transitTicketPersistencePort;
  @Mock private ParkingSessionPersistencePort parkingSessionPersistencePort;

  private TransitTicketService service;

  @BeforeEach
  void setUp() {
    service =
        new TransitTicketService(
            userVfpPersistencePort,
            transitGatewayPort,
            transitTicketPersistencePort,
            parkingSessionPersistencePort,
            FIXED_CLOCK);
  }

  @Test
  @DisplayName("Should activate and persist public transit ticket for eligible active VFP user")
  void shouldActivateTransitTicketSuccessfully() {
    LocalDate today = LocalDate.ofInstant(FIXED_NOW, TransitTicketService.BUSINESS_ZONE);
    when(userVfpPersistencePort.findByUserId(USER_ID))
        .thenReturn(Optional.of(new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW)));
    when(transitTicketPersistencePort.existsForUserOnDate(USER_ID, today)).thenReturn(false);
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, today)).thenReturn(false);

    TransitGatewayTicket gatewayTicket =
        new TransitGatewayTicket(
            "TRN-12345",
            "BUS-888999",
            "Réseau Urbain Collectiv'IT",
            60,
            FIXED_NOW,
            FIXED_NOW.plusSeconds(3600),
            "CONFIRMED");
    when(transitGatewayPort.requestTransitTicket(USER_ID, 60)).thenReturn(gatewayTicket);
    when(transitTicketPersistencePort.save(any(VfpTransitTicket.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VfpTransitTicket result = service.activateTicket(USER_ID);

    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(USER_ID);
    assertThat(result.getTicketReference()).isEqualTo("TRN-12345");
    assertThat(result.getTicketCode()).isEqualTo("BUS-888999");
    assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    verify(transitTicketPersistencePort).save(any(VfpTransitTicket.class));
  }

  @Test
  @DisplayName("Should reject transit ticket request when user is not VFP")
  void shouldRejectWhenUserIsNotVfp() {
    when(userVfpPersistencePort.findByUserId(USER_ID))
        .thenReturn(Optional.of(new UserVfpStatus(USER_ID, false, null, null, FIXED_NOW)));

    assertThatThrownBy(() -> service.activateTicket(USER_ID))
        .isInstanceOf(UserNotVfpException.class);
  }

  @Test
  @DisplayName(
      "Should reject transit ticket request when transit ticket was already claimed today (SLE-48)")
  void shouldRejectWhenTransitAlreadyUsedToday() {
    LocalDate today = LocalDate.ofInstant(FIXED_NOW, TransitTicketService.BUSINESS_ZONE);
    when(userVfpPersistencePort.findByUserId(USER_ID))
        .thenReturn(Optional.of(new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW)));
    when(transitTicketPersistencePort.existsForUserOnDate(USER_ID, today)).thenReturn(true);

    assertThatThrownBy(() -> service.activateTicket(USER_ID))
        .isInstanceOf(TransitTicketAlreadyUsedException.class)
        .hasMessageContaining("transit ticket has already been activated today");
  }

  @Test
  @DisplayName(
      "Should reject transit ticket request when parking benefit was already claimed today (SLE-48)")
  void shouldRejectWhenParkingAlreadyUsedToday() {
    LocalDate today = LocalDate.ofInstant(FIXED_NOW, TransitTicketService.BUSINESS_ZONE);
    when(userVfpPersistencePort.findByUserId(USER_ID))
        .thenReturn(Optional.of(new UserVfpStatus(USER_ID, true, FIXED_NOW, null, FIXED_NOW)));
    when(transitTicketPersistencePort.existsForUserOnDate(USER_ID, today)).thenReturn(false);
    when(parkingSessionPersistencePort.existsForUserOnDate(USER_ID, today)).thenReturn(true);

    assertThatThrownBy(() -> service.activateTicket(USER_ID))
        .isInstanceOf(TransitTicketAlreadyUsedException.class)
        .hasMessageContaining("Daily institutional benefit already consumed today");
  }
}
