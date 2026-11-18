package fr.miage.collectivit.loyaltyservice.bdd.common;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.ParkingGatewayHttpAdapter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public abstract class AbstractScenario {

  protected static final String PARKING_SESSIONS_ENDPOINT = "/v1/loyalty/vfp/parking-sessions";
  protected static final String MOCK_BASE_URL = "http://localhost:8089";
  protected static final String PARKING_GATEWAY_URL =
      MOCK_BASE_URL + ParkingGatewayHttpAdapter.PARKING_SESSIONS_PATH;

  protected static final String HEADER_AUTHORIZATION = "Authorization";
  protected static final String BEARER_PREFIX = "Bearer ";

  protected static final String TEST_JWT_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";
  protected static final String TEST_JWT_ISSUER = "shoploc-auth-service";
  protected static final String DEFAULT_CLIENT_EMAIL = "vfp.client@shoploc.fr";
  protected static final long TOKEN_VALIDITY_MS = 3_600_000L;

  protected String generateJwtToken(String userId, String role) {
    byte[] keyBytes = Base64.getDecoder().decode(TEST_JWT_SECRET);
    SecretKey signingKey = Keys.hmacShaKeyFor(keyBytes);
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(userId)
        .issuer(TEST_JWT_ISSUER)
        .issuedAt(new Date(now))
        .expiration(new Date(now + TOKEN_VALIDITY_MS))
        .claim("userId", userId)
        .claim("email", DEFAULT_CLIENT_EMAIL)
        .claim("role", role)
        .claim("clientCode", "CL10001")
        .signWith(signingKey)
        .compact();
  }

  protected String buildParkingTicketConfirmedJson(String licensePlate, String ticketRef) {
    return """
        {
          "status": "CONFIRMED",
          "ticketReference": "%s",
          "licensePlate": "%s",
          "durationMinutes": 20,
          "startedAt": "2026-11-17T14:00:00Z",
          "expiresAt": "2026-11-17T14:20:00Z"
        }
        """
        .formatted(ticketRef, licensePlate);
  }
}
