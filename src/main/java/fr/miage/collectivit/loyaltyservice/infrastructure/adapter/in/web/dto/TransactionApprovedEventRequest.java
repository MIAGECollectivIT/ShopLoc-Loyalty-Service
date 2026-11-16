package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Inbound payload representing an approved transaction event notification (SLE-45 / SLE-201). */
@Schema(description = "Payload pour la notification d'une transaction approuvée")
public record TransactionApprovedEventRequest(
    @Schema(
            description = "Identifiant de l'usager",
            example = "USER-12345",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "userId must not be blank")
        String userId,
    @Schema(description = "Identifiant technique de transaction", example = "TXN-7890")
        String transactionId,
    @Schema(description = "Montant de la transaction", example = "25.50") Double amount,
    @Schema(description = "Statut de la transaction", example = "TRANSACTION_APPROVED")
        String status) {}
