package fr.miage.collectivit.loyaltyservice.bdd.transit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import fr.miage.collectivit.loyaltyservice.application.service.TransitTicketService;
import fr.miage.collectivit.loyaltyservice.bdd.common.AbstractScenario;
import fr.miage.collectivit.loyaltyservice.bdd.common.ScenarioContext;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.SpringDataVfpTransitTicketRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserVfpStatusRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataVfpParkingSessionRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.ParkingGatewayHttpAdapter;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.TransitGatewayHttpAdapter;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.UsersServiceHttpAdapter;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

@RequiredArgsConstructor
public class TransitTicketSteps extends AbstractScenario {

  private static final String TRANSIT_ENDPOINT = "/v1/loyalty/vfp/transit-tickets";

  private final WebApplicationContext webApplicationContext;
  private final ScenarioContext scenarioContext;
  private final UsersServiceHttpAdapter usersServiceHttpAdapter;
  private final ParkingGatewayHttpAdapter parkingGatewayHttpAdapter;
  private final TransitGatewayHttpAdapter transitGatewayHttpAdapter;
  private final SpringDataUserVfpStatusRepository userVfpStatusRepository;
  private final SpringDataVfpParkingSessionRepository parkingSessionRepository;
  private final SpringDataVfpTransitTicketRepository transitTicketRepository;

  private MockMvc mockMvc;
  private MockRestServiceServer mockTransitServer;

  @Before("@transit")
  public void setUpScenario() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    transitTicketRepository.deleteAll();
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

    RestClient.Builder transitBuilder = RestClient.builder().baseUrl(MOCK_BASE_URL);
    mockTransitServer = MockRestServiceServer.bindTo(transitBuilder).build();
    transitGatewayHttpAdapter.setRestClient(transitBuilder.build());
  }

  @And("the external transit gateway confirms ticket {string} with code {string}")
  public void andExternalTransitGatewayConfirmsTicket(String ticketReference, String ticketCode) {
    String responseJson =
        """
        {
          "status": "CONFIRMED",
          "ticketReference": "%s",
          "ticketCode": "%s",
          "networkName": "Réseau Urbain Collectiv'IT",
          "durationMinutes": 60,
          "startedAt": "2026-11-24T10:00:00Z",
          "expiresAt": "2026-11-24T11:00:00Z"
        }
        """
            .formatted(ticketReference, ticketCode);

    mockTransitServer
        .expect(ExpectedCount.once(), requestTo(MOCK_BASE_URL + "/api/v1/transit/tickets"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
  }

  @When("the user requests a free transit ticket")
  public void whenUserRequestsFreeTransitTicket() throws Exception {
    MockHttpServletRequestBuilder requestBuilder =
        post(TRANSIT_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);

    if (scenarioContext.getJwtToken() != null) {
      requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + scenarioContext.getJwtToken());
    }

    scenarioContext.setLastResult(mockMvc.perform(requestBuilder));
  }

  @Then("a confirmed transit ticket is persisted in database for today")
  public void thenTransitTicketIsPersistedForToday() {
    LocalDate today = LocalDate.now(TransitTicketService.BUSINESS_ZONE);
    String userId = scenarioContext.getUserId();
    assertThat(transitTicketRepository.existsByUserIdAndTicketDate(userId, today)).isTrue();
  }
}
