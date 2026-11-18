package fr.miage.collectivit.loyaltyservice.bdd.common;

import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.ResultActions;

@Getter
@Setter
@Component
@ScenarioScope
public class ScenarioContext {

  private String userId;
  private String jwtToken;
  private ResultActions lastResult;
  private MockRestServiceServer mockUsersServer;
  private MockRestServiceServer mockParkingServer;
}
