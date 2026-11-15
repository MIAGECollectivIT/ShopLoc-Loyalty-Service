package fr.miage.collectivit.loyaltyservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Domain record representing an approved payment transaction retrieved from the Sales Service. */
public record UserTransaction(
    String transactionId,
    String reference,
    BigDecimal amount,
    String currency,
    String status,
    String authorizationCode,
    Instant createdAt) {}
