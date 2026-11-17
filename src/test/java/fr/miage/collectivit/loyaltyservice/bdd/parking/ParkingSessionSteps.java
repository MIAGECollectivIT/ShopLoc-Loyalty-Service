package fr.miage.collectivit.loyaltyservice.bdd.parking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import fr.miage.collectivit.loyaltyservice.application.service.ParkingSessionService;
import fr.miage.collectivit.loyaltyservice.bdd.common.AbstractScenario;
import fr.miage.collectivit.loyaltyservice.bdd.common.ScenarioContext;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpStatusJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpParkingSessionJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserVfpStatusRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataVfpParkingSessionRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.ParkingGatewayHttpAdapter;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.UsersServiceHttpAdapter;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

@RequiredArgsConstructor
public class ParkingSessionSteps extends AbstractScenario {

  private final WebApplicationContext webApplicationContext;
  private final ScenarioContext scenarioContext;
  private final UsersServiceHttpAdapter usersServiceHttpAdapter;
  private final ParkingGatewayHttpAdapter parkingGatewayHttpAdapter;
  private final SpringDataUserVfpStatusRepository userVfpStatusRepository;
  private final SpringDataVfpParkingSessionRepository parkingSessionRepository;

  private MockMvc mockMvc;

  @Before("@parking")
  public void setUpScenario() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    parkingSessionRepository.deleteAll();
    userVfpStatusRepository.deleteAll();

    RestClient.Builder usersBuilder = RestClient.builder().baseUrl(MOCK_BASE_URL);
    MockRestServiceServer mockUsersServer = MockRestServiceServer.bindTo(usersBuilder).build();
    usersServiceHttpAdapter.setRestClient(usersBuilder.build());
    scenarioContext.setMockUsersServer(mockUsersServer);

    RestClient.Builder parkingBuilder = RestClient.builder().baseUrl(MOCK_BASE_URL);
    MockRestServiceServer mockParkingServer = MockRestServiceServer.bindTo(parkingBuilder).build();
    parkingGatewayHttpAdapter.setRestClient(parkingBuilder.build());
    scenarioContext.setMockParkingServer(mockParkingServer);
  }

  @And("the user has an active VFP status")
  public void andUserHasActiveVfpStatus() {
    Instant now = Instant.now();
    userVfpStatusRepository.save(
        UserVfpStatusJpaEntity.builder()
            .userId(scenarioContext.getUserId())
            .isVfp(true)
            .grantedAt(now.minusSeconds(3600))
            .lastEvaluatedAt(now)
            .build());
  }

  @And("the user does not have an active VFP status")
  public void andUserDoesNotHaveActiveVfpStatus() {
    Instant now = Instant.now();
    userVfpStatusRepository.save(
        UserVfpStatusJpaEntity.builder()
            .userId(scenarioContext.getUserId())
            .isVfp(false)
            .lastEvaluatedAt(now)
            .build());
  }

  @And("Users Service returns registered vehicle {string} for the user")
  public void andUsersServiceReturnsRegisteredVehicle(String licensePlate) {
    String vehiclesUrl = MOCK_BASE_URL + "/v1/users/" + scenarioContext.getUserId() + "/vehicles";
    String responseJson =
        """
        [
          {
            "vehicleId": "%s",
            "licensePlate": "%s"
          }
        ]
        """
            .formatted(licensePlate, licensePlate);

    scenarioContext
        .getMockUsersServer()
        .expect(ExpectedCount.manyTimes(), requestTo(vehiclesUrl))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
  }

  @And("Users Service returns no registered vehicles for the user")
  public void andUsersServiceReturnsNoVehicles() {
    String vehiclesUrl = MOCK_BASE_URL + "/v1/users/" + scenarioContext.getUserId() + "/vehicles";
    scenarioContext
        .getMockUsersServer()
        .expect(ExpectedCount.manyTimes(), requestTo(vehiclesUrl))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
  }

  @And("the external parking gateway confirms ticket {string} for plate {string}")
  public void andExternalParkingGatewayConfirmsTicket(String ticketRef, String licensePlate) {
    scenarioContext
        .getMockParkingServer()
        .expect(ExpectedCount.manyTimes(), requestTo(PARKING_GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                buildParkingTicketConfirmedJson(licensePlate, ticketRef),
                MediaType.APPLICATION_JSON));
  }

  @And(
      "the external parking gateway fails once with 503 before confirming ticket {string} for plate {string}")
  public void andExternalParkingGatewayFailsOnceBeforeConfirming(
      String ticketRef, String licensePlate) {
    scenarioContext
        .getMockParkingServer()
        .expect(requestTo(PARKING_GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    scenarioContext
        .getMockParkingServer()
        .expect(requestTo(PARKING_GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                buildParkingTicketConfirmedJson(licensePlate, ticketRef),
                MediaType.APPLICATION_JSON));
  }

  @And("the external parking gateway is completely unavailable with 503")
  public void andExternalParkingGatewayIsUnavailable() {
    scenarioContext
        .getMockParkingServer()
        .expect(ExpectedCount.manyTimes(), requestTo(PARKING_GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
  }

  @When("the user requests a free parking session for plate {string}")
  public void whenUserRequestsFreeParkingSession(String licensePlate) throws Exception {
    String payload =
        """
        {
          "licensePlate": "%s"
        }
        """
            .formatted(licensePlate);

    MockHttpServletRequestBuilder requestBuilder =
        post(PARKING_SESSIONS_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(payload);

    if (scenarioContext.getJwtToken() != null) {
      requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + scenarioContext.getJwtToken());
    }

    scenarioContext.setLastResult(mockMvc.perform(requestBuilder));
  }

  @Then("a confirmed parking session for plate {string} is persisted in database for today")
  public void thenParkingSessionIsPersistedForToday(String expectedPlate) {
    LocalDate today = LocalDate.now(ParkingSessionService.BUSINESS_ZONE);
    Optional<VfpParkingSessionJpaEntity> saved =
        parkingSessionRepository.findFirstByUserIdAndSessionDateOrderByCreatedAtDesc(
            scenarioContext.getUserId(), today);
    assertThat(saved).isPresent();
    assertThat(saved.get().getLicensePlate()).isEqualTo(expectedPlate);
    assertThat(saved.get().getStatus()).isEqualTo("CONFIRMED");
    assertThat(saved.get().getTicketReference()).isNotBlank();
    assertThat(saved.get().getExpiresAt()).isEqualTo(saved.get().getStartedAt().plusSeconds(1200));
  }
}
